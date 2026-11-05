package reservas.server;

import org.junit.jupiter.api.*;
import reservas.client.*;
import reservas.logic.*;
import reservas.protocol.*;
import java.lang.reflect.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** TCP y clases de produccion reales; solo IService se simula, sin JDBC. */
class AlertasAsyncTest {
    Servidor servidor;
    ExecutorService hilos;
    final List<ServiceProxy> proxies = new ArrayList<>();
    final AtomicInteger creadas = new AtomicInteger();
    final AtomicInteger cancelaciones = new AtomicInteger();
    volatile boolean rechazar;
    volatile CountDownLatch dentro, continuar;
    final CategoriaRecurso categoria = new CategoriaRecurso("C1", "Equipo");

    @BeforeEach void iniciar() throws Exception {
        ServicioSimulado base = new ServicioSimulado();
        IService servicio = (IService) java.lang.reflect.Proxy.newProxyInstance(IService.class.getClassLoader(),
                new Class<?>[]{IService.class}, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("login") && List.of("A", "B", "C").contains(args[0]))
                        return new Funcionario((String) args[0], "NO_TRANSMITIR", "Persona " + args[0], "NO_TRANSMITIR");
                    if (metodo.getName().equals("crearReserva")) {
                        if (rechazar) throw new Exception("Rechazo simulado");
                        if (dentro != null) { dentro.countDown(); continuar.await(3, TimeUnit.SECONDS); }
                        Reserva reserva = new Reserva("V" + creadas.incrementAndGet(), (Funcionario) args[0],
                                (String) args[1], (LocalDate) args[2], (LocalTime) args[3], (LocalTime) args[4]);
                        reserva.getRecursos().add(new Recurso("R1", categoria, "Proyector"));
                        return reserva;
                    }
                    if (metodo.getName().equals("cancelarReserva")) cancelaciones.incrementAndGet();
                    return base.invoke(proxy, metodo, args);
                });
        servidor = new Servidor(servicio, new Object(), InetAddress.getLoopbackAddress(), 0);
        hilos = Executors.newCachedThreadPool();
        hilos.submit(() -> { servidor.servir(); return null; });
    }
    @AfterEach void cerrar() throws Exception {
        if (continuar != null) continuar.countDown();
        proxies.forEach(ServiceProxy::close);
        servidor.close();
        hilos.shutdownNow();
        assertTrue(hilos.awaitTermination(3, TimeUnit.SECONDS));
        ServiceProxyTest.esperar(() -> servidor.sesionesActivas() == 0 && servidor.conexionesAsync() == 0);
    }
    ServiceProxy cliente(String id) throws Exception {
        ServiceProxy p = new ServiceProxy("127.0.0.1", servidor.puerto());
        proxies.add(p);
        if (id != null) p.login(id, "solo-prueba");
        return p;
    }
    SocketListener escuchar(ServiceProxy p, BlockingQueue<ReservaCreada> eventos) throws Exception {
        SocketListener listener = p.iniciarAlertas(eventos::add, mensaje -> { });
        ServiceProxyTest.esperar(listener::isConectado);
        return listener;
    }
    Reserva crear(ServiceProxy p, String id) throws Exception {
        return p.crearReserva(new Funcionario(id, "", "", ""), "Actividad", LocalDate.of(2030, 1, 7),
                LocalTime.of(8, 0), LocalTime.of(9, 0), List.of(categoria));
    }
    ReservaCreada recibir(BlockingQueue<ReservaCreada> eventos) throws Exception {
        ReservaCreada evento = eventos.poll(3, TimeUnit.SECONDS);
        assertNotNull(evento, "Falta notificacion ASYNC");
        return evento;
    }

    @Test void tresFuncionariosRecibenUnaVezYSyncSigueFuncionando() throws Exception {
        ServiceProxy a = cliente("A"), b = cliente("B"), c = cliente("C"), admin = cliente("admin"), otroA = cliente("A");
        var ea = new LinkedBlockingQueue<ReservaCreada>();
        var eb = new LinkedBlockingQueue<ReservaCreada>();
        var ec = new LinkedBlockingQueue<ReservaCreada>();
        var eadmin = new LinkedBlockingQueue<ReservaCreada>();
        var eotroA = new LinkedBlockingQueue<ReservaCreada>();
        escuchar(a, ea); escuchar(b, eb); escuchar(c, ec); escuchar(admin, eadmin); escuchar(otroA, eotroA);
        Future<?> consultas = hilos.submit(() -> {
            for (int i = 0; i < 30; i++) {
                assertEquals("C1", b.findAllCategorias().get(0).getId());
                assertEquals("C1", c.findAllCategorias().get(0).getId());
            }
        });
        Set<String> esperadas = new HashSet<>();
        for (int i = 0; i < 10; i++) esperadas.add(crear(a, "A").getId());
        Set<String> recibidasB = new HashSet<>(), recibidasC = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            ReservaCreada evento = recibir(eb);
            assertTrue(recibidasB.add(evento.id()), "Duplicado B");
            assertEquals("Persona A", evento.encargado());
            assertEquals("Actividad", evento.actividad());
            assertArrayEquals(new String[]{"Proyector (Equipo)"}, evento.recursos());
            assertTrue(recibidasC.add(recibir(ec).id()), "Duplicado C");
        }
        consultas.get(5, TimeUnit.SECONDS);
        assertEquals(esperadas, recibidasB); assertEquals(esperadas, recibidasC);
        assertNull(eb.poll(150, TimeUnit.MILLISECONDS)); assertTrue(ec.isEmpty());
        assertTrue(ea.isEmpty()); assertTrue(eotroA.isEmpty()); assertTrue(eadmin.isEmpty());
    }

    @Test void soloNotificaCreacionConfirmadaYAutorizada() throws Exception {
        ServiceProxy a = cliente("A"), b = cliente("B");
        var eventos = new LinkedBlockingQueue<ReservaCreada>();
        escuchar(b, eventos);
        assertEquals(CodigoError.NO_AUTORIZADO,
                assertThrows(ServiceProxyException.class, () -> crear(a, "B")).getCodigo());
        rechazar = true;
        assertEquals(CodigoError.NEGOCIO,
                assertThrows(ServiceProxyException.class, () -> crear(a, "A")).getCodigo());
        assertEquals(0, creadas.get());
        assertNull(eventos.poll(150, TimeUnit.MILLISECONDS));
        rechazar = false; dentro = new CountDownLatch(1); continuar = new CountDownLatch(1);
        Future<Reserva> solicitud = hilos.submit(() -> crear(a, "A"));
        assertTrue(dentro.await(2, TimeUnit.SECONDS));
        assertNull(eventos.poll(150, TimeUnit.MILLISECONDS)); // Todavia no confirmo IService.
        continuar.countDown();
        assertEquals(solicitud.get(3, TimeUnit.SECONDS).getId(), recibir(eventos).id());
        assertNull(eventos.poll(150, TimeUnit.MILLISECONDS));
    }

    @Test void reconexionAsyncSinRepeticionYCierreDeSesion() throws Exception {
        ServiceProxy a = cliente("A"), b = cliente("B");
        var eventos = new LinkedBlockingQueue<ReservaCreada>();
        SocketListener listener = escuchar(b, eventos);
        crear(a, "A"); recibir(eventos);
        b.detenerAlertas();
        ServiceProxyTest.esperar(() -> servidor.conexionesAsync() == 0);
        crear(a, "A"); // No hay cola historica de eventos para conexiones ausentes.
        listener = escuchar(b, eventos);
        assertNull(eventos.poll(150, TimeUnit.MILLISECONDS));
        crear(a, "A"); recibir(eventos);
        // Simula perdida de red; el receptor debe abrir otro ASYNC usando el mismo sid.
        Field campo = SocketListener.class.getDeclaredField("socket"); campo.setAccessible(true);
        Socket original = (Socket) campo.get(listener);
        original.close();
        SocketListener actual = listener;
        ServiceProxyTest.esperar(() -> {
            try { return campo.get(actual) != original && actual.isConectado(); }
            catch (IllegalAccessException e) { throw new AssertionError(e); }
        });
        assertEquals(1, b.findAllCategorias().size());
        crear(a, "A"); recibir(eventos);
        assertNull(eventos.poll(150, TimeUnit.MILLISECONDS));
        String sid = b.getSesionId();
        b.close();
        ServiceProxyTest.esperar(() -> servidor.conexionesAsync() == 0);
        assertTrue(listener.isClosed());
        assertRechazado(sid);
    }

    @Test void sesionesInvalidasYAnonimasNoRecibenEventos() throws Exception {
        ServiceProxy anonimo = cliente(null);
        assertThrows(IllegalStateException.class, () -> anonimo.iniciarAlertas(e -> fail(), e -> fail()));
        assertRechazado(anonimo.getSesionId()); assertRechazado("sesion-inventada");
        var errores = new LinkedBlockingQueue<String>();
        try (SocketListener listener = new SocketListener("127.0.0.1", servidor.puerto(),
                anonimo.getSesionId(), e -> fail("Sesion anonima recibio evento"), errores::add)) {
            listener.iniciar();
            assertNotNull(errores.poll(3, TimeUnit.SECONDS));
            ServiceProxyTest.esperar(listener::isClosed);
            assertFalse(listener.isConectado());
        }
        assertEquals(0, servidor.conexionesAsync());
    }

    void assertRechazado(String sid) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", servidor.puerto()); CanalObjetos canal = new CanalObjetos(socket)) {
            socket.setSoTimeout(2000);
            canal.enviar(new Identificacion(TipoConexion.ASYNC, sid));
            assertEquals(CodigoError.SESION_INVALIDA,
                    ((Respuesta) canal.recibir(FiltrosProtocolo.apertura())).codigoErrorControlado());
        }
    }

    @Test void alertaEnEdtVerYDescartarSoloLocalConConfirmacion() throws Exception {
        ServiceProxy a = cliente("A"), b = cliente("B");
        var modelo = new reservas.presentation.alertas.Model();
        var vista = new reservas.presentation.alertas.View[1];
        var controlador = new reservas.presentation.alertas.Controller[1];
        var ventana = new javax.swing.JFrame[1];
        var recibida = new CountDownLatch(1);
        var fueraDeEdt = new java.util.concurrent.atomic.AtomicBoolean();
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            vista[0] = new reservas.presentation.alertas.View();
            controlador[0] = new reservas.presentation.alertas.Controller(vista[0], modelo);
            modelo.addPropertyChangeListener(e -> {
                if (!javax.swing.SwingUtilities.isEventDispatchThread()) fueraDeEdt.set(true);
                if (!modelo.getList().isEmpty()) recibida.countDown();
            });
            ventana[0] = new javax.swing.JFrame("Prueba A5");
            ventana[0].setContentPane(vista[0].getPanel());
            ventana[0].setSize(450, 350); ventana[0].setVisible(true);
        });
        try {
            SocketListener listener = b.iniciarAlertas(evento -> controlador[0].agregar(
                    new reservas.presentation.alertas.Alerta(evento.id(), evento.encargado(), evento.actividad(),
                            evento.fecha().toString(), evento.desde().toString(), evento.hasta().toString(),
                            List.of(evento.recursos()))), e -> { });
            ServiceProxyTest.esperar(listener::isConectado);
            crear(a, "A"); assertTrue(recibida.await(3, TimeUnit.SECONDS));
            Field campo = reservas.presentation.alertas.View.class.getDeclaredField("listado"); campo.setAccessible(true);
            var tabla = (javax.swing.JTable) campo.get(vista[0]);
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                assertFalse(fueraDeEdt.get());
                assertEquals(1, tabla.getRowCount());
                dialogo(tabla, 1, "Actividad", javax.swing.JOptionPane.OK_OPTION, texto -> {
                    for (String esperado : List.of("Persona A", "2030-01-07", "08:00", "09:00", "Proyector (Equipo)"))
                        assertTrue(texto.contains(esperado), texto);
                });
                assertEquals(1, modelo.getList().size());
                dialogo(tabla, 2, "Descartar?", javax.swing.JOptionPane.NO_OPTION, texto -> assertEquals("Actividad", texto));
                assertEquals(1, modelo.getList().size());
                dialogo(tabla, 2, "Descartar?", javax.swing.JOptionPane.YES_OPTION, texto -> { });
                assertTrue(modelo.getList().isEmpty()); assertEquals(0, tabla.getRowCount());
            });
            assertEquals(1, creadas.get()); assertEquals(0, cancelaciones.get());
        } finally { javax.swing.SwingUtilities.invokeAndWait(ventana[0]::dispose); }
    }

    static void dialogo(javax.swing.JTable tabla, int columna, String titulo, int opcion,
                        java.util.function.Consumer<String> comprobar) {
        var fallo = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        var encontrado = new java.util.concurrent.atomic.AtomicBoolean();
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        javax.swing.Timer timer = new javax.swing.Timer(30, null);
        timer.addActionListener(e -> {
            for (java.awt.Window w : java.awt.Window.getWindows()) {
                if (w.isShowing() && w instanceof javax.swing.JDialog d && titulo.equals(d.getTitle())) {
                    encontrado.set(true);
                    try {
                        javax.swing.JOptionPane pane = encontrarPane(d);
                        assertNotNull(pane);
                        comprobar.accept(String.valueOf(pane.getMessage()));
                        pane.setValue(opcion);
                    } catch (Throwable t) { fallo.set(t); d.dispose(); }
                    timer.stop(); return;
                }
            }
            if (System.nanoTime() > limite) {
                fallo.set(new AssertionError("No se encontro el dialogo " + titulo));
                for (java.awt.Window w : java.awt.Window.getWindows())
                    if (w instanceof javax.swing.JDialog) w.dispose();
                timer.stop();
            }
        });
        timer.start();
        try {
            java.awt.Rectangle celda = tabla.getCellRect(0, columna, true);
            tabla.dispatchEvent(new java.awt.event.MouseEvent(tabla, java.awt.event.MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, celda.x + 2, celda.y + 2, 1, false));
        } finally { timer.stop(); }
        assertTrue(encontrado.get());
        if (fallo.get() != null) throw new AssertionError(fallo.get());
    }
    static javax.swing.JOptionPane encontrarPane(java.awt.Container padre) {
        for (java.awt.Component hijo : padre.getComponents()) {
            if (hijo instanceof javax.swing.JOptionPane pane) return pane;
            if (hijo instanceof java.awt.Container contenedor) {
                var pane = encontrarPane(contenedor);
                if (pane != null) return pane;
            }
        }
        return null;
    }
}
