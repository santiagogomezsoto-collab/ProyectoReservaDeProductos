package reservas.data;

import reservas.logic.Funcionario;
import reservas.logic.Usuario;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * Acceso a la tabla usuario (credencial y rol).
 *
 * Detalle importante: usuario y funcionario forman una herencia que comparte
 * el identificador. Al leer un usuario se consulta con LEFT JOIN, y si existe
 * la ficha de funcionario se devuelve un objeto Funcionario, no un Usuario.
 * El Frontend hace  (Funcionario) Sesion.getUsuario()  despues del login,
 * asi que devolver un Usuario plano provocaria ClassCastException.
 */
public class UsuarioDao {
    Database db;

    public UsuarioDao() {
        db = Database.instance();
    }

    /** Consulta con la ficha de funcionario si la tiene. */
    private static final String SELECT_BASE =
            "select u.id, u.clave, u.rol, f.nombre, f.telefono"
            + " from usuario u left join funcionario f on f.id = u.id";

    public Usuario read(String id) throws Exception {
        PreparedStatement stm = db.prepareStatement(SELECT_BASE + " where u.id=?");
        stm.setString(1, id);
        ResultSet rs = db.executeQuery(stm);

        if (rs.next()) {
            return from(rs);
        } else {
            throw new Exception("Usuario no existe");
        }
    }

    public boolean existe(String id) throws SQLException {
        PreparedStatement stm = db.prepareStatement("select id from usuario where id=?");
        stm.setString(1, id);
        return db.executeQuery(stm).next();
    }

    public void create(Usuario u) throws Exception {
        String sql = "insert into usuario (id, clave, rol) values(?,?,?)";
        try {
            PreparedStatement stm = db.prepareStatement(sql);
            stm.setString(1, u.getId());
            stm.setString(2, u.getClave());
            stm.setString(3, u.getRol());
            db.executeUpdate(stm);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new Exception("Usuario ya existe");
        }
    }

    public void updateClave(String id, String claveNueva) throws Exception {
        PreparedStatement stm = db.prepareStatement("update usuario set clave=? where id=?");
        stm.setString(1, claveNueva);
        stm.setString(2, id);

        if (db.executeUpdate(stm) == 0) {
            throw new Exception("Usuario no existe");
        }
    }

    public void delete(String id) throws Exception {
        PreparedStatement stm = db.prepareStatement("delete from usuario where id=?");
        stm.setString(1, id);

        if (db.executeUpdate(stm) == 0) {
            throw new Exception("Usuario no existe");
        }
    }

    /**
     * Metodo traductor. Si la fila trae nombre de funcionario, el objeto
     * construido es un Funcionario; si no, un Usuario plano (el administrador).
     */
    Usuario from(ResultSet rs) throws SQLException {
        String nombre = rs.getString("nombre");

        if (nombre == null) {
            Usuario u = new Usuario();
            u.setId(rs.getString("id"));
            u.setClave(rs.getString("clave"));
            u.setRol(rs.getString("rol"));
            return u;
        }

        Funcionario f = new Funcionario();
        f.setId(rs.getString("id"));
        f.setClave(rs.getString("clave"));
        f.setRol(rs.getString("rol"));
        f.setNombre(nombre);
        f.setTelefono(rs.getString("telefono"));
        return f;
    }
}
