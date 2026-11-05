package reservas.server;

import org.junit.jupiter.api.*;
import reservas.client.*;
import reservas.logic.*;
import reservas.protocol.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

/** Proxy real -> TCP -> Worker real -> IService simulado, sin persistencia. */
class ServiceProxyTest {
    ServicioSimulado simulado;
    Servidor servidor;
    ExecutorService hilos;
    Future<?> aceptador;

    @BeforeEach void iniciar() throws Exception {
        simulado = new ServicioSimulado();
        servidor = new Servidor(simulado.service(), new Object(), InetAddress.getLoopbackAddress(), 0);
        hilos = Executors.newCachedThreadPool();
        aceptador = hilos.submit(() -> { servidor.servir(); return null; });
    }
    @AfterEach void cerrar() throws Exception {
        servidor.close();
        aceptador.get(3, TimeUnit.SECONDS);
        hilos.shutdownNow();
        esperar(() -> servidor.sesionesActivas() == 0);
    }
    ServiceProxy proxy() { return new ServiceProxy("127.0.0.1", servidor.puerto(), 3000); }
    static void esperar(BooleanSupplier condicion) throws Exception {
        long fin = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condicion.getAsBoolean() && System.nanoTime() < fin) Thread.sleep(10);
        assertTrue(condicion.getAsBoolean());
    }

    @Test void veintitresFirmasConversionesYSobrecargas() throws Exception {
        Set<Method> verificadas = new HashSet<>();
        try (ServiceProxy admin = proxy(); ServiceProxy f = proxy()) {
            assertEquals(Usuario.ADMIN, admin.login("admin", "solo-prueba").getRol());
            Usuario usuario = f.login("f1", "solo-prueba");
            assertInstanceOf(Funcionario.class, usuario);
            assertEquals("", usuario.getClave());
            for (Method metodo : IService.class.getDeclaredMethods()) {
                Method implementado = ServiceProxy.class.getDeclaredMethod(metodo.getName(), metodo.getParameterTypes());
                assertArrayEquals(metodo.getExceptionTypes(), implementado.getExceptionTypes());
                assertEquals(metodo.getReturnType(), implementado.getReturnType());
                if (metodo.getName().equals("login")) { verificadas.add(metodo); continue; }
                boolean deFuncionario = Set.of("findReservas", "crearReserva", "cancelarReserva", "extraerReserva").contains(metodo.getName());
                ServiceProxy destinatario = deFuncionario ? f : admin;
                Object[] args = argumentos(metodo);
                int antes = simulado.llamadas.size();
                Object resultado = metodo.invoke(destinatario, args);
                assertTrue(simulado.llamadas.size() > antes);
                assertEquals(metodo, simulado.llamadas.get(simulado.llamadas.size() - 1));
                comprobarArgumentos(metodo, args, simulado.ultimosArgumentos);
                comprobarResultado(metodo, resultado);
                verificadas.add(metodo);
            }
            assertEquals(23, verificadas.size());
            assertEquals(2, servidor.sesionesActivas());
        }
        esperar(() -> servidor.sesionesActivas() == 0);
    }

    Object[] argumentos(Method m) {
        return switch (m.getName()) {
            case "changePassword" -> new Object[]{"admin", "solo-prueba", "nueva-prueba"};
            case "create", "update", "delete" -> new Object[]{m.getParameterTypes()[0] == Funcionario.class
                    ? simulado.funcionario : m.getParameterTypes()[0] == Recurso.class ? simulado.recurso : simulado.categoria};
            case "search" -> m.getParameterCount() == 2 ? new Object[]{simulado.categoria, "proyector"}
                    : new Object[]{m.getParameterTypes()[0] == Funcionario.class ? simulado.funcionario : simulado.categoria};
            case "findAllCategorias" -> new Object[0];
            case "findReservas" -> new Object[]{simulado.funcionario};
            case "crearReserva" -> new Object[]{simulado.funcionario, "Nueva", simulado.fecha,
                    LocalTime.of(10, 0), LocalTime.of(11, 0), List.of(simulado.categoria)};
            case "cancelarReserva" -> new Object[]{simulado.reserva};
            case "findReservasActivasPorFecha" -> new Object[]{simulado.fecha};
            case "findReservasActivasPorSemana", "estadisticasRecursosPorCategoria", "estadisticasActividadesPorSemana" ->
                    new Object[]{simulado.fecha, simulado.fecha.plusDays(6)};
            case "extraerReserva" -> new Object[]{"reservar un equipo"};
            default -> throw new AssertionError(m);
        };
    }

    void comprobarArgumentos(Method metodo, Object[] originales, Object[] recibidos) {
        if (originales.length == 0) { assertTrue(recibidos == null || recibidos.length == 0); return; }
        assertEquals(originales.length, recibidos.length);
        for (int i = 0; i < originales.length; i++) {
            Object a = originales[i], b = recibidos[i];
            if (a instanceof Funcionario f) {
                assertEquals(f.getId(), ((Funcionario) b).getId());
                assertEquals("", ((Funcionario) b).getClave());
                assertEquals(f.getNombre(), ((Funcionario) b).getNombre());
            } else if (a instanceof CategoriaRecurso c) {
                assertEquals(c.getId(), ((CategoriaRecurso) b).getId());
                assertEquals(c.getDescripcion(), ((CategoriaRecurso) b).getDescripcion());
            } else if (a instanceof Recurso r) {
                assertEquals(r.getId(), ((Recurso) b).getId());
                assertEquals(r.getCategoria().getId(), ((Recurso) b).getCategoria().getId());
            } else if (a instanceof Reserva r) {
                assertEquals(r.getId(), ((Reserva) b).getId());
            } else if (a instanceof List<?>) {
                assertEquals("C1", ((CategoriaRecurso) ((List<?>) b).get(0)).getId());
            } else assertEquals(a, b, metodo + " parametro " + i);
        }
    }

    void comprobarResultado(Method m, Object resultado) {
        if (m.getReturnType() == void.class) { assertNull(resultado); return; }
        if (resultado instanceof List<?> lista) {
            assertEquals(1, lista.size());
            Object primero = lista.get(0);
            if (primero instanceof Reserva r) comprobarReserva(r);
            if (primero instanceof Funcionario f) { assertEquals("f1", f.getId()); assertEquals("", f.getClave()); }
            if (primero instanceof CategoriaRecurso c) assertEquals("C1", c.getId());
            if (primero instanceof Recurso r) assertEquals("C1", r.getCategoria().getId());
            lista.clear(); // Las pantallas reciben listas de dominio mutables.
        } else if (resultado instanceof Reserva r) comprobarReserva(r);
        else if (m.getName().equals("estadisticasRecursosPorCategoria")) assertEquals(Map.of("Equipo", 3L), resultado);
        else if (m.getName().equals("estadisticasActividadesPorSemana")) assertEquals(Map.of(simulado.fecha, 2L), resultado);
        else {
            reservas.ia.ReservaExtraccion e = (reservas.ia.ReservaExtraccion) resultado;
            assertEquals(List.of("Equipo"), e.getCategoriasRecurso());
            assertEquals("08:00", e.getHoraInicio());
        }
    }
    void comprobarReserva(Reserva r) {
        assertEquals("V1", r.getId());
        assertEquals(simulado.fecha, r.getFecha());
        assertEquals(LocalTime.of(8, 0), r.getHoraInicio());
        assertEquals("f1", r.getFuncionario().getId());
        assertEquals("", r.getFuncionario().getClave());
        assertEquals("C1", r.getRecursos().get(0).getCategoria().getId());
    }

    @Test void erroresNoSonListasVaciasYPersistenciaTrasRechazo() throws Exception {
        try (ServiceProxy p = proxy()) {
            String sesion = p.getSesionId();
            assertEquals(CodigoError.AUTENTICACION, assertThrows(ServiceProxyException.class,
                    () -> p.login("admin", "incorrecta")).getCodigo());
            assertFalse(p.isClosed());
            assertEquals(CodigoError.AUTENTICACION, assertThrows(ServiceProxyException.class,
                    () -> p.changePassword("admin", "incorrecta", "nueva-prueba")).getCodigo());
            assertEquals("C1", p.findAllCategorias().get(0).getId());
            assertEquals(CodigoError.AUTENTICACION, assertThrows(ServiceProxyException.class,
                    () -> p.search(simulado.funcionario)).getCodigo());
            p.login("f1", "solo-prueba");
            assertEquals(CodigoError.NO_AUTORIZADO, assertThrows(ServiceProxyException.class,
                    () -> p.create(simulado.categoria)).getCodigo());
            assertEquals(sesion, p.getSesionId());
            assertEquals(1, servidor.sesionesActivas());
            simulado.fallo = new IllegalStateException("detalle privado no publicar");
            ServiceProxyException ex = assertThrows(ServiceProxyException.class, p::findAllCategorias);
            assertEquals(CodigoError.INTERNO, ex.getCodigo());
            assertFalse(ex.getMessage().contains("privado"));
            assertFalse(ex.isConexionPerdida());
            simulado.fallo = null;
            assertEquals(1, p.findAllCategorias().size());
        }
    }

    @Test void solicitudesConcurrentesSobreUnSoloSocket() throws Exception {
        try (ServiceProxy p = proxy()) {
            p.login("admin", "solo-prueba");
            List<Future<?>> trabajos = new ArrayList<>();
            for (int i = 0; i < 20; i++) trabajos.add(hilos.submit(() -> {
                assertEquals("C1", p.findAllCategorias().get(0).getId());
                assertEquals(Map.of("Equipo", 3L), p.estadisticasRecursosPorCategoria(simulado.fecha, simulado.fecha));
            }));
            for (Future<?> t : trabajos) t.get(5, TimeUnit.SECONDS);
            assertEquals(1, servidor.sesionesActivas());
        }
    }

    @Test void esperaRemotaMantieneEventosDelEdt() throws Exception {
        simulado.demoraMillis = 300;
        try (ServiceProxy p = proxy()) {
            java.util.concurrent.atomic.AtomicInteger ticks = new java.util.concurrent.atomic.AtomicInteger();
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                javax.swing.Timer timer = new javax.swing.Timer(20, e -> ticks.incrementAndGet());
                timer.start();
                try { assertEquals(1, p.findAllCategorias().size()); }
                finally { timer.stop(); }
            });
            assertTrue(ticks.get() >= 3, "El EDT debe procesar eventos mientras espera la red");
        }
    }

    @Test void cerrarIdempotenteLiberaSesionEInvalidaToken() throws Exception {
        ServiceProxy p = proxy();
        String id = p.getSesionId();
        p.login("f1", "solo-prueba");
        p.close();
        p.close();
        esperar(() -> servidor.sesionesActivas() == 0);
        assertTrue(p.isClosed());
        assertTrue(assertThrows(ServiceProxyException.class, p::findAllCategorias).isConexionPerdida());
        try (Socket socket = new Socket("127.0.0.1", servidor.puerto()); CanalObjetos canal = new CanalObjetos(socket)) {
            socket.setSoTimeout(3000);
            canal.enviar(new Identificacion(TipoConexion.ASYNC, id));
            assertEquals(CodigoError.SESION_INVALIDA,
                    ((Respuesta) canal.recibir(FiltrosProtocolo.apertura())).codigoErrorControlado());
        }
    }

    @Test void desconexionNoReintentaNiReconecta() throws Exception {
        try (ServiceProxy p = proxy()) {
            p.findAllCategorias();
            servidor.close();
            assertTrue(assertThrows(ServiceProxyException.class, p::findAllCategorias).isConexionPerdida());
            assertTrue(p.isClosed());
            assertThrows(ServiceProxyException.class, p::findAllCategorias);
            assertEquals(1, simulado.llamadas.size());
        }
    }

    @Test void timeoutYCierreDesbloqueanLaEspera() throws Exception {
        simulado.demoraMillis = 1000;
        try (ServiceProxy p = new ServiceProxy("127.0.0.1", servidor.puerto(), 100)) {
            assertThrows(ServiceProxyException.class, p::findAllCategorias);
            assertTrue(p.isClosed());
        }
        try (ServiceProxy p = proxy()) {
            Future<?> llamada = hilos.submit(p::findAllCategorias);
            assertTrue(simulado.entrada.await(1, TimeUnit.SECONDS));
            p.close();
            assertThrows(ExecutionException.class, () -> llamada.get(2, TimeUnit.SECONDS));
        }
    }

    @Test void respuestaDeTipoIncorrectoYFiltroCierranProxy() throws Exception {
        for (Object respuesta : List.of(Respuesta.exito(Operaciones.CREAR_RESERVA,
                new Resultado.Reserva(ConvertidorDto.reserva(simulado.reserva))), new File("no-permitido"))) {
            try (ServerSocket falso = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
                Future<?> tarea = hilos.submit(() -> {
                    try (Socket s = falso.accept(); CanalObjetos canal = new CanalObjetos(s)) {
                        canal.recibir(FiltrosProtocolo.identificacion());
                        canal.enviar(new SesionCreada("sesion-test"));
                        canal.recibir(FiltrosProtocolo.solicitudes());
                        try { canal.enviar(respuesta); } catch (IOException ignored) { }
                    } catch (IOException | ClassNotFoundException e) { /* El rechazo puede cerrar durante el envio. */ }
                });
                try (ServiceProxy p = new ServiceProxy("127.0.0.1", falso.getLocalPort(), 1000)) {
                    assertTrue(assertThrows(ServiceProxyException.class, p::findAllCategorias).isConexionPerdida());
                    assertTrue(p.isClosed());
                }
                tarea.get(2, TimeUnit.SECONDS);
            }
        }
    }
}
