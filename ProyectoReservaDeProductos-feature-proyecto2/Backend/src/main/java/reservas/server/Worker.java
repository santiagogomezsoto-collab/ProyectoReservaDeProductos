package reservas.server;

import reservas.logic.Service;
import reservas.protocol.CategoriaDto;
import reservas.protocol.Operaciones;
import reservas.protocol.Respuesta;
import reservas.protocol.Solicitud;

import java.io.*;
import java.net.Socket;

/** Atiende una solicitud y cierra la conexion. */
public final class Worker implements Runnable {
    private final Socket socket;

    public Worker(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (socket) {
            socket.setSoTimeout(10000);
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.flush(); // Enviar cabecera antes de esperar la cabecera del cliente.
                try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                    in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                            "maxdepth=5;maxrefs=20;maxbytes=4096;reservas.protocol.Solicitud;!*"));
                    Respuesta respuesta;
                    try {
                        Object mensaje = in.readObject();
                        if (!(mensaje instanceof Solicitud solicitud)) {
                            respuesta = new Respuesta(null, "Solicitud invalida");
                        } else if (solicitud.operacion() != Operaciones.FIND_ALL_CATEGORIAS) {
                            respuesta = new Respuesta(null, "Operacion no soportada: " + solicitud.operacion());
                        } else {
                            CategoriaDto[] categorias = Service.instance().findAllCategorias().stream()
                                    .map(c -> new CategoriaDto(c.getId(), c.getDescripcion()))
                                    .toArray(CategoriaDto[]::new);
                            respuesta = new Respuesta(categorias, null);
                        }
                    } catch (ClassNotFoundException | InvalidClassException e) {
                        respuesta = new Respuesta(null, "Mensaje no admitido por el protocolo");
                    } catch (RuntimeException e) {
                        System.err.println("Error consultando categorias: " + e);
                        respuesta = new Respuesta(null, "No se pudieron consultar las categorias");
                    }
                    out.writeObject(respuesta);
                    out.flush();
                }
            }
        } catch (IOException e) {
            System.err.println("Conexion finalizada: " + e.getMessage());
        }
    }
}
