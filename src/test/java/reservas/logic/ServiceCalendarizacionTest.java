package reservas.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServiceCalendarizacionTest {

    @BeforeEach
    void setUp() {
        Service.resetForTesting();
    }

    @Test
    void findReservasActivasPorFechaSoloDevuelveLasDeEsaFecha() throws Exception {
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("R-1", cat, "Sala A"));
        Service.instance().create(new Recurso("R-2", cat, "Sala B"));

        Funcionario f = new Funcionario("f1", "f1", "Pedro Gomez", "0000");
        Service.instance().create(f);

        Service.instance().crearReserva(f, "Reunion 1", LocalDate.of(2026, 9, 10),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));
        Service.instance().crearReserva(f, "Reunion 2", LocalDate.of(2026, 9, 11),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));

        List<Reserva> resultado = Service.instance().findReservasActivasPorFecha(LocalDate.of(2026, 9, 10));
        assertEquals(1, resultado.size());
        assertEquals("Reunion 1", resultado.get(0).getActividad());
    }

    @Test
    void findReservasActivasPorSemanaIncluyeSoloElRangoDado() throws Exception {
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("R-1", cat, "Sala A"));
        Service.instance().create(new Recurso("R-2", cat, "Sala B"));

        Funcionario f = new Funcionario("f1", "f1", "Pedro Gomez", "0000");
        Service.instance().create(f);

        Service.instance().crearReserva(f, "Dentro del rango", LocalDate.of(2026, 9, 14),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));
        Service.instance().crearReserva(f, "Fuera del rango", LocalDate.of(2026, 9, 25),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));

        List<Reserva> resultado = Service.instance().findReservasActivasPorSemana(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20));

        assertEquals(1, resultado.size());
        assertEquals("Dentro del rango", resultado.get(0).getActividad());
    }

    @Test
    void reservaCanceladaNoApareceEnBusquedaPorFecha() throws Exception {
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("R-1", cat, "Sala A"));

        Funcionario f = new Funcionario("f1", "f1", "Pedro Gomez", "0000");
        Service.instance().create(f);

        Reserva reserva = Service.instance().crearReserva(f, "Reunion", LocalDate.now().plusDays(5),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));
        Service.instance().cancelarReserva(reserva);

        List<Reserva> resultado = Service.instance().findReservasActivasPorFecha(LocalDate.now().plusDays(5));
        assertTrue(resultado.isEmpty());
    }

    @Test
    void estadisticasRecursosPorCategoriaCuentaCorrectamente() throws Exception {
        CategoriaRecurso catSala = new CategoriaRecurso("", "Sala");
        CategoriaRecurso catLaptop = new CategoriaRecurso("", "Laptop");
        Service.instance().create(catSala);
        Service.instance().create(catLaptop);
        Service.instance().create(new Recurso("R-1", catSala, "Sala A"));
        Service.instance().create(new Recurso("R-2", catLaptop, "Laptop A"));

        Funcionario f = new Funcionario("f1", "f1", "Pedro Gomez", "0000");
        Service.instance().create(f);

        Service.instance().crearReserva(f, "Reunion 1", LocalDate.of(2026, 9, 10),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(catSala));
        Service.instance().crearReserva(f, "Reunion 2", LocalDate.of(2026, 9, 11),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(catLaptop));

        Map<String, Long> resultado = Service.instance().estadisticasRecursosPorCategoria(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertEquals(1L, resultado.get("Sala"));
        assertEquals(1L, resultado.get("Laptop"));
    }

    @Test
    void estadisticasActividadesPorSemanaAgrupaPorLunes() throws Exception {
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("R-1", cat, "Sala A"));
        Service.instance().create(new Recurso("R-2", cat, "Sala B"));

        Funcionario f = new Funcionario("f1", "f1", "Pedro Gomez", "0000");
        Service.instance().create(f);

        // Ambas fechas caen en la misma semana (lunes 2026-09-07)
        Service.instance().crearReserva(f, "Reunion 1", LocalDate.of(2026, 9, 8),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));
        Service.instance().crearReserva(f, "Reunion 2", LocalDate.of(2026, 9, 10),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));

        Map<LocalDate, Long> resultado = Service.instance().estadisticasActividadesPorSemana(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertEquals(2L, resultado.get(LocalDate.of(2026, 9, 7)));
    }
}