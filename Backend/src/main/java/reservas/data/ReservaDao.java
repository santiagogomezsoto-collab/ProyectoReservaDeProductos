package reservas.data;

import reservas.logic.CategoriaRecurso;
import reservas.logic.Funcionario;
import reservas.logic.Recurso;
import reservas.logic.Reserva;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a las tablas reserva y reserva_recurso.
 *
 * Es el DAO mas complejo del proyecto por dos razones:
 *  1. Una reserva no vive en una sola tabla. Guardarla significa escribir una
 *     fila en reserva y una por cada recurso en reserva_recurso, que es la
 *     tabla intermedia de la relacion N:M entre reserva y recurso. Eso tiene
 *     que ocurrir dentro de una transaccion: o quedan todas las filas o
 *     ninguna, porque una reserva sin recursos no tiene sentido.
 *  2. La disponibilidad se decide comparando con las reservas que ya existen,
 *     asi que comprobar y escribir no pueden separarse.
 */
public class ReservaDao {
    Database db;

    public ReservaDao() {
        db = Database.instance();
    }

    private static final String SELECT_BASE =
            "select res.id as reserva_id, res.actividad, res.fecha,"
            + " res.hora_inicio, res.hora_fin, res.estado,"
            + " u.id as usuario_id, u.clave, u.rol, f.nombre, f.telefono"
            + " from reserva res"
            + " inner join funcionario f on f.id = res.funcionario_id"
            + " inner join usuario u on u.id = f.id";

    /**
     * Objeto de bloqueo de este DAO. Quien tenga que comprobar disponibilidad
     * y guardar como una sola operacion indivisible debe sincronizar sobre el:
     *
     *     synchronized (reservaDao.lock()) { ...comprobar...; ...crear... }
     *
     * Sin esto, dos Frontend podrian recibir el mismo recurso libre: los dos
     * consultan, los dos lo ven disponible, y los dos lo reservan.
     * El bloqueo cubre a los hilos de ESTE Backend, que es el unico proceso
     * que escribe en la base. Si algun dia hubiera dos Backend, haria falta
     * ademas un bloqueo en SQL (select ... for update).
     */
    public Object lock() {
        return db;
    }

    /** Guarda la reserva y sus recursos. Todo o nada. */
    public void create(Reserva e) throws Exception {
        synchronized (db) {
            try {
                db.beginTransaction();

                String sql = "insert into reserva"
                        + " (id, funcionario_id, actividad, fecha, hora_inicio, hora_fin, estado)"
                        + " values(?,?,?,?,?,?,?)";
                PreparedStatement stm = db.prepareStatement(sql);
                stm.setString(1, e.getId());
                stm.setString(2, e.getFuncionario().getId());
                stm.setString(3, e.getActividad());
                stm.setDate(4, java.sql.Date.valueOf(e.getFecha()));
                stm.setTime(5, java.sql.Time.valueOf(e.getHoraInicio()));
                stm.setTime(6, java.sql.Time.valueOf(e.getHoraFin()));
                stm.setString(7, e.getEstado());
                db.executeUpdate(stm);

                String sqlRecurso = "insert into reserva_recurso (reserva_id, recurso_id) values(?,?)";
                for (Recurso r : e.getRecursos()) {
                    PreparedStatement stmRecurso = db.prepareStatement(sqlRecurso);
                    stmRecurso.setString(1, e.getId());
                    stmRecurso.setString(2, r.getId());
                    db.executeUpdate(stmRecurso);
                }

                db.commit();
            } catch (Exception ex) {
                db.rollback();
                throw ex;
            }
        }
    }

    public Reserva read(String id) throws Exception {
        PreparedStatement stm = db.prepareStatement(SELECT_BASE + " where res.id=?");
        stm.setString(1, id);
        ResultSet rs = db.executeQuery(stm);

        if (rs.next()) {
            Reserva reserva = from(rs);
            cargarRecursos(reserva);
            return reserva;
        } else {
            throw new Exception("Reserva no existe");
        }
    }

    /** Cambia el estado a CANCELADA. Las asignaciones de recursos se conservan. */
    public void cancelar(Reserva e) throws Exception {
        PreparedStatement stm = db.prepareStatement("update reserva set estado=? where id=?");
        stm.setString(1, Reserva.CANCELADA);
        stm.setString(2, e.getId());

        if (db.executeUpdate(stm) == 0) {
            throw new Exception("Reserva no existe");
        }
    }

    public List<Reserva> findByFuncionario(Funcionario funcionario) throws Exception {
        PreparedStatement stm = db.prepareStatement(SELECT_BASE
                + " where res.funcionario_id=? order by res.fecha, res.hora_inicio");
        stm.setString(1, funcionario.getId());
        return listar(stm);
    }

    public List<Reserva> findActivasPorFecha(LocalDate fecha) throws Exception {
        PreparedStatement stm = db.prepareStatement(SELECT_BASE
                + " where res.estado=? and res.fecha=? order by res.hora_inicio");
        stm.setString(1, Reserva.ACTIVA);
        stm.setDate(2, java.sql.Date.valueOf(fecha));
        return listar(stm);
    }

    public List<Reserva> findActivasEntre(LocalDate desde, LocalDate hasta) throws Exception {
        PreparedStatement stm = db.prepareStatement(SELECT_BASE
                + " where res.estado=? and res.fecha between ? and ?"
                + " order by res.fecha, res.hora_inicio");
        stm.setString(1, Reserva.ACTIVA);
        stm.setDate(2, java.sql.Date.valueOf(desde));
        stm.setDate(3, java.sql.Date.valueOf(hasta));
        return listar(stm);
    }

    /**
     * Devuelve el primer recurso de la categoria que no este ocupado en ese
     * horario, o null si no hay ninguno.
     *
     * La condicion de solape es  inicio menor que hora_fin  Y  fin mayor que
     * hora_inicio: dos intervalos se cruzan si cada uno empieza antes de que
     * el otro termine. Intervalos contiguos (uno termina justo cuando el otro
     * empieza) NO se consideran ocupados, igual que en la version anterior.
     *
     * Solo las reservas ACTIVA ocupan recursos; una CANCELADA los libera.
     */
    public Recurso buscarRecursoLibre(CategoriaRecurso categoria, LocalDate fecha,
                                      LocalTime horaInicio, LocalTime horaFin,
                                      String reservaIdExcluida) throws Exception {
        boolean excluye = reservaIdExcluida != null;

        String sql = RecursoDao.SELECT_BASE
                + " where r.categoria_id = ?"
                + " and not exists ("
                + "   select 1 from reserva_recurso rr"
                + "   inner join reserva res on res.id = rr.reserva_id"
                + "   where rr.recurso_id = r.id"
                + "     and res.estado = ?"
                + "     and res.fecha = ?"
                + "     and ? < res.hora_fin and ? > res.hora_inicio"
                + (excluye ? "     and res.id <> ?" : "")
                + " )"
                + " order by r.descripcion limit 1";

        PreparedStatement stm = db.prepareStatement(sql);
        stm.setString(1, categoria.getId());
        stm.setString(2, Reserva.ACTIVA);
        stm.setDate(3, java.sql.Date.valueOf(fecha));
        stm.setTime(4, java.sql.Time.valueOf(horaInicio));
        stm.setTime(5, java.sql.Time.valueOf(horaFin));
        if (excluye) {
            stm.setString(6, reservaIdExcluida);
        }

        ResultSet rs = db.executeQuery(stm);
        return rs.next() ? RecursoDao.from(rs) : null;
    }

    /** Siguiente identificador con el formato RES-000001. */
    public String nextId() throws SQLException {
        String sql = "select max(cast(substring(id, 5) as unsigned)) from reserva";
        PreparedStatement stm = db.prepareStatement(sql);
        ResultSet rs = db.executeQuery(stm);
        int max = rs.next() ? rs.getInt(1) : 0;
        return String.format("RES-%06d", max + 1);
    }

    private List<Reserva> listar(PreparedStatement stm) throws Exception {
        ResultSet rs = db.executeQuery(stm);

        List<Reserva> resultado = new ArrayList<>();
        while (rs.next()) {
            resultado.add(from(rs));
        }
        // Los recursos se cargan despues de terminar el recorrido anterior: la
        // conexion es una sola y no conviene tener dos ResultSet abiertos a la
        // vez sobre ella.
        for (Reserva r : resultado) {
            cargarRecursos(r);
        }
        return resultado;
    }

    /** Llena la lista de recursos de una reserva desde la tabla intermedia. */
    private void cargarRecursos(Reserva reserva) throws Exception {
        String sql = RecursoDao.SELECT_BASE
                + " inner join reserva_recurso rr on rr.recurso_id = r.id"
                + " where rr.reserva_id = ? order by r.descripcion";
        PreparedStatement stm = db.prepareStatement(sql);
        stm.setString(1, reserva.getId());
        ResultSet rs = db.executeQuery(stm);

        List<Recurso> recursos = new ArrayList<>();
        while (rs.next()) {
            recursos.add(RecursoDao.from(rs));
        }
        reserva.setRecursos(recursos);
    }

    /** Metodo traductor. Arma la reserva con su Funcionario, sin los recursos. */
    Reserva from(ResultSet rs) throws SQLException {
        Funcionario f = new Funcionario();
        f.setId(rs.getString("usuario_id"));
        f.setClave(rs.getString("clave"));
        f.setRol(rs.getString("rol"));
        f.setNombre(rs.getString("nombre"));
        f.setTelefono(rs.getString("telefono"));

        Reserva r = new Reserva();
        r.setId(rs.getString("reserva_id"));
        r.setFuncionario(f);
        r.setActividad(rs.getString("actividad"));
        r.setFecha(rs.getDate("fecha").toLocalDate());
        r.setHoraInicio(rs.getTime("hora_inicio").toLocalTime());
        r.setHoraFin(rs.getTime("hora_fin").toLocalTime());
        r.setEstado(rs.getString("estado"));
        return r;
    }
}
