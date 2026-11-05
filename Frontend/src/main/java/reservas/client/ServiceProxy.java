package reservas.client;

import reservas.logic.*;
import reservas.ia.ReservaExtraccion;
import reservas.protocol.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import static reservas.protocol.Operaciones.*;

/** Una conexion SYNC y una solicitud pendiente; no reintenta escrituras automaticamente. */
public final class ServiceProxy implements IService, AutoCloseable {
    private final Socket socket = new Socket();
    private final CanalObjetos canal;
    private final String sesionId;
    private final String host;
    private final int puerto;
    private volatile boolean autenticado;
    private SocketListener listener;
    private final AtomicBoolean cerrado = new AtomicBoolean();
    private final ExecutorService solicitudes = Executors.newSingleThreadExecutor(r -> {
        Thread hilo = new Thread(r, "reservas-sync");
        hilo.setDaemon(true);
        return hilo;
    });

    public ServiceProxy(String host, int puerto) { this(host, puerto, 30000); }

    /** timeoutMillis limita lecturas; el constructor debe invocarse fuera del EDT. */
    public ServiceProxy(String host, int puerto, int timeoutMillis) {
        this.host = host;
        this.puerto = puerto;
        if (host == null || host.isBlank() || puerto < 1 || puerto > 65535 || timeoutMillis <= 0)
            throw new IllegalArgumentException("Host, puerto o timeout invalidos");
        try {
            socket.connect(new InetSocketAddress(host, puerto), 3000);
            socket.setSoTimeout(timeoutMillis);
            canal = new CanalObjetos(socket);
            canal.enviar(new Identificacion(TipoConexion.SYNC, null));
            Object saludo = canal.recibir(FiltrosProtocolo.apertura());
            if (saludo instanceof Respuesta r && r.error() != null)
                throw new ServiceProxyException(r.codigoErrorControlado(), r.error(), true);
            if (!(saludo instanceof SesionCreada sesion))
                throw new IOException("Saludo invalido");
            sesionId = sesion.sesionId();
        } catch (ServiceProxyException e) {
            close();
            throw e;
        } catch (IOException | ClassNotFoundException | IllegalArgumentException e) {
            close();
            throw perdido("No se pudo conectar con el servidor. Revise host y puerto.");
        }
    }

    /** Secreto para asociar ASYNC en A5; nunca imprimirlo. No implica autenticacion. */
    public String getSesionId() { return sesionId; }
    public boolean isClosed() { return cerrado.get(); }

    /** Llamar despues del login y de preparar el receptor MVC. No hace I/O en el llamador. */
    public synchronized SocketListener iniciarAlertas(java.util.function.Consumer<ReservaCreada> receptor,
                                                     java.util.function.Consumer<String> errores) {
        if (isClosed() || !autenticado) throw new IllegalStateException("Se requiere una sesion autenticada");
        if (listener == null || listener.isClosed()) {
            listener = new SocketListener(host, puerto, sesionId, receptor, errores);
            listener.iniciar();
        }
        return listener;
    }

    public synchronized void detenerAlertas() {
        if (listener != null) listener.close();
        listener = null;
    }

    private static ServiceProxyException perdido(String mensaje) {
        return new ServiceProxyException(CodigoError.INTERNO, mensaje, true);
    }

    private <T extends Resultado> T pedir(int op, Argumentos args, Class<T> tipo) {
        if (isClosed()) throw perdido("La conexion esta cerrada. Inicie nuevamente el Frontend.");
        Solicitud solicitud = new Solicitud(op, args);
        try {
            return EsperaRemota.ejecutar(solicitudes, () -> intercambiar(solicitud, tipo));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            close();
            throw perdido("La solicitud fue interrumpida; su resultado no pudo confirmarse.");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ServiceProxyException error) throw error;
            close();
            throw perdido("No se pudo interpretar la respuesta del servidor.");
        } catch (RejectedExecutionException | CancellationException e) {
            throw perdido("La conexion esta cerrada. Inicie nuevamente el Frontend.");
        }
    }

    private <T extends Resultado> T intercambiar(Solicitud solicitud, Class<T> tipo) {
        if (isClosed()) throw perdido("La conexion esta cerrada.");
        try {
            canal.enviar(solicitud);
            Object objeto = canal.recibir(FiltrosProtocolo.respuestas());
            if (!(objeto instanceof Respuesta respuesta)) throw new IOException("Respuesta invalida");
            if (respuesta.error() != null) {
                boolean invalida = respuesta.codigoErrorControlado() == CodigoError.SESION_INVALIDA;
                if (invalida) close();
                throw new ServiceProxyException(respuesta.codigoErrorControlado(), respuesta.error(), invalida);
            }
            Resultado resultado = respuesta.resultadoTipado();
            Operaciones.validarResultado(solicitud.operacion(), resultado);
            return tipo.cast(resultado);
        } catch (ServiceProxyException e) {
            throw e; // Un rechazo de negocio no desincroniza la conexion.
        } catch (IOException | ClassNotFoundException | IllegalArgumentException | ClassCastException e) {
            close();
            throw perdido("Se perdio la comunicacion o se recibio una respuesta invalida. "
                    + "No se reintentara la operacion; su resultado puede no estar confirmado.");
        }
    }

    @Override public void close() {
        if (cerrado.compareAndSet(false, true)) {
            detenerAlertas();
            // Cerrar el socket desbloquea cualquier lectura en curso sin esperar su lock.
            try { socket.close(); } catch (IOException ignored) { }
            for (Runnable pendiente : solicitudes.shutdownNow())
                if (pendiente instanceof Future<?> future) future.cancel(false);
        }
    }

    private static <D, E> List<E> lista(D[] datos, Function<D, E> convertir) {
        return Arrays.stream(datos).map(convertir).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override public Usuario login(String id, String clave) throws Exception {
        Usuario usuario = ConvertidorDto.usuario(pedir(LOGIN, new Argumentos.Login(id, clave), Resultado.Usuario.class).usuario());
        autenticado = true;
        return usuario;
    }
    @Override public void changePassword(String id, String claveActual, String claveNueva) throws Exception {
        pedir(CHANGE_PASSWORD, new Argumentos.CambioClave(id, claveActual, claveNueva), Resultado.Confirmacion.class);
    }
    @Override public void create(Funcionario e) throws Exception {
        pedir(CREATE_FUNCIONARIO, new Argumentos.Funcionario(ConvertidorDto.funcionario(e)), Resultado.Confirmacion.class);
    }
    @Override public void update(Funcionario e) throws Exception {
        pedir(UPDATE_FUNCIONARIO, new Argumentos.Funcionario(ConvertidorDto.funcionario(e)), Resultado.Confirmacion.class);
    }
    @Override public void delete(Funcionario e) throws Exception {
        pedir(DELETE_FUNCIONARIO, new Argumentos.Funcionario(ConvertidorDto.funcionario(e)), Resultado.Confirmacion.class);
    }
    @Override public List<Funcionario> search(Funcionario e) {
        return lista(pedir(SEARCH_FUNCIONARIO, new Argumentos.Funcionario(ConvertidorDto.funcionario(e)),
                Resultado.Funcionarios.class).funcionarios(), ConvertidorDto::funcionario);
    }
    @Override public void create(CategoriaRecurso e) throws Exception {
        pedir(CREATE_CATEGORIA, new Argumentos.Categoria(ConvertidorDto.categoria(e)), Resultado.Confirmacion.class);
    }
    @Override public void update(CategoriaRecurso e) throws Exception {
        pedir(UPDATE_CATEGORIA, new Argumentos.Categoria(ConvertidorDto.categoria(e)), Resultado.Confirmacion.class);
    }
    @Override public void delete(CategoriaRecurso e) throws Exception {
        pedir(DELETE_CATEGORIA, new Argumentos.Categoria(ConvertidorDto.categoria(e)), Resultado.Confirmacion.class);
    }
    @Override public List<CategoriaRecurso> search(CategoriaRecurso e) {
        return lista(pedir(SEARCH_CATEGORIA, new Argumentos.Categoria(ConvertidorDto.categoria(e)),
                Resultado.Categorias.class).categorias(), ConvertidorDto::categoria);
    }
    @Override public List<CategoriaRecurso> findAllCategorias() {
        return lista(pedir(FIND_ALL_CATEGORIAS, null, Resultado.Categorias.class).categorias(), ConvertidorDto::categoria);
    }
    @Override public void create(Recurso e) throws Exception {
        pedir(CREATE_RECURSO, new Argumentos.Recurso(ConvertidorDto.recurso(e)), Resultado.Confirmacion.class);
    }
    @Override public void update(Recurso e) throws Exception {
        pedir(UPDATE_RECURSO, new Argumentos.Recurso(ConvertidorDto.recurso(e)), Resultado.Confirmacion.class);
    }
    @Override public void delete(Recurso e) throws Exception {
        pedir(DELETE_RECURSO, new Argumentos.Recurso(ConvertidorDto.recurso(e)), Resultado.Confirmacion.class);
    }
    @Override public List<Recurso> search(CategoriaRecurso categoria, String descripcion) {
        return lista(pedir(SEARCH_RECURSOS, new Argumentos.BuscarRecursos(ConvertidorDto.categoria(categoria), descripcion),
                Resultado.Recursos.class).recursos(), ConvertidorDto::recurso);
    }
    @Override public List<Reserva> findReservas(Funcionario funcionario) {
        return lista(pedir(FIND_RESERVAS, new Argumentos.Funcionario(ConvertidorDto.funcionario(funcionario)),
                Resultado.Reservas.class).reservas(), ConvertidorDto::reserva);
    }
    @Override public Reserva crearReserva(Funcionario funcionario, String actividad, LocalDate fecha,
            LocalTime horaInicio, LocalTime horaFin, List<CategoriaRecurso> categoriasSolicitadas) throws Exception {
        CategoriaDto[] categorias = categoriasSolicitadas.stream().map(ConvertidorDto::categoria).toArray(CategoriaDto[]::new);
        return ConvertidorDto.reserva(pedir(CREAR_RESERVA, new Argumentos.CrearReserva(
                ConvertidorDto.funcionario(funcionario), actividad, fecha, horaInicio, horaFin, categorias),
                Resultado.Reserva.class).reserva());
    }
    @Override public void cancelarReserva(Reserva e) throws Exception {
        pedir(CANCELAR_RESERVA, new Argumentos.Reserva(ConvertidorDto.reserva(e)), Resultado.Confirmacion.class);
    }
    @Override public List<Reserva> findReservasActivasPorFecha(LocalDate fecha) {
        return lista(pedir(FIND_RESERVAS_ACTIVAS_POR_FECHA, new Argumentos.Fecha(fecha),
                Resultado.Reservas.class).reservas(), ConvertidorDto::reserva);
    }
    @Override public List<Reserva> findReservasActivasPorSemana(LocalDate inicioSemana, LocalDate finSemana) {
        return lista(pedir(FIND_RESERVAS_ACTIVAS_POR_SEMANA, new Argumentos.Periodo(inicioSemana, finSemana),
                Resultado.Reservas.class).reservas(), ConvertidorDto::reserva);
    }
    @Override public Map<String, Long> estadisticasRecursosPorCategoria(LocalDate desde, LocalDate hasta) {
        return ConvertidorDto.categorias(pedir(ESTADISTICAS_RECURSOS_POR_CATEGORIA,
                new Argumentos.Periodo(desde, hasta), Resultado.EstadisticasCategorias.class));
    }
    @Override public Map<LocalDate, Long> estadisticasActividadesPorSemana(LocalDate desde, LocalDate hasta) {
        return ConvertidorDto.semanas(pedir(ESTADISTICAS_ACTIVIDADES_POR_SEMANA,
                new Argumentos.Periodo(desde, hasta), Resultado.EstadisticasSemanas.class));
    }
    @Override public ReservaExtraccion extraerReserva(String frase) {
        return ConvertidorDto.extraccion(pedir(EXTRAER_RESERVA,
                new Argumentos.ExtraerReserva(frase), Resultado.Extraccion.class).extraccion());
    }
}
