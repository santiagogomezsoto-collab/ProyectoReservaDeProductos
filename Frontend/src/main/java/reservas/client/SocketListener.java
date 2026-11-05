package reservas.client;

import reservas.protocol.*;
import java.io.*;
import java.net.*;
import java.util.Objects;
import java.util.function.Consumer;

/** Receptor ASYNC independiente. Sus callbacks no deben bloquear ni tocar Swing directamente. */
public final class SocketListener implements Runnable, AutoCloseable {
    private final String host, sid;
    private final int puerto;
    private final Consumer<ReservaCreada> receptor;
    private final Consumer<String> errores;
    private final Thread hilo;
    private Socket socket;
    private volatile boolean cerrado, conectado;

    public SocketListener(String host, int puerto, String sid,
                          Consumer<ReservaCreada> receptor, Consumer<String> errores) {
        this.host = Objects.requireNonNull(host);
        this.puerto = puerto;
        this.sid = Objects.requireNonNull(sid);
        this.receptor = Objects.requireNonNull(receptor);
        this.errores = Objects.requireNonNull(errores);
        hilo = new Thread(this, "reservas-async");
        hilo.setDaemon(true);
    }
    public void iniciar() { hilo.start(); }
    public boolean isConectado() { return conectado; }
    public boolean isClosed() { return cerrado; }

    @Override public void run() {
        boolean avisado = false;
        try {
            while (!cerrado) {
                Socket actual = new Socket();
                synchronized (this) {
                    if (cerrado) { actual.close(); return; }
                    socket = actual;
                }
                try (actual) {
                    actual.connect(new InetSocketAddress(host, puerto), 3000);
                    actual.setSoTimeout(10000);
                    try (CanalObjetos canal = new CanalObjetos(actual)) {
                        canal.enviar(new Identificacion(TipoConexion.ASYNC, sid));
                        Object saludo = canal.recibir(FiltrosProtocolo.apertura());
                        if (saludo instanceof Respuesta) {
                            avisar("No se pudo asociar el canal de alertas a la sesion. Inicie nuevamente el Frontend.");
                            return; // No reintentar una sesion rechazada ni intentar autenticarla.
                        }
                        if (!(saludo instanceof SesionCreada s) || !sid.equals(s.sesionId()))
                            throw new StreamCorruptedException("Acuse invalido");
                        actual.setSoTimeout(0);
                        synchronized (this) {
                            if (cerrado) return;
                            conectado = true;
                        }
                        avisado = false;
                        while (!cerrado) {
                            Object mensaje = canal.recibir(FiltrosProtocolo.notificaciones());
                            if (!(mensaje instanceof ReservaCreada evento))
                                throw new StreamCorruptedException("Evento invalido");
                            if (!cerrado) receptor.accept(evento);
                        }
                    }
                } catch (ObjectStreamException | ClassNotFoundException | IllegalArgumentException e) {
                    avisar("El canal de alertas recibio un mensaje no admitido y se cerro.");
                    return;
                } catch (IOException e) {
                    if (!avisado) {
                        avisar("Se interrumpio el canal de alertas. Se intentara reconectar; los avisos durante la desconexion no se recuperan.");
                        avisado = true;
                    }
                } finally {
                    conectado = false;
                    synchronized (this) { if (socket == actual) socket = null; }
                }
                if (!cerrado) Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException e) {
            avisar("El receptor de alertas se detuvo.");
        } finally { close(); }
    }

    private void avisar(String mensaje) { if (!cerrado) errores.accept(mensaje); }

    @Override public synchronized void close() {
        cerrado = true;
        conectado = false;
        if (socket != null) try { socket.close(); } catch (IOException ignored) { }
        hilo.interrupt();
    }
}
