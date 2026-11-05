package reservas.server;

import reservas.logic.Usuario;
import reservas.protocol.CodigoError;
import java.net.Socket;
import java.io.IOException;

/** Una identidad validada pertenece a la conexion SYNC, nunca al DTO del cliente. */
final class SesionServidor {
    final String id;
    private final Socket sync;
    private Socket async;
    private Usuario usuario;
    private boolean vigente = true;

    SesionServidor(String id, Socket sync) { this.id = id; this.sync = sync; }
    synchronized Usuario usuario() { return usuario; }
    synchronized boolean vigente() { return vigente; }
    synchronized boolean tieneAsync() { return async != null; }

    synchronized void autenticar(Usuario usuario) throws Rechazo {
        if (!vigente) throw new Rechazo(CodigoError.SESION_INVALIDA, "Sesion cerrada");
        this.usuario = usuario;
    }

    synchronized void asociar(Socket socket) throws Rechazo {
        if (!vigente || usuario == null)
            throw new Rechazo(CodigoError.SESION_INVALIDA, "Sesion no disponible");
        if (async != null)
            throw new Rechazo(CodigoError.SESION_INVALIDA, "La sesion ya tiene conexion ASYNC");
        async = socket;
    }

    synchronized void separar(Socket socket) {
        if (async == socket) async = null;
    }

    synchronized void cerrar() {
        vigente = false;
        usuario = null;
        cerrarSocket(async);
        async = null;
        cerrarSocket(sync);
    }

    static void cerrarSocket(Socket socket) {
        if (socket != null) try { socket.close(); } catch (IOException ignored) { }
    }
}
