package reservas.server;

import org.junit.jupiter.api.*;
import reservas.logic.*;
import reservas.ia.ReservaExtraccion;
import reservas.protocol.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;
import static reservas.protocol.Operaciones.*;

/** TCP real con IService simulado: no carga Service, Database ni usuarios MySQL. */
class ServidorTest {
    static final CategoriaDto CAT = new CategoriaDto("C1", "Equipo");
    static final FuncionarioDto F1 = new FuncionarioDto("f1", "FUNCIONARIO", "Uno", "1");
    static final FuncionarioDto F2 = new FuncionarioDto("f2", "FUNCIONARIO", "Dos", "2");
    static final RecursoDto REC = new RecursoDto("R1", CAT, "Recurso");
    static final LocalDate FECHA = LocalDate.of(2030, 1, 1);
    static final ReservaDto PROPIA = reserva("propia", F1);
    static final ReservaDto AJENA = reserva("ajena", F2);
    final Simulado simulado = new Simulado();
    Servidor servidor;
    ExecutorService ejecutor;
    Future<?> aceptador;

    static ReservaDto reserva(String id, FuncionarioDto funcionario) {
        return new ReservaDto(id, funcionario, "Datos reales del simulado", FECHA,
                LocalTime.of(8, 0), LocalTime.of(9, 0), new RecursoDto[]{REC}, "ACTIVA");
    }

    @BeforeEach void iniciar() throws Exception {
        servidor = new Servidor(simulado.service(), new Object(), InetAddress.getLoopbackAddress(), 0);
        ejecutor = Executors.newCachedThreadPool();
        aceptador = ejecutor.submit(() -> { servidor.servir(); return null; });
    }

    @AfterEach void cerrar() throws Exception {
        servidor.close();
        aceptador.get(3, TimeUnit.SECONDS);
        ejecutor.shutdownNow();
        esperar(() -> servidor.sesionesActivas() == 0);
    }

    final class Cliente implements AutoCloseable {
        final Socket socket;
        final CanalObjetos canal;
        final Object saludo;
        final String sesion;
        Cliente(TipoConexion tipo, String id) throws Exception {
            socket = new Socket(InetAddress.getLoopbackAddress(), servidor.puerto());
            socket.setSoTimeout(3000);
            canal = new CanalObjetos(socket);
            canal.enviar(new Identificacion(tipo, id));
            saludo = canal.recibir(FiltrosProtocolo.apertura());
            sesion = saludo instanceof SesionCreada s ? s.sesionId() : null;
        }
        Respuesta pedir(int op, Argumentos args) throws Exception {
            canal.enviar(new Solicitud(op, args));
            return (Respuesta) canal.recibir(FiltrosProtocolo.respuestas());
        }
        void login(String id) throws Exception {
            assertNull(pedir(LOGIN, new Argumentos.Login(id, "clave-prueba")).error());
        }
        @Override public void close() throws IOException { socket.close(); }
    }
    Cliente sync() throws Exception { return new Cliente(TipoConexion.SYNC, null); }
    static void esperar(BooleanSupplier condicion) throws Exception {
        long fin = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condicion.getAsBoolean() && System.nanoTime() < fin) Thread.sleep(10);
        assertTrue(condicion.getAsBoolean(), "No se completo la limpieza/asociacion");
    }

    static void rechazoTransporte(CanalObjetos canal, Object objeto) throws Exception {
        try {
            canal.enviar(objeto);
            Object mensaje = canal.recibir(FiltrosProtocolo.respuestas());
            assertInstanceOf(Respuesta.class, mensaje);
            assertEquals(CodigoError.SOLICITUD_INVALIDA, ((Respuesta) mensaje).codigoErrorControlado());
        } catch (SocketTimeoutException e) {
            fail("El servidor no rechazo ni cerro la conexion", e);
        } catch (IOException expected) {
            // Un filtro puede cerrar antes de que el emisor termine el objeto.
        }
    }

    static Argumentos argumentos(int op) {
        return switch (op) {
            case FIND_ALL_CATEGORIAS -> null;
            case LOGIN -> new Argumentos.Login("f1", "clave-prueba");
            case CHANGE_PASSWORD -> new Argumentos.CambioClave("f1", "clave-prueba", "otra");
            case CREATE_FUNCIONARIO, UPDATE_FUNCIONARIO, DELETE_FUNCIONARIO, SEARCH_FUNCIONARIO,
                 FIND_RESERVAS -> new Argumentos.Funcionario(F1);
            case CREATE_CATEGORIA, UPDATE_CATEGORIA, DELETE_CATEGORIA, SEARCH_CATEGORIA -> new Argumentos.Categoria(CAT);
            case CREATE_RECURSO, UPDATE_RECURSO, DELETE_RECURSO -> new Argumentos.Recurso(REC);
            case SEARCH_RECURSOS -> new Argumentos.BuscarRecursos(CAT, "buscar");
            case CREAR_RESERVA -> new Argumentos.CrearReserva(F1, "Actividad", FECHA,
                    LocalTime.of(10, 0), LocalTime.of(11, 0), new CategoriaDto[]{CAT});
            case CANCELAR_RESERVA -> new Argumentos.Reserva(PROPIA);
            case FIND_RESERVAS_ACTIVAS_POR_FECHA -> new Argumentos.Fecha(FECHA);
            case FIND_RESERVAS_ACTIVAS_POR_SEMANA, ESTADISTICAS_RECURSOS_POR_CATEGORIA,
                 ESTADISTICAS_ACTIVIDADES_POR_SEMANA -> new Argumentos.Periodo(FECHA, FECHA.plusDays(6));
            case EXTRAER_RESERVA -> new Argumentos.ExtraerReserva("Reservar equipo");
            default -> throw new AssertionError();
        };
    }

    @Test void sesionTecnicaNoAutenticaYPersistencia() throws Exception {
        try (Cliente c = sync()) {
            assertNotNull(c.sesion);
            assertEquals(32, Base64.getUrlDecoder().decode(c.sesion).length);
            assertEquals(CAT, c.pedir(1, null).categorias()[0]);
            assertEquals(CAT, c.pedir(1, null).categorias()[0]);
            assertNull(c.pedir(CHANGE_PASSWORD, argumentos(CHANGE_PASSWORD)).error());
            assertEquals(CodigoError.AUTENTICACION, c.pedir(CHANGE_PASSWORD,
                    new Argumentos.CambioClave("f1", "incorrecta", "otra")).codigoErrorControlado());
            int llamadas = simulado.llamadas.size();
            for (int op = 4; op <= 23; op++)
                assertEquals(CodigoError.AUTENTICACION, c.pedir(op, argumentos(op)).codigoErrorControlado());
            assertEquals(llamadas, simulado.llamadas.size());
            assertEquals(CodigoError.AUTENTICACION,
                    c.pedir(LOGIN, new Argumentos.Login("f1", "incorrecta")).codigoErrorControlado());
            assertEquals(CodigoError.AUTENTICACION,
                    c.pedir(LOGIN, new Argumentos.Login("inexistente", "clave-prueba")).codigoErrorControlado());
            assertEquals(CodigoError.AUTENTICACION,
                    c.pedir(LOGIN, new Argumentos.Login("rol-invalido", "clave-prueba")).codigoErrorControlado());
            c.login("f1");
            assertEquals(CodigoError.SOLICITUD_INVALIDA,
                    c.pedir(LOGIN, new Argumentos.Login("admin", "clave-prueba")).codigoErrorControlado());
            assertEquals(CodigoError.NO_AUTORIZADO, c.pedir(CREATE_CATEGORIA, argumentos(CREATE_CATEGORIA)).codigoErrorControlado());
        }
    }

    @Test void despachoDeLas23OperacionesConTiposYArgumentos() throws Exception {
        Set<Integer> verificados = new HashSet<>();
        try (Cliente admin = sync(); Cliente f = sync()) {
            Respuesta login = admin.pedir(LOGIN, new Argumentos.Login("admin", "clave-prueba"));
            assertEquals("ADMIN", ((Resultado.Usuario) login.resultadoTipado()).usuario().rol());
            verificados.add(LOGIN);
            f.login("f1");
            for (int op : new int[]{1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 19, 20, 21, 22}) {
                Argumentos a = op == CHANGE_PASSWORD
                        ? new Argumentos.CambioClave("admin", "clave-prueba", "otra") : argumentos(op);
                Respuesta r = admin.pedir(op, a);
                assertNull(r.error(), "Operacion " + op);
                Operaciones.validarResultado(op, r.resultadoTipado());
                assertEquals(op, simulado.ultima().operacion);
                verificados.add(op);
            }
            for (int op : new int[]{11, 15, 16, 17, 18, 19, 20, 21, 22, 23}) {
                Respuesta r = f.pedir(op, argumentos(op));
                assertNull(r.error(), "Operacion " + op);
                Operaciones.validarResultado(op, r.resultadoTipado());
                assertEquals(op, simulado.ultima().operacion);
                verificados.add(op);
                if (op == CREAR_RESERVA) {
                    Object[] args = simulado.ultima().args;
                    assertEquals("f1", ((Funcionario) args[0]).getId());
                    assertEquals("", ((Funcionario) args[0]).getClave());
                    assertEquals("Actividad", args[1]);
                    assertEquals(FECHA, args[2]);
                    assertEquals(LocalTime.of(10, 0), args[3]);
                    assertEquals(LocalTime.of(11, 0), args[4]);
                    assertEquals("C1", ((CategoriaRecurso) ((List<?>) args[5]).get(0)).getId());
                }
                if (op == CANCELAR_RESERVA)
                    assertEquals("Datos reales del simulado", ((Reserva) simulado.ultima().args[0]).getActividad());
            }
        }
        assertEquals(23, verificados.size());
    }

    @Test void autorizacionPorRolEIdentidadDelServidor() throws Exception {
        try (Cliente f = sync(); Cliente admin = sync()) {
            f.login("f1");
            admin.login("admin");
            for (int op : new int[]{4, 5, 6, 7, 8, 9, 10, 12, 13, 14})
                assertEquals(CodigoError.NO_AUTORIZADO, f.pedir(op, argumentos(op)).codigoErrorControlado());
            for (int op : new int[]{16, 17, 18, 23})
                assertEquals(CodigoError.NO_AUTORIZADO, admin.pedir(op, argumentos(op)).codigoErrorControlado());
            int llamadas = simulado.llamadas.size();
            assertEquals(CodigoError.NO_AUTORIZADO,
                    f.pedir(FIND_RESERVAS, new Argumentos.Funcionario(F2)).codigoErrorControlado());
            assertEquals(CodigoError.NO_AUTORIZADO, f.pedir(CREAR_RESERVA,
                    new Argumentos.CrearReserva(F2, "falsa", FECHA, LocalTime.NOON,
                            LocalTime.of(13, 0), new CategoriaDto[]{CAT})).codigoErrorControlado());
            assertEquals(CodigoError.NO_AUTORIZADO, f.pedir(CANCELAR_RESERVA,
                    new Argumentos.Reserva(AJENA)).codigoErrorControlado());
            assertEquals(CodigoError.NO_AUTORIZADO, f.pedir(CHANGE_PASSWORD,
                    new Argumentos.CambioClave("f2", "clave-prueba", "nueva")).codigoErrorControlado());
            assertEquals(llamadas, simulado.llamadas.size());
            // Id de reserva ajena con propietario falsificado: tampoco pasa.
            assertEquals(CodigoError.NO_AUTORIZADO, f.pedir(CANCELAR_RESERVA,
                    new Argumentos.Reserva(reserva("ajena", F1))).codigoErrorControlado());
            assertEquals(FIND_RESERVAS, simulado.ultima().operacion);
            FuncionarioDto falsoRol = new FuncionarioDto("f1", "ADMIN", "falso", "falso");
            assertNull(f.pedir(FIND_RESERVAS, new Argumentos.Funcionario(falsoRol)).error());
            assertEquals("FUNCIONARIO", ((Funcionario) simulado.ultima().args[0]).getRol());
            assertEquals("Uno", ((Funcionario) simulado.ultima().args[0]).getNombre());
        }
    }

    @Test void asociacionAsyncYDesconexionLimpianSesiones() throws Exception {
        try (Cliente invalido = new Cliente(TipoConexion.ASYNC, "inexistente")) {
            assertEquals(CodigoError.SESION_INVALIDA, ((Respuesta) invalido.saludo).codigoErrorControlado());
        }
        Cliente sync = sync();
        try {
            try (Cliente anonimo = new Cliente(TipoConexion.ASYNC, sync.sesion)) {
                assertEquals(CodigoError.SESION_INVALIDA, ((Respuesta) anonimo.saludo).codigoErrorControlado());
            }
            sync.login("f1");
            try (Cliente async = new Cliente(TipoConexion.ASYNC, sync.sesion)) {
                assertEquals(sync.sesion, async.sesion);
                assertEquals(1, servidor.conexionesAsync());
                try (Cliente duplicado = new Cliente(TipoConexion.ASYNC, sync.sesion)) {
                    assertEquals(CodigoError.SESION_INVALIDA, ((Respuesta) duplicado.saludo).codigoErrorControlado());
                }
                assertNull(sync.pedir(1, null).error());
            }
            esperar(() -> servidor.conexionesAsync() == 0);
            assertEquals(1, servidor.sesionesActivas());
            try (Cliente async = new Cliente(TipoConexion.ASYNC, sync.sesion)) {
                sync.close();
                assertThrows(IOException.class, () -> async.canal.recibir(FiltrosProtocolo.respuestas()));
            }
            esperar(() -> servidor.sesionesActivas() == 0);
            try (Cliente vencida = new Cliente(TipoConexion.ASYNC, sync.sesion)) {
                assertEquals(CodigoError.SESION_INVALIDA, ((Respuesta) vencida.saludo).codigoErrorControlado());
            }
        } finally { sync.close(); }
    }

    @Test void dosClientesConcurrentesConBloqueoCompartido() throws Exception {
        simulado.demora = true;
        CountDownLatch listos = new CountDownLatch(2);
        Callable<String> trabajo = () -> {
            try (Cliente c = sync()) {
                listos.countDown();
                assertTrue(listos.await(3, TimeUnit.SECONDS));
                c.login("f1");
                for (int i = 0; i < 20; i++) assertNull(c.pedir(1, null).error());
                return c.sesion;
            }
        };
        Future<String> a = ejecutor.submit(trabajo);
        Future<String> b = ejecutor.submit(trabajo);
        assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
        assertEquals(1, simulado.maximo.get(), "El servicio compartido no debe ejecutarse simultaneamente");
    }

    @Test void erroresControladosNoRompenElCicloNiExponenDetalles() throws Exception {
        try (Cliente c = sync()) {
            c.login("admin");
            simulado.fallo = new Exception("clave SQL interna NO_PUBLICAR");
            Respuesta r = c.pedir(CREATE_CATEGORIA, argumentos(CREATE_CATEGORIA));
            assertEquals(CodigoError.NEGOCIO, r.codigoErrorControlado());
            assertFalse(r.error().contains("NO_PUBLICAR"));
            simulado.fallo = new IllegalStateException("NO_PUBLICAR");
            assertEquals(CodigoError.INTERNO, c.pedir(1, null).codigoErrorControlado());
            simulado.fallo = new java.sql.SQLException("NO_PUBLICAR");
            assertEquals(CodigoError.INTERNO, c.pedir(CREATE_CATEGORIA, argumentos(CREATE_CATEGORIA)).codigoErrorControlado());
            simulado.fallo = null;
            assertEquals(CAT, c.pedir(1, null).categorias()[0]);
        }
    }

    @Test void filtrosSaludosInvalidosYSeparacionDeCanales() throws Exception {
        try (Cliente identificacionArbitraria = new Cliente(TipoConexion.SYNC, "admin")) {
            assertEquals(CodigoError.SESION_INVALIDA,
                    ((Respuesta) identificacionArbitraria.saludo).codigoErrorControlado());
        }
        try (Cliente c = sync()) {
            rechazoTransporte(c.canal, new File("objeto-no-permitido"));
        }
        esperar(() -> servidor.sesionesActivas() == 0);
        assertTrue(simulado.llamadas.isEmpty());
        try (Cliente c = sync()) {
            c.login("f1");
            try (Cliente async = new Cliente(TipoConexion.ASYNC, c.sesion)) {
                int llamadas = simulado.llamadas.size();
                rechazoTransporte(async.canal, new Solicitud(1));
                esperar(() -> servidor.conexionesAsync() == 0);
                assertEquals(llamadas, simulado.llamadas.size());
            }
            assertNull(c.pedir(1, null).error());
        }
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), servidor.puerto())) {
            socket.setSoTimeout(3000);
            CanalObjetos canal = new CanalObjetos(socket);
            rechazoTransporte(canal, new Solicitud(1)); // El saludo es obligatorio en A2.
        }
    }

    @Test void limitesPorMensajePermitenMasDeCuatroMiBAcumulados() throws Exception {
        try (Cliente c = sync()) {
            c.login("f1");
            String frase = "a".repeat(15000);
            for (int i = 0; i < 300; i++)
                assertNull(c.pedir(EXTRAER_RESERVA, new Argumentos.ExtraerReserva(frase)).error());
            assertNull(c.pedir(1, null).error());
        }
    }

    @Test void cierreServidorLiberaConexionesPendientes() throws Exception {
        try (Cliente c = sync(); Socket sinSaludo = new Socket(InetAddress.getLoopbackAddress(), servidor.puerto())) {
            c.login("f1");
            servidor.close();
            assertThrows(IOException.class, () -> c.canal.recibir(FiltrosProtocolo.respuestas()));
            esperar(() -> servidor.sesionesActivas() == 0);
        }
    }

    @Test void limiteDeUnMensajeYRespuestasSinCredenciales() throws Exception {
        try (Cliente c = sync()) {
            Respuesta respuesta = c.pedir(LOGIN, argumentos(LOGIN));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(respuesta); }
            assertFalse(bytes.toString(java.nio.charset.StandardCharsets.ISO_8859_1).contains("NO_TRANSMITIR"));
            int llamadas = simulado.llamadas.size();
            // Las cadenas no siempre pasan por ObjectInputFilter: el limite de bytes tambien aplica.
            rechazoTransporte(c.canal, "x".repeat(4 * 1024 * 1024 + 100));
            esperar(() -> servidor.sesionesActivas() == 0);
            assertEquals(llamadas, simulado.llamadas.size());
        }
    }

    record Llamada(int operacion, Object[] args) { }
    static final class Simulado implements InvocationHandler {
        final List<Llamada> llamadas = Collections.synchronizedList(new ArrayList<>());
        final AtomicInteger activas = new AtomicInteger(), maximo = new AtomicInteger();
        volatile Exception fallo;
        volatile boolean demora;
        IService service() {
            return (IService) java.lang.reflect.Proxy.newProxyInstance(IService.class.getClassLoader(), new Class<?>[]{IService.class}, this);
        }
        Llamada ultima() { return llamadas.get(llamadas.size() - 1); }
        public Object invoke(Object proxy, Method metodo, Object[] args) throws Throwable {
            int n = activas.incrementAndGet();
            maximo.accumulateAndGet(n, Math::max);
            try {
                if (demora) Thread.sleep(3);
                int op = codigo(metodo);
                llamadas.add(new Llamada(op, args));
                if (fallo != null) throw fallo;
                return switch (op) {
                    case CHANGE_PASSWORD -> {
                        if (!"clave-prueba".equals(args[1])) throw new Exception("Credencial invalida");
                        yield null;
                    }
                    case LOGIN -> {
                        if (!"clave-prueba".equals(args[1])) throw new Exception("Credencial invalida");
                        yield switch ((String) args[0]) {
                            case "admin" -> new Usuario("admin", "NO_TRANSMITIR", Usuario.ADMIN);
                            case "f1" -> new Funcionario("f1", "NO_TRANSMITIR", "Uno", "1");
                            case "f2" -> new Funcionario("f2", "NO_TRANSMITIR", "Dos", "2");
                            case "rol-invalido" -> new Usuario("rol-invalido", "", "INVENTADO");
                            default -> throw new Exception("No existe");
                        };
                    }
                    case FIND_ALL_CATEGORIAS, SEARCH_CATEGORIA -> List.of(ConvertidorDto.categoria(CAT));
                    case SEARCH_FUNCIONARIO -> List.of(ConvertidorDto.funcionario(F1));
                    case SEARCH_RECURSOS -> List.of(ConvertidorDto.recurso(REC));
                    case FIND_RESERVAS -> List.of(ConvertidorDto.reserva(
                            "f1".equals(((Funcionario) args[0]).getId()) ? PROPIA : AJENA));
                    case FIND_RESERVAS_ACTIVAS_POR_FECHA, FIND_RESERVAS_ACTIVAS_POR_SEMANA ->
                            List.of(ConvertidorDto.reserva(PROPIA), ConvertidorDto.reserva(AJENA));
                    case CREAR_RESERVA -> ConvertidorDto.reserva(PROPIA);
                    case ESTADISTICAS_RECURSOS_POR_CATEGORIA -> Map.of("Equipo", 2L);
                    case ESTADISTICAS_ACTIVIDADES_POR_SEMANA -> Map.of(FECHA, 2L);
                    case EXTRAER_RESERVA -> new ReservaExtraccion("Clase", null, null, null, List.of("Equipo"));
                    default -> null;
                };
            } finally { activas.decrementAndGet(); }
        }
        static int codigo(Method m) {
            return switch (m.getName()) {
                case "login" -> LOGIN;
                case "changePassword" -> CHANGE_PASSWORD;
                case "findAllCategorias" -> FIND_ALL_CATEGORIAS;
                case "create", "update", "delete", "search" -> {
                    int base = m.getParameterTypes()[0] == Funcionario.class ? CREATE_FUNCIONARIO
                            : m.getParameterTypes()[0] == Recurso.class ? CREATE_RECURSO : CREATE_CATEGORIA;
                    if (m.getParameterCount() == 2) yield SEARCH_RECURSOS;
                    yield base + switch (m.getName()) { case "create" -> 0; case "update" -> 1; case "delete" -> 2; default -> 3; };
                }
                case "findReservas" -> FIND_RESERVAS;
                case "crearReserva" -> CREAR_RESERVA;
                case "cancelarReserva" -> CANCELAR_RESERVA;
                case "findReservasActivasPorFecha" -> FIND_RESERVAS_ACTIVAS_POR_FECHA;
                case "findReservasActivasPorSemana" -> FIND_RESERVAS_ACTIVAS_POR_SEMANA;
                case "estadisticasRecursosPorCategoria" -> ESTADISTICAS_RECURSOS_POR_CATEGORIA;
                case "estadisticasActividadesPorSemana" -> ESTADISTICAS_ACTIVIDADES_POR_SEMANA;
                case "extraerReserva" -> EXTRAER_RESERVA;
                default -> throw new AssertionError(m);
            };
        }
    }
}
