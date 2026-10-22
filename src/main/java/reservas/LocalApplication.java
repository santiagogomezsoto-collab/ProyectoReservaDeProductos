package reservas;

import reservas.logic.Service;

import javax.swing.SwingUtilities;

/**
 * Arranque local temporal hasta integrar el Proxy y el servidor.
 * Ejecutar con la raiz del repositorio como directorio de trabajo:
 * Service carga y guarda data.xml relativo a ese directorio.
 */
public class LocalApplication {
    public static void main(String[] args) {
        Service service = Service.instance();
        SwingUtilities.invokeLater(() -> Application.start(service, service::stop));
    }
}
