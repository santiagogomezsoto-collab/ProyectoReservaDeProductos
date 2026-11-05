package reservas;

import reservas.logic.Service;

import javax.swing.SwingUtilities;

/**
 * Herramienta local opcional del proyecto raiz; no es el arranque del Frontend.
 * El cliente independiente utiliza reservas.FrontendApplication en Frontend.
 * La persistencia es MySQL: la conexion sale de reservas.properties, que viaja
 * en el classpath, asi que ya no depende del directorio de trabajo.
 */
public class LocalApplication {
    public static void main(String[] args) {
        Service service = Service.instance();
        SwingUtilities.invokeLater(() -> Application.start(service, service::stop));
    }
}
