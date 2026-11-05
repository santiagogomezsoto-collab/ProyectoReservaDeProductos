package reservas.data;

import reservas.logic.CategoriaRecurso;
import reservas.logic.Recurso;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla recurso. Cada recurso pertenece a una categoria
 * (recurso.categoria_id es llave foranea a categoria_recurso.id).
 *
 * Aqui aparece el primer metodo traductor que arma un objeto compuesto: la
 * fila trae a la vez los datos del recurso y los de su categoria, y from()
 * devuelve un Recurso con su CategoriaRecurso ya construida adentro.
 *
 * Por eso la consulta renombra las columnas con alias: recurso y
 * categoria_recurso tienen las dos una columna id y una descripcion, y sin
 * alias el ResultSet no podria distinguirlas.
 */
public class RecursoDao {
    Database db;

    public RecursoDao() {
        db = Database.instance();
    }

    static final String SELECT_BASE =
            "select r.id as recurso_id, r.descripcion as recurso_descripcion,"
            + " c.id as categoria_id, c.descripcion as categoria_descripcion"
            + " from recurso r inner join categoria_recurso c on c.id = r.categoria_id";

    public void create(Recurso e) throws Exception {
        String sql = "insert into recurso (id, categoria_id, descripcion) values(?,?,?)";
        try {
            PreparedStatement stm = db.prepareStatement(sql);
            stm.setString(1, e.getId());
            stm.setString(2, e.getCategoria() == null ? null : e.getCategoria().getId());
            stm.setString(3, e.getDescripcion());
            db.executeUpdate(stm);
        } catch (SQLIntegrityConstraintViolationException ex) {
            // Puede ser id repetido o una categoria que no existe (error 1452).
            if (existe(e.getId())) {
                throw new Exception("Recurso ya existe");
            }
            throw new Exception("La categoria del recurso no existe");
        }
    }

    public boolean existe(String id) throws SQLException {
        PreparedStatement stm = db.prepareStatement("select id from recurso where id=?");
        stm.setString(1, id);
        return db.executeQuery(stm).next();
    }

    public Recurso read(Recurso e) throws Exception {
        PreparedStatement stm = db.prepareStatement(SELECT_BASE + " where r.id=?");
        stm.setString(1, e.getId());
        ResultSet rs = db.executeQuery(stm);

        if (rs.next()) {
            return from(rs);
        } else {
            throw new Exception("Recurso no existe");
        }
    }

    public void update(Recurso e) throws Exception {
        String sql = "update recurso set categoria_id=?, descripcion=? where id=?";
        try {
            PreparedStatement stm = db.prepareStatement(sql);
            stm.setString(1, e.getCategoria() == null ? null : e.getCategoria().getId());
            stm.setString(2, e.getDescripcion());
            stm.setString(3, e.getId());

            if (db.executeUpdate(stm) == 0) {
                throw new Exception("Recurso no existe");
            }
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new Exception("La categoria del recurso no existe");
        }
    }

    public void delete(Recurso e) throws Exception {
        try {
            PreparedStatement stm = db.prepareStatement("delete from recurso where id=?");
            stm.setString(1, e.getId());

            if (db.executeUpdate(stm) == 0) {
                throw new Exception("Recurso no existe");
            }
        } catch (SQLIntegrityConstraintViolationException ex) {
            // fk_reserva_recurso_recurso es ON DELETE RESTRICT.
            throw new Exception("No se puede eliminar el recurso porque tiene reservas asociadas");
        }
    }

    /**
     * Busqueda por categoria y/o descripcion. Un filtro vacio o nulo no filtra,
     * igual que en la version anterior.
     */
    public List<Recurso> search(CategoriaRecurso categoria, String descripcion) throws Exception {
        boolean filtraCategoria = categoria != null
                && categoria.getId() != null
                && !categoria.getId().isEmpty();

        String sql = SELECT_BASE
                + " where lower(r.descripcion) like ?"
                + (filtraCategoria ? " and r.categoria_id = ?" : "")
                + " order by r.descripcion";

        PreparedStatement stm = db.prepareStatement(sql);
        stm.setString(1, "%" + (descripcion == null ? "" : descripcion.toLowerCase()) + "%");
        if (filtraCategoria) {
            stm.setString(2, categoria.getId());
        }
        ResultSet rs = db.executeQuery(stm);

        List<Recurso> resultado = new ArrayList<>();
        while (rs.next()) {
            resultado.add(from(rs));
        }
        return resultado;
    }

    /**
     * Metodo traductor. Lee los alias de SELECT_BASE, no los nombres reales de
     * las columnas. Construye tambien la CategoriaRecurso de la misma fila.
     */
    static Recurso from(ResultSet rs) throws SQLException {
        CategoriaRecurso c = new CategoriaRecurso();
        c.setId(rs.getString("categoria_id"));
        c.setDescripcion(rs.getString("categoria_descripcion"));

        Recurso r = new Recurso();
        r.setId(rs.getString("recurso_id"));
        r.setDescripcion(rs.getString("recurso_descripcion"));
        r.setCategoria(c);
        return r;
    }
}
