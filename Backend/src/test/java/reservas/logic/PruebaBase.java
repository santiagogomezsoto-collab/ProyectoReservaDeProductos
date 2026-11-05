package reservas.logic;

import reservas.data.Database;

import java.sql.PreparedStatement;

/**
 * Apoyo para las pruebas de integracion: deja la base de datos vacia antes de
 * cada prueba, para que cada una empiece desde un estado conocido.
 *
 * Cuando la persistencia era XML, Service.resetForTesting() bastaba porque
 * creaba una estructura de datos nueva en memoria. Con MySQL los datos
 * sobreviven entre pruebas, asi que hay que borrarlos explicitamente; si no,
 * una prueba que espera que la primera categoria sea CAT-000001 falla en
 * cuanto otra prueba haya creado categorias antes.
 */
final class PruebaBase {

    private PruebaBase() {
    }

    /**
     * Orden hijo -> padre, el inverso de la insercion. Las llaves foraneas son
     * ON DELETE RESTRICT, asi que borrar un padre antes que sus hijos falla.
     */
    private static final String[] TABLAS = {
            "reserva_recurso",
            "reserva",
            "recurso",
            "categoria_recurso",
            "funcionario",
            "usuario"
    };

    private static final String SUFIJO_PERMITIDO = "_test";

    /**
     * Borra todas las filas de las seis tablas.
     *
     * Se niega a trabajar si la base no termina en "_test". Este metodo borra
     * TODO, y ejecutarlo por descuido contra la base de desarrollo destruiria
     * los datos con los que se prueba la aplicacion.
     */
    static void limpiar() throws Exception {
        Database db = Database.instance();

        String nombre = db.nombreBaseDeDatos();
        if (nombre == null || !nombre.endsWith(SUFIJO_PERMITIDO)) {
            throw new IllegalStateException(
                    "Las pruebas de integracion borran todas las filas, y la base conectada es '"
                    + nombre + "', que no termina en " + SUFIJO_PERMITIDO + "."
                    + " Cree reservas_proyecto2_test, copie reservas-test.properties y ejecute:"
                    + " mvn verify -Dreservas.db.properties=/reservas-test.properties");
        }

        // El nombre de una tabla no puede ir como parametro de PreparedStatement:
        // los ? sustituyen valores, no identificadores. Aqui no hay riesgo de
        // inyeccion porque los nombres salen de la constante TABLAS, no de
        // ninguna entrada externa.
        synchronized (db) {
            for (String tabla : TABLAS) {
                PreparedStatement stm = db.prepareStatement("delete from " + tabla);
                db.executeUpdate(stm);
            }
        }
    }

    /** Reinicia el Service y deja la base vacia. Para usar en @BeforeEach. */
    static void reiniciar() throws Exception {
        Service.resetForTesting();
        limpiar();
    }
}
