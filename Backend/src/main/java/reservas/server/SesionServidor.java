package reservas.server;

import reservas.logic.Usuario;
import reservas.protocol.CodigoError;
import reservas.protocol.CanalObjetos;
import reservas.protocol.ReservaCreada;
import java.net.Socket;
import java.io.IOException;

/** Una identidad validada pertenece a la conexion SYNC, nunca al DTO del cliente. */
final class SesionServidor {
    final String id;
    private final Socket sync;
    private Socket async;
    private EmisorAlertas emisor;
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
        if (async == socket) {
            if (emisor != null) emisor.close();
            emisor = null;
            async = null;
        }
    }

    synchronized void iniciarEnvios(Socket socket, CanalObjetos canal) throws Rechazo {
        if (!vigente || async != socket)
            throw new Rechazo(CodigoError.SESION_INVALIDA, "Sesion cerrada");
        emisor = new EmisorAlertas(socket, canal, id);
        emisor.iniciar();
    }

    synchronized void notificar(String creador, ReservaCreada evento) {
        if (vigente && emisor != null && usuario != null
                && Usuario.FUNCIONARIO.equals(usuario.getRol()) && !creador.equals(usuario.getId()))
            emisor.ofrecer(evento);
    }

    synchronized void cerrar() {
        vigente = false;
        usuario = null;
        if (emisor != null) emisor.close();
        emisor = null;
        cerrarSocket(async);
        async = null;
        cerrarSocket(sync);
    }

    static void cerrarSocket(Socket socket) {
        if (socket != null) try { socket.close(); } catch (IOException ignored) { }
    }
}
