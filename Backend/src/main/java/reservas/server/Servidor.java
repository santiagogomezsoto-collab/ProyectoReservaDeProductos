package reservas.server;

import reservas.logic.Service;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

/** Servidor local de consulta. Argumentos: [puerto], por defecto 5000. */
public final class Servidor {
    public static void main(String[] args) {
        try {
            if (args.length > 1) throw new IllegalArgumentException("Uso: Servidor [puerto]");
            int puerto = args.length == 0 ? 5000 : Integer.parseInt(args[0]);
            if (puerto < 1 || puerto > 65535) throw new IllegalArgumentException("Puerto invalido");
            // Inicializar el singleton antes de publicar trabajos a los hilos.
            // Aqui se abre la conexion con MySQL: si falla, mejor enterarse al
            // arrancar que en la primera peticion de un cliente.
            Service.instance();
            ThreadPoolExecutor workers = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(16));
            try (ServerSocket servidor = new ServerSocket()) {
                servidor.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), puerto));
                System.out.println("Servidor escuchando en 127.0.0.1:" + puerto);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket cliente = servidor.accept();
                    try {
                        workers.execute(new Worker(cliente));
                    } catch (RejectedExecutionException e) {
                        cliente.close();
                        System.err.println("Servidor ocupado: conexion rechazada");
                    }
                }
            } finally {
                // Service.stop() ya no persiste nada: con MySQL cada operacion
                // queda guardada en el momento.
                workers.shutdown();
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
            System.exit(1);
        } catch (IllegalStateException e) {
            // La lanza Database cuando no puede conectarse a MySQL o cuando
            // falta reservas.properties en el classpath.
            System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
            System.err.println("Revise que MySQL este corriendo y que exista"
                    + " Backend/src/main/resources/reservas.properties");
            System.exit(1);
        }
    }
}
