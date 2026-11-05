package reservas.data;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Acceso centralizado a MySQL. Patron Singleton: una sola instancia para todo
 * el Backend, con una sola conexion que los DAO reutilizan.
 *
 * Diferencias respecto al ejemplo visto en clase, y el motivo de cada una:
 *  1. Los metodos son synchronized. El ejemplo de clase es de un solo usuario;
 *     aqui el Backend atiende a varios Frontend con un pool de hilos y todos
 *     comparten esta unica conexion, que no es segura entre hilos.
 *  2. Los errores de SQL se propagan en vez de devolver 0 o null en silencio,
 *     porque el Service tiene que poder reportar la causa real.
 *  3. Expone transacciones, que el ejemplo no necesitaba y la creacion de una
 *     reserva si: escribe en reserva y en reserva_recurso, todo o nada.
 */
public class Database {
    private static Database theInstance;

    /** Unico punto de acceso a la instancia. Estatico: debe poder llamarse
     *  cuando todavia no existe ningun objeto Database. */
    public static synchronized Database instance() {
        if (theInstance == null) {
            theInstance = new Database();
        }
        return theInstance;
    }

    public static final String PROPERTIES_FILE_NAME = "/reservas.properties";

    /**
     * Nombre de la propiedad del sistema que permite apuntar a otro archivo de
     * conexion. Las pruebas de integracion la usan para trabajar contra una
     * base aparte y no tocar los datos reales:
     *
     *     mvn verify -Dreservas.db.properties=/reservas-test.properties
     */
    public static final String PROPERTIES_SYSTEM_PROPERTY = "reservas.db.properties";

    static String propertiesFileName() {
        String valor = System.getProperty(PROPERTIES_SYSTEM_PROPERTY, PROPERTIES_FILE_NAME).trim();
        // getResourceAsStream necesita la barra inicial para buscar desde la
        // raiz del classpath; sin ella buscaria dentro del paquete
        // reservas/data y no encontraria nada. Se admiten las dos formas para
        // que "-Dreservas.db.properties=reservas-test.properties" tambien sirva.
        return valor.startsWith("/") ? valor : "/" + valor;
    }

    /**
     * Cierra la conexion y olvida la instancia, para que la siguiente llamada a
     * instance() vuelva a leer el archivo de conexion. Solo para pruebas.
     */
    public static synchronized void resetForTesting() {
        if (theInstance != null) {
            try {
                theInstance.cnx.close();
            } catch (SQLException e) {
                System.err.println("No se pudo cerrar la conexion anterior: " + e.getMessage());
            }
            theInstance = null;
        }
    }

    Connection cnx;

    public Database() {
        cnx = this.getConnection();
    }

    public Connection getConnection() {
        String archivo = propertiesFileName();
        try (InputStream entrada = Database.class.getResourceAsStream(archivo)) {
            if (entrada == null) {
                throw new IOException("No se encontro " + archivo + " en el classpath."
                        + " Debe estar en Backend/src/main/resources/reservas.properties"
                        + " (copie reservas.properties.example y escriba su clave).");
            }
            Properties prop = new Properties();
            prop.load(entrada);

            String driver = requerido(prop, "database_driver");
            String server = requerido(prop, "database_server");
            String port = requerido(prop, "database_port");
            String database = requerido(prop, "database_name");
            String user = requerido(prop, "database_user");
            String password = prop.getProperty("database_password", "");

            // Registro explicito del driver. Desde JDBC 4 el driver se registra
            // solo al estar en el classpath, pero se conserva la propiedad
            // database_driver del ejemplo de clase.
            Class.forName(driver);

            String url = "jdbc:mysql://" + server + ":" + port + "/" + database;

            // Usuario y clave van como propiedades, no pegados en la URL: una
            // clave con @, & o : romperia la URL.
            Properties credenciales = new Properties();
            credenciales.setProperty("user", user);
            credenciales.setProperty("password", password);
            credenciales.setProperty("connectionTimeZone", "UTC");
            return DriverManager.getConnection(url, credenciales);

        } catch (IOException | ClassNotFoundException | SQLException e) {
            // Sin conexion el Backend no puede trabajar, pero se informa la
            // causa en lugar de terminar el proceso en silencio.
            throw new IllegalStateException("No se pudo conectar a MySQL: " + e.getMessage(), e);
        }
    }

    private String requerido(Properties prop, String clave) throws IOException {
        String valor = prop.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new IOException("Falta la propiedad " + clave + " en " + propertiesFileName());
        }
        return valor.trim();
    }

    /**
     * Nombre de la base a la que esta conectado. Lo usan las pruebas de
     * integracion para negarse a borrar datos de una base que no sea de prueba.
     */
    public synchronized String nombreBaseDeDatos() throws SQLException {
        return cnx.getCatalog();
    }

    public synchronized PreparedStatement prepareStatement(String statement) throws SQLException {
        return cnx.prepareStatement(statement);
    }

    public synchronized int executeUpdate(PreparedStatement statement) throws SQLException {
        statement.executeUpdate();
        return statement.getUpdateCount();
    }

    public synchronized ResultSet executeQuery(PreparedStatement statement) throws SQLException {
        return statement.executeQuery();
    }

    // ===================== Transacciones =====================
    // Una transaccion agrupa varias instrucciones SQL en una sola unidad:
    // o se confirman todas (commit) o no queda ninguna (rollback).
    //
    // Importante: synchronized protege cada llamada por separado, no una
    // transaccion completa. Quien abra una transaccion debe mantener el
    // bloqueo de esta instancia mientras dure:  synchronized (db) { ... }

    public synchronized void beginTransaction() throws SQLException {
        cnx.setAutoCommit(false);
    }

    public synchronized void commit() throws SQLException {
        try {
            cnx.commit();
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    /** Deshace la transaccion en curso. No propaga: se usa dentro de un catch,
     *  donde ya hay un error mas importante que reportar. */
    public synchronized void rollback() {
        try {
            cnx.rollback();
        } catch (SQLException e) {
            System.err.println("Fallo el rollback: " + e.getMessage());
        } finally {
            try {
                cnx.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("No se pudo restaurar autocommit: " + e.getMessage());
            }
        }
    }
}
