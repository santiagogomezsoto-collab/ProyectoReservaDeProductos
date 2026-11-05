package reservas;

import reservas.client.ServiceProxy;
import reservas.client.ServiceProxyException;
import reservas.presentation.Sesion;
import reservas.presentation.alertas.Alerta;
import javax.swing.*;
import java.awt.Window;

/** Entrada remota independiente. No carga Backend, JDBC ni configuracion MySQL. */
public final class FrontendApplication {
    private FrontendApplication() { }

    public static void main(String[] args) {
        if (args.length == 1 && "--help".equals(args[0])) {
            System.out.println("Uso: reservas.FrontendApplication [host] [puerto]");
            return;
        }
        final ServiceProxy proxy;
        try {
            if (args.length > 2) throw new IllegalArgumentException();
            String host = args.length > 0 ? args[0] : "127.0.0.1";
            int puerto = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
            proxy = new ServiceProxy(host, puerto); // Conexion fuera del EDT.
        } catch (IllegalArgumentException e) {
            mostrarError("Uso: reservas.FrontendApplication [host] [puerto entre 1 y 65535]");
            return;
        } catch (ServiceProxyException e) {
            mostrarError(e.getMessage());
            return;
        }
        Thread cierre = new Thread(proxy::close, "cierre-frontend");
        Runtime.getRuntime().addShutdownHook(cierre);
        Runnable cerrar = () -> {
            proxy.close();
            try { Runtime.getRuntime().removeShutdownHook(cierre); }
            catch (IllegalStateException ignored) { }
        };
        SwingUtilities.invokeLater(() -> {
            Thread.UncaughtExceptionHandler anterior = Thread.currentThread().getUncaughtExceptionHandler();
            Thread.currentThread().setUncaughtExceptionHandler((hilo, error) -> {
                if (error instanceof ServiceProxyException remoto) mostrarError(remoto.getMessage());
                else anterior.uncaughtException(hilo, error);
            });
            try {
                Application.start(proxy, cerrar);
                if (!Sesion.isLoggedIn()) cerrar.run(); // Cancelar/cerrar el login tambien libera SYNC.
                else proxy.iniciarAlertas(evento -> Application.getAlertas().agregar(new Alerta(
                        evento.id(), evento.encargado(), evento.actividad(), evento.fecha().toString(),
                        evento.desde().toString(), evento.hasta().toString(), java.util.List.of(evento.recursos()))),
                        FrontendApplication::mostrarError);
            } catch (RuntimeException e) {
                cerrar.run();
                for (Window ventana : Window.getWindows()) ventana.dispose();
                mostrarError(e instanceof ServiceProxyException ? e.getMessage() : "No se pudo iniciar la interfaz.");
            }
        });
    }

    private static void mostrarError(String mensaje) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, mensaje,
                "Sistema de Reservas", JOptionPane.ERROR_MESSAGE));
    }
}
