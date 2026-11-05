package reservas.data;

import reservas.logic.CategoriaRecurso;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla categoria_recurso. Es el DAO mas simple del proyecto
 * (dos columnas, sin llaves foraneas) y sirve de molde para los demas.
 *
 * Responsabilidad: traducir entre filas de SQL y objetos CategoriaRecurso.
 * No contiene reglas de negocio; esas quedan en Service.
 */
public class CategoriaRecursoDao {
    Database db;

    public CategoriaRecursoDao() {
        db = Database.instance();
    }

    public void create(CategoriaRecurso e) throws Exception {
        // El id se calcula y se inserta sin que otro hilo pueda interponerse;
        // si no, dos Workers podrian generar el mismo CAT-000004.
        synchronized (db) {
            e.setId(nextId());
            String sql = "insert into categoria_recurso (id, descripcion) values(?,?)";
            try {
                PreparedStatement stm = db.prepareStatement(sql);
                stm.setString(1, e.getId());
                stm.setString(2, e.getDescripcion());
                db.executeUpdate(stm);
            } catch (SQLIntegrityConstraintViolationException ex) {
                throw new Exception("Categoria ya existe");
            }
        }
    }

    public CategoriaRecurso read(CategoriaRecurso e) throws Exception {
        String sql = "select id, descripcion from categoria_recurso where id=?";
        PreparedStatement stm = db.prepareStatement(sql);
        stm.setString(1, e.getId());
        ResultSet rs = db.executeQuery(stm);

        if (rs.next()) {
            return from(rs);
        } else {
            throw new Exception("Categoria no existe");
        }
    }

    public void update(CategoriaRecurso e) throws Exception {
        String sql = "update categoria_recurso set descripcion=? where id=?";
        PreparedStatement stm = db.prepareStatement(sql);
        stm.setString(1, e.getDescripcion());
        stm.setString(2, e.getId());

        if (db.executeUpdate(stm) == 0) {
            throw new Exception("Categoria no existe");
        }
    }

    public void delete(CategoriaRecurso e) throws Exception {
        String sql = "delete from categoria_recurso where id=?";
        try {
            PreparedStatement stm = db.prepareStatement(sql);
            stm.setString(1, e.getId());
            if (db.executeUpdate(stm) == 0) {
                throw new Exception("Categoria no existe");
            }
        } catch (SQLIntegrityConstraintViolationException ex) {
            // La llave foranea fk_recurso_categoria es ON DELETE RESTRICT:
            // MySQL impide borrar una categoria que todavia tiene recursos.
            throw new Exception("No se puede eliminar la categoría porque tiene recursos asociados");
        }
    }

    public List<CategoriaRecurso> findAll() throws Exception {
        String sql = "select id, descripcion from categoria_recurso order by descripcion";
        PreparedStatement stm = db.prepareStatement(sql);
        ResultSet rs = db.executeQuery(stm);

        List<CategoriaRecurso> resultado = new ArrayList<>();
        while (rs.next()) {
            resultado.add(from(rs));
        }
        return resultado;
    }

    /** Busqueda parcial por descripcion, sin distinguir mayusculas. La columna
     *  usa collation binaria, asi que se comparan ambos lados en minuscula. */
    public List<CategoriaRecurso> findByDescripcion(CategoriaRecurso filtro) throws Exception {
        String sql = "select id, descripcion from categoria_recurso"
                + " where lower(descripcion) like ? order by descripcion";
        PreparedStatement stm = db.prepareStatement(sql);
        String texto = filtro.getDescripcion() == null ? "" : filtro.getDescripcion();
        stm.setString(1, "%" + texto.toLowerCase() + "%");
        ResultSet rs = db.executeQuery(stm);

        List<CategoriaRecurso> resultado = new ArrayList<>();
        while (rs.next()) {
            resultado.add(from(rs));
        }
        return resultado;
    }

    /** Siguiente identificador con el formato CAT-000001 que ya usa el sistema. */
    private String nextId() throws SQLException {
        String sql = "select max(cast(substring(id, 5) as unsigned)) from categoria_recurso";
        PreparedStatement stm = db.prepareStatement(sql);
        ResultSet rs = db.executeQuery(stm);
        int max = rs.next() ? rs.getInt(1) : 0;
        return String.format("CAT-%06d", max + 1);
    }

    /** Metodo traductor: convierte la fila actual del ResultSet en una entidad. */
    CategoriaRecurso from(ResultSet rs) throws SQLException {
        CategoriaRecurso c = new CategoriaRecurso();
        c.setId(rs.getString("id"));
        c.setDescripcion(rs.getString("descripcion"));
        return c;
    }
}
