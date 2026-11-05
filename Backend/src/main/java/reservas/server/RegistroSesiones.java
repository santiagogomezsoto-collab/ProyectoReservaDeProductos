package reservas.server;

import reservas.protocol.CodigoError;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

final class RegistroSesiones {
    private final ConcurrentHashMap<String, SesionServidor> sesiones = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    SesionServidor crear(Socket socket) {
        while (true) {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            SesionServidor sesion = new SesionServidor(id, socket);
            if (sesiones.putIfAbsent(id, sesion) == null) return sesion;
        }
    }

    SesionServidor asociar(String id, Socket socket) throws Rechazo {
        SesionServidor sesion = sesiones.get(id);
        if (sesion == null) throw new Rechazo(CodigoError.SESION_INVALIDA, "Sesion no disponible");
        sesion.asociar(socket);
        return sesion;
    }

    void eliminar(SesionServidor sesion) {
        sesiones.remove(sesion.id, sesion);
        sesion.cerrar();
    }

    void cerrar() { sesiones.values().forEach(this::eliminar); }
    int cantidad() { return sesiones.size(); }
    int conexionesAsync() { return (int) sesiones.values().stream().filter(SesionServidor::tieneAsync).count(); }
}
