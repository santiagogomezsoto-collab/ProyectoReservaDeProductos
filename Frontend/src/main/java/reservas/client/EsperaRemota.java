package reservas.client;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;

/** Conserva las llamadas sincrónicas del MVC sin realizar I/O en el EDT. */
final class EsperaRemota {
    private EsperaRemota() { }

    static <T> T ejecutar(ExecutorService executor, Callable<T> operacion)
            throws InterruptedException, ExecutionException {
        if (!SwingUtilities.isEventDispatchThread()) return executor.submit(operacion).get();
        Window owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        JDialog espera = new JDialog(owner, "Sistema de Reservas", Dialog.ModalityType.APPLICATION_MODAL);
        espera.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        panel.add(new JLabel("Consultando al servidor..."), BorderLayout.NORTH);
        JProgressBar progreso = new JProgressBar();
        progreso.setIndeterminate(true);
        panel.add(progreso, BorderLayout.CENTER);
        espera.setContentPane(panel);
        espera.pack();
        espera.setLocationRelativeTo(owner);
        FutureTask<T> tarea = new FutureTask<>(operacion) {
            @Override protected void done() { SwingUtilities.invokeLater(espera::dispose); }
        };
        try {
            executor.execute(tarea);
            // El dialogo modal procesa eventos y evita acciones reentrantes de las pantallas.
            if (!tarea.isDone()) espera.setVisible(true);
            return tarea.get();
        } finally { espera.dispose(); }
    }
}
