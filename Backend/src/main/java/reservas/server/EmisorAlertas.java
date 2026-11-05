package reservas.server;

import reservas.protocol.*;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/** Un escritor por ASYNC. Un cliente lento no detiene SYNC ni a otros clientes. */
final class EmisorAlertas implements AutoCloseable {
    private final Socket socket;
    private final CanalObjetos canal;
    private final ArrayBlockingQueue<ReservaCreada> pendientes = new ArrayBlockingQueue<>(128);
    private final AtomicBoolean cerrado = new AtomicBoolean();
    private final Thread hilo;

    EmisorAlertas(Socket socket, CanalObjetos canal, String sid) {
        this.socket = socket;
        this.canal = canal;
        hilo = new Thread(() -> enviar(sid), "emisor-alertas");
        hilo.setDaemon(true);
    }
    void iniciar() { hilo.start(); }
    void ofrecer(ReservaCreada evento) {
        if (!cerrado.get() && !pendientes.offer(evento)) close();
    }
    private void enviar(String sid) {
        try {
            // Siempre precede a cualquier evento que haya llegado durante el saludo.
            canal.enviar(new SesionCreada(sid));
            while (!cerrado.get()) canal.enviar(pendientes.take());
        } catch (IOException ignored) {
            // Worker detecta el cierre y libera la asociacion ASYNC.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally { close(); }
    }
    @Override public void close() {
        if (cerrado.compareAndSet(false, true)) {
            SesionServidor.cerrarSocket(socket);
            pendientes.clear();
            hilo.interrupt();
        }
    }
}
