package reservas.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {

    @BeforeEach
    void setUp() {
        Service.resetForTesting();
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
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala VIP");
        Service.instance().create(cat);

        Funcionario func = new Funcionario("f1", "f1", "Pedro Gomez", "0000");
        Service.instance().create(func);

        Exception ex = assertThrows(Exception.class, () ->
                Service.instance().crearReserva(func, "Reunion", LocalDate.of(2026, 9, 10),
                        LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat)));

        assertTrue(ex.getMessage().contains("Sala VIP"));
    }

    @Test
    void crearReservaConRecursoDisponibleTieneExito() throws Exception {
        CategoriaRecurso cat = new CategoriaRecurso("", "Laptop");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("LAP-1", cat, "Laptop #1"));

        Funcionario func = new Funcionario("f2", "f2", "Ana Vargas", "1111");
        Service.instance().create(func);

        Reserva reserva = Service.instance().crearReserva(func, "Capacitacion",
                LocalDate.of(2026, 9, 10), LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));

        assertEquals(1, reserva.getRecursos().size());
        assertEquals("LAP-1", reserva.getRecursos().get(0).getId());
        assertEquals(Reserva.ACTIVA, reserva.getEstado());
    }

    @Test
    void cancelarReservaPasadaLanzaExcepcion() throws Exception {
        CategoriaRecurso cat = new CategoriaRecurso("", "Proyector");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("PROY-1", cat, "Proyector #1"));

        Funcionario func = new Funcionario("f3", "f3", "Luis Mora", "2222");
        Service.instance().create(func);

        Reserva reserva = Service.instance().crearReserva(func, "Charla",
                LocalDate.of(2020, 1, 1), LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));

        Exception ex = assertThrows(Exception.class, () ->
                Service.instance().cancelarReserva(reserva));

        assertEquals("No se puede cancelar una reserva pasada", ex.getMessage());
    }
}