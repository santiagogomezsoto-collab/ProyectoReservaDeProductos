package reservas.client;

import reservas.FrontendApplication;
import reservas.Application;
import reservas.protocol.CodigoError;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;

/** Automatiza un proceso Swing real; nunca se distribuye con Frontend. */
public final class FrontendSmokeProcess {
    private static int etapa;
    private static Object campo(Object objeto, String nombre) throws Exception {
        Field f = objeto.getClass().getDeclaredField(nombre);
        f.setAccessible(true);
        return f.get(objeto);
    }
    public static void main(String[] args) throws Exception {
        for (String prohibida : new String[]{"reservas.logic.Service", "reservas.data.Database", "com.mysql.cj.jdbc.Driver"}) {
            try { Class.forName(prohibida); throw new AssertionError("Dependencia indebida: " + prohibida); }
            catch (ClassNotFoundException expected) { }
        }
        if (FrontendSmokeProcess.class.getResource("/reservas.properties") != null)
            throw new AssertionError("Configuracion JDBC en Frontend");
        String modo = args[1];
        if (modo.equals("real")) {
            try (ServiceProxy p = new ServiceProxy("127.0.0.1", Integer.parseInt(args[0]))) {
                var primera = p.findAllCategorias();
                if (primera.isEmpty()) throw new AssertionError("Sin categorias");
                try { p.login("__a34_inexistente__", "invalida"); throw new AssertionError("Login aceptado"); }
                catch (ServiceProxyException e) { if (e.getCodigo() != CodigoError.AUTENTICACION) throw e; }
                if (!primera.get(0).getId().equals(p.findAllCategorias().get(0).getId()))
                    throw new AssertionError("Consulta posterior distinta");
                System.out.println("TCP real: consultas consecutivas y login rechazado; categoria="
                        + primera.get(0).getId() + ", " + primera.get(0).getDescripcion());
            }
        }
        Thread watchdog = new Thread(() -> {
            try { Thread.sleep(25000); } catch (InterruptedException ignored) { }
            System.err.println("Timeout de prueba Swing"); System.exit(2);
        });
        watchdog.setDaemon(true); watchdog.start();
        SwingUtilities.invokeLater(() -> {
            Timer timer = new Timer(100, null);
            timer.setCoalesce(false); // Permite cerrar el dialogo modal de error durante doClick.
            timer.addActionListener(event -> {
                try {
                    for (Window w : Window.getWindows()) {
                        if (!w.isShowing()) continue;
                        if (etapa == 0 && w instanceof reservas.presentation.login.View) {
                            etapa = 1;
                            ((JTextField) campo(w, "idFld")).setText(modo.equals("real") ? "__a34_inexistente__" : modo);
                            ((JPasswordField) campo(w, "claveFld")).setText(modo.equals("real") ? "invalida" : "solo-prueba");
                            ((JButton) campo(w, "ingresarBtn")).doClick();
                        } else if (modo.equals("real") && etapa == 1 && w instanceof JDialog d && d.getTitle().equals("Error")) {
                            etapa = 2; d.dispose();
                        } else if (modo.equals("real") && etapa == 2 && w instanceof reservas.presentation.login.View) {
                            etapa = 3; timer.stop();
                            ((JButton) campo(w, "cancelarBtn")).doClick();
                            System.out.println("PASS Swing: error controlado y cancelacion; classpath sin Backend/JDBC");
                        } else if (!modo.equals("real") && w instanceof JFrame frame && etapa == 1) {
                            var root = frame.getContentPane();
                            var layout = (BorderLayout) root.getLayout();
                            Component alertas = layout.getLayoutComponent(BorderLayout.EAST);
                            JTabbedPane tabs = (JTabbedPane) layout.getLayoutComponent(BorderLayout.CENTER);
                            if (tabs.getTabCount() != (modo.equals("admin") ? 6 : 4) || Application.getAlertas() == null)
                                throw new AssertionError("Pantallas incompletas");
                            for (int i = 0; i < tabs.getTabCount(); i++) {
                                tabs.setSelectedIndex(i);
                                if (!alertas.isShowing() || alertas.getWidth() <= 0) throw new AssertionError("Alertas ocultas");
                            }
                            timer.stop(); etapa = 3;
                            System.out.println("PASS Swing " + modo + ": pestañas y alertas visibles; classpath sin Backend/JDBC");
                            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                        }
                    }
                } catch (Throwable e) { e.printStackTrace(); System.exit(1); }
            });
            timer.start();
        });
        FrontendApplication.main(new String[]{"127.0.0.1", args[0]});
    }
}
