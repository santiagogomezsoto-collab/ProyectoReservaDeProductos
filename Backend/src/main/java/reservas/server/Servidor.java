package reservas.server;

import reservas.logic.IService;
import reservas.logic.Service;
import reservas.data.Database;
import java.io.IOException;
import java.net.*;
import java.util.Set;
import java.util.concurrent.*;

/** Acepta conexiones; cada Worker posee exclusivamente sus streams. */
public final class Servidor implements AutoCloseable {
    private final ServerSocket servidor;
    private final RegistroSesiones sesiones = new RegistroSesiones();
    private final Despachador despachador;
    private final Set<Socket> conexiones = ConcurrentHashMap.newKeySet();
    private final ExecutorService workers = new ThreadPoolExecutor(0, 64, 60L,
            TimeUnit.SECONDS, new SynchronousQueue<>());
    private volatile boolean cerrado;

    public Servidor(IService service, Object bloqueo, InetAddress direccion, int puerto) throws IOException {
        despachador = new Despachador(java.util.Objects.requireNonNull(service),
                java.util.Objects.requireNonNull(bloqueo));
        servidor = new ServerSocket();
        try { servidor.bind(new InetSocketAddress(direccion, puerto)); }
        catch (IOException e) { servidor.close(); workers.shutdownNow(); throw e; }
    }

    public int puerto() { return servidor.getLocalPort(); }
    int sesionesActivas() { return sesiones.cantidad(); }
    int conexionesAsync() { return sesiones.conexionesAsync(); }

    public void servir() throws IOException {
        while (!cerrado) {
            Socket cliente;
            try { cliente = servidor.accept(); }
            catch (SocketException e) { if (cerrado) return; throw e; }
            conexiones.add(cliente);
            if (cerrado) { liberar(cliente); continue; }
            try {
                workers.execute(new Worker(cliente, sesiones, despachador, () -> liberar(cliente)));
            } catch (RejectedExecutionException e) {
                liberar(cliente);
            }
        }
    }

    private void liberar(Socket cliente) {
        conexiones.remove(cliente);
        SesionServidor.cerrarSocket(cliente);
    }

    @Override public void close() {
        cerrado = true;
        try { servidor.close(); } catch (IOException ignored) { }
        conexiones.forEach(this::liberar);
        workers.shutdownNow();
        sesiones.cerrar();
    }

    /** Argumentos: [puerto] [direccion de escucha]. Por defecto solo loopback. */
    public static void main(String[] args) {
        try {
            if (args.length > 2) throw new IllegalArgumentException();
            int puerto = args.length == 0 ? 5000 : Integer.parseInt(args[0]);
            if (puerto < 1 || puerto > 65535) throw new IllegalArgumentException();
            InetAddress direccion = InetAddress.getByName(args.length < 2 ? "127.0.0.1" : args[1]);
            // Database comparte una Connection: bloquear operaciones completas.
            try (Servidor app = new Servidor(Service.instance(), Database.instance(), direccion, puerto)) {
                Thread cierre = new Thread(app::close, "cierre-servidor");
                Runtime.getRuntime().addShutdownHook(cierre);
                try {
                    System.out.println("Servidor escuchando en " + direccion.getHostAddress() + ":" + app.puerto());
                    app.servir();
                } finally {
                    try { Runtime.getRuntime().removeShutdownHook(cierre); }
                    catch (IllegalStateException ignored) { }
                }
            }
        } catch (Exception e) {
            System.err.println("No se pudo iniciar o mantener el servidor. Revise configuracion local y puerto.");
        }
    }
}
