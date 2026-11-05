package reservas.client;

import reservas.protocol.CategoriaDto;
import reservas.protocol.Operaciones;
import reservas.protocol.Respuesta;
import reservas.protocol.Solicitud;

import java.io.*;
import java.net.*;

/** Cliente de consola temporal. Argumentos: [host] [puerto]. */
public final class ClienteCategorias {
    public static CategoriaDto[] consultar(String host, int puerto) throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), 3000);
            socket.setSoTimeout(10000);
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.flush();
                try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                    in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                            "maxdepth=5;maxrefs=50000;maxarray=10000;maxbytes=1048576;"
                                    + "reservas.protocol.Respuesta;reservas.protocol.CategoriaDto;!*"));
                    out.writeObject(new Solicitud(Operaciones.FIND_ALL_CATEGORIAS));
                    out.flush();
                    Object mensaje = in.readObject();
                    if (!(mensaje instanceof Respuesta respuesta)) throw new IOException("Respuesta invalida");
                    if (respuesta.error() != null) throw new IOException("Servidor: " + respuesta.error());
                    return respuesta.categorias();
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length > 2) throw new IllegalArgumentException("Uso: ClienteCategorias [host] [puerto]");
            String host = args.length > 0 ? args[0] : "127.0.0.1";
            int puerto = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
            if (puerto < 1 || puerto > 65535) throw new IllegalArgumentException("Puerto invalido");
            CategoriaDto[] categorias = consultar(host, puerto);
            System.out.println("Categorias recibidas: " + categorias.length);
            for (CategoriaDto categoria : categorias) {
                System.out.println(categoria.id() + "\t" + categoria.descripcion());
            }
        } catch (IOException | ClassNotFoundException | IllegalArgumentException e) {
            System.err.println("No se pudo consultar categorias: " + e.getMessage());
            System.exit(1);
        }
    }
}
