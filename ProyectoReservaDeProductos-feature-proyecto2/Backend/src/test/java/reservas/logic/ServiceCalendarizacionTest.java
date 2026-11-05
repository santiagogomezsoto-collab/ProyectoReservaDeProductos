package reservas.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
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
        LocalDate hoy = LocalDate.now();
        LocalDate futura = hoy.plusDays(10);
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("R-1", cat, "Sala A"));
        Service.instance().create(new Recurso("R-2", cat, "Sala B"));

        Funcionario f = new Funcionario("f1", "f1", "Pedro Gomez", "0000");
        Service.instance().create(f);

        Service.instance().crearReserva(f, "Reunion 1", futura,
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));
        Service.instance().crearReserva(f, "Reunion 2", futura.plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));

        List<Reserva> resultado = Service.instance().findReservasActivasPorFecha(futura);
        assertEquals(1, resultado.size());
        assertEquals("Reunion 1", resultado.get(0).getActividad());
    }

    @Test
    void findReservasActivasPorSemanaIncluyeSoloElRangoDado() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.plusWeeks(2).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("R-1", cat, "Sala A"));
        Service.instance().create(new Recurso("R-2", cat, "Sala B"));

        Funcionario f = new Funcionario("f1", "f1", "Pedro Gomez", "0000");
        Service.instance().create(f);

        Service.instance().crearReserva(f, "Dentro del rango", lunes,
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));
        Service.instance().crearReserva(f, "Fuera del rango", lunes.plusDays(11),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));

        List<Reserva> resultado = Service.instance().findReservasActivasPorSemana(
                lunes, lunes.plusDays(6));

        assertEquals(1, resultado.size());
        assertEquals("Dentro del rango", resultado.get(0).getActividad());
    }

    @Test
    void reservaCanceladaNoApareceEnBusquedaPorFecha() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate futura = hoy.plusDays(10);
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("R-1", cat, "Sala A"));

        Funcionario f = new Funcionario("f1", "f1", "Pedro Gomez", "0000");
        Service.instance().create(f);

        Reserva reserva = Service.instance().crearReserva(f, "Reunion", futura,
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));
        Service.instance().cancelarReserva(reserva);

        List<Reserva> resultado = Service.instance().findReservasActivasPorFecha(futura);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void estadisticasRecursosPorCategoriaCuentaCorrectamente() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate futura = hoy.plusDays(10);
        CategoriaRecurso catSala = new CategoriaRecurso("", "Sala");
        CategoriaRecurso catLaptop = new CategoriaRecurso("", "Laptop");
        Service.instance().create(catSala);
        Service.instance().create(catLaptop);
        Service.instance().create(new Recurso("R-1", catSala, "Sala A"));
        Service.instance().create(new Recurso("R-2", catLaptop, "Laptop A"));

        Funcionario f = new Funcionario("f1", "f1", "Pedro Gomez", "0000");
        Service.instance().create(f);

        Service.instance().crearReserva(f, "Reunion 1", futura,
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(catSala));
        Service.instance().crearReserva(f, "Reunion 2", futura.plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(catLaptop));

        Map<String, Long> resultado = Service.instance().estadisticasRecursosPorCategoria(
                futura, futura.plusDays(1));

        assertEquals(1L, resultado.get("Sala"));
        assertEquals(1L, resultado.get("Laptop"));
    }

    @Test
    void estadisticasActividadesPorSemanaAgrupaPorLunes() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.plusWeeks(2).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        CategoriaRecurso cat = new CategoriaRecurso("", "Sala");
        Service.instance().create(cat);
        Service.instance().create(new Recurso("R-1", cat, "Sala A"));
        Service.instance().create(new Recurso("R-2", cat, "Sala B"));

        Funcionario f = new Funcionario("f1", "f1", "Pedro Gomez", "0000");
        Service.instance().create(f);

        // Ambas fechas caen en la misma semana futura
        Service.instance().crearReserva(f, "Reunion 1", lunes.plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));
        Service.instance().crearReserva(f, "Reunion 2", lunes.plusDays(3),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(cat));

        Map<LocalDate, Long> resultado = Service.instance().estadisticasActividadesPorSemana(
                lunes, lunes.plusDays(6));

        assertEquals(2L, resultado.get(lunes));
    }
}
