package reservas;

import reservas.logic.Service;

import javax.swing.SwingUtilities;

/**
 * Arranque local temporal hasta integrar el Proxy y el servidor.
 * La persistencia es MySQL: la conexion sale de reservas.properties, que viaja
 * en el classpath, asi que ya no depende del directorio de trabajo.
 */
public class LocalApplication {
    public static void main(String[] args) {
        Service service = Service.instance();
        SwingUtilities.invokeLater(() -> Application.start(service, service::stop));
    }
}
