package reservas.data;

import reservas.logic.Funcionario;
import reservas.logic.Usuario;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla funcionario. Un funcionario comparte su identificador con
 * su usuario (funcionario.id es llave primaria y llave foranea a usuario.id),
 * asi que crear o borrar uno toca DOS tablas y requiere una transaccion:
 * si falla la segunda instruccion, la primera tampoco debe quedar guardada.
 */
public class FuncionarioDao {
    Database db;
    UsuarioDao usuarioDao;

    public FuncionarioDao() {
        db = Database.instance();
        usuarioDao = new UsuarioDao();
    }

    private static final String SELECT_BASE =
            "select u.id, u.clave, u.rol, f.nombre, f.telefono"
            + " from funcionario f inner join usuario u on u.id = f.id";

    /**
     * Inserta la fila de usuario y la de funcionario como una sola unidad.
     * La clave inicial es el propio identificador, igual que antes.
     */
    public void create(Funcionario e) throws Exception {
        synchronized (db) {
            try {
                db.beginTransaction();

                // Si ya hay un usuario con ese identificador no se puede crear
                // el funcionario, porque comparten la llave primaria. Se
                // comprueba aqui para dar el mensaje que espera la interfaz:
                // UsuarioDao.create diria "Usuario ya existe", que no es el
                // termino que el usuario de la aplicacion reconoce.
                if (usuarioDao.existe(e.getId())) {
                    throw new Exception("Funcionario ya existe");
                }

                Usuario u = new Usuario(e.getId(), e.getId(), Usuario.FUNCIONARIO);
                usuarioDao.create(u);

                String sql = "insert into funcionario (id, nombre, telefono) values(?,?,?)";
                PreparedStatement stm = db.prepareStatement(sql);
                stm.setString(1, e.getId());
                stm.setString(2, e.getNombre());
                stm.setString(3, e.getTelefono());
                db.executeUpdate(stm);

                db.commit();
                e.setClave(e.getId());
            } catch (SQLIntegrityConstraintViolationException ex) {
                db.rollback();
                throw new Exception("Funcionario ya existe");
            } catch (Exception ex) {
                db.rollback();
                throw ex;
            }
        }
    }

    public Funcionario read(Funcionario e) throws Exception {
        PreparedStatement stm = db.prepareStatement(SELECT_BASE + " where f.id=?");
        stm.setString(1, e.getId());
        ResultSet rs = db.executeQuery(stm);

        if (rs.next()) {
            return from(rs);
        } else {
            throw new Exception("Funcionario no existe");
        }
    }

    public void update(Funcionario e) throws Exception {
        String sql = "update funcionario set nombre=?, telefono=? where id=?";
        PreparedStatement stm = db.prepareStatement(sql);
        stm.setString(1, e.getNombre());
        stm.setString(2, e.getTelefono());
        stm.setString(3, e.getId());

        if (db.executeUpdate(stm) == 0) {
            throw new Exception("Funcionario no existe");
        }
    }

    /** Borra la ficha y su usuario en una sola transaccion. */
    public void delete(Funcionario e) throws Exception {
        synchronized (db) {
            try {
                db.beginTransaction();

                PreparedStatement stm = db.prepareStatement("delete from funcionario where id=?");
                stm.setString(1, e.getId());
                if (db.executeUpdate(stm) == 0) {
                    throw new Exception("Funcionario no existe");
                }
                usuarioDao.delete(e.getId());

                db.commit();
            } catch (SQLIntegrityConstraintViolationException ex) {
                // fk_reserva_funcionario es ON DELETE RESTRICT.
                db.rollback();
                throw new Exception("No se puede eliminar el funcionario porque tiene reservas asociadas");
            } catch (Exception ex) {
                db.rollback();
                throw ex;
            }
        }
    }

    /** Busqueda parcial por id y por nombre, sin distinguir mayusculas. */
    public List<Funcionario> search(Funcionario filtro) throws Exception {
        String sql = SELECT_BASE
                + " where lower(f.id) like ? and lower(f.nombre) like ?"
                + " order by f.nombre";
        PreparedStatement stm = db.prepareStatement(sql);
        stm.setString(1, "%" + texto(filtro.getId()) + "%");
        stm.setString(2, "%" + texto(filtro.getNombre()) + "%");
        ResultSet rs = db.executeQuery(stm);

        List<Funcionario> resultado = new ArrayList<>();
        while (rs.next()) {
            resultado.add(from(rs));
        }
        return resultado;
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.toLowerCase();
    }

    /** Metodo traductor. La consulta siempre trae la ficha, asi que es Funcionario. */
    Funcionario from(ResultSet rs) throws SQLException {
        Funcionario f = new Funcionario();
        f.setId(rs.getString("id"));
        f.setClave(rs.getString("clave"));
        f.setRol(rs.getString("rol"));
        f.setNombre(rs.getString("nombre"));
        f.setTelefono(rs.getString("telefono"));
        return f;
    }
}
