package reservas.server;

import reservas.protocol.*;
import java.io.*;
import java.net.*;

/** Saludo inicial y ciclo SYNC; ASYNC solo se asocia, sin notificaciones en A2. */
public final class Worker implements Runnable {
    private final Socket socket;
    private final RegistroSesiones sesiones;
    private final Despachador despachador;
    private final Runnable liberar;

    Worker(Socket socket, RegistroSesiones sesiones, Despachador despachador, Runnable liberar) {
        this.socket = socket;
        this.sesiones = sesiones;
        this.despachador = despachador;
        this.liberar = liberar;
    }

    @Override public void run() {
        SesionServidor sesion = null;
        boolean sync = false;
        try (socket) {
            socket.setSoTimeout(10000);
            try (CanalObjetos canal = new CanalObjetos(socket)) {
                try {
                    Object primero = canal.recibir(FiltrosProtocolo.identificacion());
                    if (!(primero instanceof Identificacion id))
                        throw new Rechazo(CodigoError.SOLICITUD_INVALIDA, "Se requiere identificacion");
                    if (id.tipo() == TipoConexion.SYNC) {
                        if (id.sesionId() != null)
                            throw new Rechazo(CodigoError.SESION_INVALIDA, "SYNC requiere una sesion nueva");
                        sync = true;
                        sesion = sesiones.crear(socket);
                        canal.enviar(new SesionCreada(sesion.id));
                        // Tiempo maximo de inactividad de SYNC. Reconexion crea otra sesion.
                        socket.setSoTimeout(300000);
                        while (sesion.vigente()) {
                            Object mensaje = canal.recibir(FiltrosProtocolo.solicitudes());
                            if (!(mensaje instanceof Solicitud solicitud))
                                throw new Rechazo(CodigoError.SOLICITUD_INVALIDA, "Se requiere una solicitud");
                            canal.enviar(despachador.atender(sesion, solicitud));
                        }
                    } else {
                        sesion = sesiones.asociar(id.sesionId(), socket);
                        canal.enviar(new SesionCreada(sesion.id));
                        // Solo esperar EOF: no se admiten solicitudes por ASYNC.
                        socket.setSoTimeout(0);
                        canal.recibir(info -> java.io.ObjectInputFilter.Status.REJECTED);
                        throw new Rechazo(CodigoError.SOLICITUD_INVALIDA, "ASYNC no admite solicitudes");
                    }
                } catch (Rechazo e) {
                    canal.enviar(Respuesta.fallo(e.codigo, e.getMessage()));
                } catch (ClassNotFoundException | ObjectStreamException e) {
                    canal.enviar(Respuesta.fallo(CodigoError.SOLICITUD_INVALIDA, "Mensaje no admitido"));
                }
            }
        } catch (EOFException | SocketException | SocketTimeoutException ignored) {
            // Desconexion o inactividad: limpiar sin imprimir datos del cliente.
        } catch (IOException e) {
            System.err.println("Conexion finalizada por un error de comunicacion");
        } finally {
            if (sesion != null) {
                if (sync) sesiones.eliminar(sesion);
                else sesion.separar(socket);
            }
            liberar.run();
        }
    }
}
