package reservas.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reservas.data.ReservaDao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceIT {

    @BeforeEach
    void setUp() throws Exception {
        PruebaBase.reiniciar();
    }

    @Test
    void loginConCredencialesCorrectasDevuelveElUsuario() throws Exception {
        Service.instance().create(new Funcionario("111", "x", "Juan Perez", "8888-0000"));
        Usuario u = Service.instance().login("111", "111");
        assertNotNull(u);
        assertEquals("111", u.getId());
    }

    @Test
    void loginConClaveIncorrectaLanzaExcepcion() throws Exception {
        Service.instance().create(new Funcionario("111", "x", "Juan Perez", "8888-0000"));
        Exception ex = assertThrows(Exception.class, () ->
                Service.instance().login("111", "clave-mala"));
        assertEquals("Clave incorrecta", ex.getMessage());
    }

    @Test
    void loginConUsuarioInexistenteLanzaExcepcion() {
        Exception ex = assertThrows(Exception.class, () ->
                Service.instance().login("no-existe", "x"));
        assertEquals("Usuario no existe", ex.getMessage());
    }

    @Test
    void crearFuncionarioAsignaClaveIgualAlId() throws Exception {
        Funcionario f = new Funcionario("222", "clave-cualquiera", "Maria Lopez", "7777-1111");
        Service.instance().create(f);
        Funcionario leido = Service.instance().read(f);
        assertEquals("222", leido.getClave());
    }

    @Test
    void crearFuncionarioConIdDuplicadoLanzaExcepcion() throws Exception {
        Service.instance().create(new Funcionario("333", "x", "Nombre 1", "1111"));
        Exception ex = assertThrows(Exception.class, () ->
                Service.instance().create(new Funcionario("333", "x", "Nombre 2", "2222")));
        assertEquals("Funcionario ya existe", ex.getMessage());
    }

    @Test
    void crearCategoriaAutogeneraIdConFormatoCorrecto() throws Exception {
        CategoriaRecurso c = new CategoriaRecurso("", "Sala de Juntas");
        Service.instance().create(c);
        assertEquals("CAT-000001", c.getId());
    }

    @Test
    void crearSegundaCategoriaIncrementaElConsecutivo() throws Exception {
        Service.instance().create(new CategoriaRecurso("", "Categoria 1"));
        CategoriaRecurso c2 = new CategoriaRecurso("", "Categoria 2");
        Service.instance().create(c2);
        assertEquals("CAT-000002", c2.getId());
    }

    @Test
    void buscarFuncionarioPorNombreParcialFuncionaSinDistinguirMayusculas() throws Exception {
        Service.instance().create(new Funcionario("1", "x", "Ana Torres", "1111"));
        Service.instance().create(new Funcionario("2", "x", "Carlos Ruiz", "2222"));

        Funcionario filtro = new Funcionario();
        filtro.setId("");
        filtro.setNombre("ana");

        List<Funcionario> resultado = Service.instance().search(filtro);

        assertEquals(1, resultado.size());
        assertEquals("Ana Torres", resultado.get(0).getNombre());
    }

    @Test
    void crearReservaSinRecursosDisponiblesLanzaExcepcion() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate futura = hoy.plusDays(10);
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala VIP");
        Service.instance().create(cat);

        Funcionario func = new Funcionario("f1", "f1", "Pedro Gomez", "0000");
        Service.instance().create(func);

        Exception ex = assertThrows(Exception.class, () ->
                Service.instance().crearReserva(func, "Reunion", futura,
                        LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat)));

        assertTrue(ex.getMessage().contains("Sala VIP"));
    }

    @Test
    void crearReservaConRecursoDisponibleTieneExito() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate futura = hoy.plusDays(10);
        CategoriaRecurso cat = new CategoriaRecurso("", "Laptop");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("LAP-1", cat, "Laptop #1"));

        Funcionario func = new Funcionario("f2", "f2", "Ana Vargas", "1111");
        Service.instance().create(func);

        Reserva reserva = Service.instance().crearReserva(func, "Capacitacion",
                futura, LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));

        assertEquals(1, reserva.getRecursos().size());
        assertEquals("LAP-1", reserva.getRecursos().get(0).getId());
        assertEquals(Reserva.ACTIVA, reserva.getEstado());
    }

    @Test
    void cancelarReservaPasadaLanzaExcepcion() throws Exception {
        LocalDate hoy = LocalDate.now();
        CategoriaRecurso cat = new CategoriaRecurso("", "Proyector");
        Service.instance().create(cat);
        Recurso recurso = new Recurso("PROY-1", cat, "Proyector #1");
        Service.instance().create(recurso);

        Funcionario func = new Funcionario("f3", "f3", "Luis Mora", "2222");
        Service.instance().create(func);

        // Service.crearReserva rechaza las fechas pasadas, y con razon. Para
        // probar la cancelacion de una reserva historica hay que insertarla
        // directamente por el DAO, saltandose esa regla a proposito.
        //
        // Antes esta prueba cambiaba la fecha del objeto en memoria. Con XML
        // funcionaba porque el Service devolvia esa misma instancia; con MySQL
        // cancelarReserva vuelve a leer la fila y no veria el cambio.
        Reserva pasada = new Reserva("RES-000001", func, "Charla del mes pasado",
                hoy.minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0));
        pasada.getRecursos().add(recurso);
        new ReservaDao().create(pasada);

        Exception ex = assertThrows(Exception.class, () ->
                Service.instance().cancelarReserva(pasada));

        assertEquals("No se puede cancelar una reserva pasada o que ya comenzó", ex.getMessage());

        // La fila siguio ACTIVA en la base: la cancelacion no se aplico.
        List<Reserva> delFuncionario = Service.instance().findReservas(func);
        assertEquals(1, delFuncionario.size());
        assertEquals(Reserva.ACTIVA, delFuncionario.get(0).getEstado());
    }

    @Test
    void crearReservaEnFechaPasadaLanzaExcepcion() throws Exception {
        LocalDate hoy = LocalDate.now();
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("SALA-1", cat, "Sala disponible"));
        Funcionario funcionario = new Funcionario("f4", "f4", "Ana", "1111");
        Service.instance().create(funcionario);

        Exception ex = assertThrows(Exception.class, () ->
                Service.instance().crearReserva(funcionario, "Reunion", hoy.minusDays(1),
                        LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat)));

        assertEquals("No se puede crear una reserva en una fecha u hora pasada", ex.getMessage());
        assertTrue(Service.instance().findReservas(funcionario).isEmpty());
    }
}
