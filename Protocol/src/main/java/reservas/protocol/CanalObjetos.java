package reservas.protocol;

import java.io.*;
import java.net.Socket;

/**
 * Un lector y un escritor por conexion. Conserva el formato ObjectStream de A1.
 * Los limites son por mensaje, no acumulativos durante toda la sesion.
 */
public final class CanalObjetos implements AutoCloseable {
    private static final long MAX_BYTES = 4L * 1024 * 1024;
    private final ObjectOutputStream salida;
    private final EntradaLimitada limitada;
    private final ObjectInputStream entrada;
    private ObjectInputFilter filtro;
    private long referencias, baseReferencias, bytes, baseBytes;

    public CanalObjetos(Socket socket) throws IOException {
        salida = new ObjectOutputStream(socket.getOutputStream());
        salida.flush();
        limitada = new EntradaLimitada(socket.getInputStream());
        entrada = new ObjectInputStream(limitada);
        entrada.setObjectInputFilter(info -> {
            referencias = info.references();
            bytes = info.streamBytes();
            if (filtro == null) return ObjectInputFilter.Status.REJECTED;
            return filtro.checkInput(new ObjectInputFilter.FilterInfo() {
                public Class<?> serialClass() { return info.serialClass(); }
                public long arrayLength() { return info.arrayLength(); }
                public long depth() { return info.depth(); }
                public long references() { return referencias - baseReferencias; }
                public long streamBytes() { return bytes - baseBytes; }
            });
        });
    }

    public Object recibir(ObjectInputFilter filtro) throws IOException, ClassNotFoundException {
        this.filtro = java.util.Objects.requireNonNull(filtro);
        baseReferencias = referencias;
        baseBytes = bytes;
        limitada.reiniciar();
        return entrada.readObject();
    }

    public synchronized void enviar(Object mensaje) throws IOException {
        // Evitar referencias a objetos de respuestas anteriores y liberar la cache.
        salida.reset();
        salida.writeObject(mensaje);
        salida.flush();
    }

    @Override public void close() throws IOException {
        try { entrada.close(); } finally { salida.close(); }
    }

    private static final class EntradaLimitada extends FilterInputStream {
        private long restantes = MAX_BYTES;
        EntradaLimitada(InputStream in) { super(in); }
        void reiniciar() { restantes = MAX_BYTES; }
        private void verificar() throws IOException {
            if (restantes <= 0) throw new StreamCorruptedException("Mensaje demasiado grande");
        }
        @Override public int read() throws IOException {
            verificar();
            int dato = in.read();
            if (dato >= 0) restantes--;
            return dato;
        }
        @Override public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) return 0;
            verificar();
            int leidos = in.read(b, off, (int) Math.min(len, restantes));
            if (leidos > 0) restantes -= leidos;
            return leidos;
        }
        @Override public long skip(long n) throws IOException {
            if (n <= 0) return 0;
            verificar();
            long saltados = in.skip(Math.min(n, restantes));
            restantes -= saltados;
            return saltados;
        }
    }
}
