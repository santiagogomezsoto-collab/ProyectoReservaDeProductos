package reservas.server;

import reservas.logic.Service;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

/** Servidor local de consulta. Argumentos: [puerto], por defecto 5000. */
public final class Servidor {
    public static void main(String[] args) {
        try {
            if (args.length > 1) throw new IllegalArgumentException("Uso: Servidor [puerto]");
            int puerto = args.length == 0 ? 5000 : Integer.parseInt(args[0]);
            if (puerto < 1 || puerto > 65535) throw new IllegalArgumentException("Puerto invalido");
            if (!Files.isReadable(Path.of("data.xml"))) {
                throw new IOException("No se encuentra data.xml; ejecutar desde la raiz del repositorio");
            }
            // Inicializar el singleton antes de publicar trabajos a los hilos.
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
                // No llamar Service.stop(): esta operacion es exclusivamente de lectura.
                workers.shutdown();
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
            System.exit(1);
        }
    }
}
