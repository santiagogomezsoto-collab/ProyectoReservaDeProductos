package reservas.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias (Surefire) de las reglas de reserva. No necesitan MySQL:
 * ReglasReserva no consulta datos, y la fecha y hora actuales se le pasan como
 * parametro, asi que las pruebas son deterministas y no caducan.
 */
class ReglasReservaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);
    private static final LocalTime AHORA = LocalTime.of(10, 0);

    private List<CategoriaRecurso> unaCategoria() {
        List<CategoriaRecurso> lista = new ArrayList<>();
        lista.add(new CategoriaRecurso("CAT-000001", "Laptop"));
        return lista;
    }

    // ---------- validarSolicitud ----------

    @Test
    @DisplayName("Una solicitud valida para mañana no lanza error")
    void solicitudValida() {
        assertDoesNotThrow(() -> ReglasReserva.validarSolicitud(unaCategoria(),
                HOY.plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0), HOY, AHORA));
    }

    @Test
    @DisplayName("Sin categorias se rechaza")
    void sinCategorias() {
        Exception e = assertThrows(Exception.class, () -> ReglasReserva.validarSolicitud(
                new ArrayList<>(), HOY.plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0), HOY, AHORA));
        assertEquals("Debe seleccionar al menos una categoria", e.getMessage());
    }

    @Test
    @DisplayName("La lista de categorias nula se rechaza igual que la vacia")
    void categoriasNulas() {
        assertThrows(Exception.class, () -> ReglasReserva.validarSolicitud(
                null, HOY.plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0), HOY, AHORA));
    }

    @Test
    @DisplayName("La hora de fin no puede ser anterior a la de inicio")
    void horaFinAnterior() {
        Exception e = assertThrows(Exception.class, () -> ReglasReserva.validarSolicitud(
                unaCategoria(), HOY.plusDays(1), LocalTime.of(10, 0), LocalTime.of(9, 0), HOY, AHORA));
        assertEquals("La hora de fin debe ser posterior a la hora de inicio", e.getMessage());
    }

    @Test
    @DisplayName("La hora de fin no puede ser igual a la de inicio")
    void horaFinIgual() {
        assertThrows(Exception.class, () -> ReglasReserva.validarSolicitud(
                unaCategoria(), HOY.plusDays(1), LocalTime.of(9, 0), LocalTime.of(9, 0), HOY, AHORA));
    }

    @Test
    @DisplayName("Una fecha pasada se rechaza")
    void fechaPasada() {
        Exception e = assertThrows(Exception.class, () -> ReglasReserva.validarSolicitud(
                unaCategoria(), HOY.minusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0), HOY, AHORA));
        assertEquals("No se puede crear una reserva en una fecha u hora pasada", e.getMessage());
    }

    @Test
    @DisplayName("Hoy a una hora ya pasada se rechaza")
    void hoyHoraPasada() {
        assertThrows(Exception.class, () -> ReglasReserva.validarSolicitud(
                unaCategoria(), HOY, LocalTime.of(9, 0), LocalTime.of(11, 0), HOY, AHORA));
    }

    @Test
    @DisplayName("Hoy a una hora futura se acepta")
    void hoyHoraFutura() {
        assertDoesNotThrow(() -> ReglasReserva.validarSolicitud(unaCategoria(),
                HOY, LocalTime.of(11, 0), LocalTime.of(12, 0), HOY, AHORA));
    }

    // ---------- esPasado ----------

    @Test
    @DisplayName("Hoy exactamente a la hora actual cuenta como pasado")
    void horaExacta() {
        assertTrue(ReglasReserva.esPasado(HOY, AHORA, HOY, AHORA));
    }

    @Test
    @DisplayName("Mañana nunca es pasado, ni a medianoche")
    void mananaNoEsPasado() {
        assertFalse(ReglasReserva.esPasado(HOY.plusDays(1), LocalTime.MIDNIGHT, HOY, AHORA));
    }

    // ---------- seSolapan ----------

    @Test
    @DisplayName("Intervalos que se cruzan parcialmente se solapan")
    void cruceParcial() {
        assertTrue(ReglasReserva.seSolapan(
                LocalTime.of(8, 0), LocalTime.of(10, 0),
                LocalTime.of(9, 0), LocalTime.of(11, 0)));
    }

    @Test
    @DisplayName("Un intervalo contenido en otro se solapa")
    void contenido() {
        assertTrue(ReglasReserva.seSolapan(
                LocalTime.of(9, 0), LocalTime.of(9, 30),
                LocalTime.of(8, 0), LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("Intervalos contiguos NO se solapan: 8-9 y 9-10 son compatibles")
    void contiguos() {
        assertFalse(ReglasReserva.seSolapan(
                LocalTime.of(8, 0), LocalTime.of(9, 0),
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("Intervalos separados no se solapan")
    void separados() {
        assertFalse(ReglasReserva.seSolapan(
                LocalTime.of(8, 0), LocalTime.of(9, 0),
                LocalTime.of(14, 0), LocalTime.of(15, 0)));
    }

    @Test
    @DisplayName("El solape no depende del orden de los argumentos")
    void simetria() {
        boolean ab = ReglasReserva.seSolapan(
                LocalTime.of(8, 0), LocalTime.of(10, 0),
                LocalTime.of(9, 0), LocalTime.of(11, 0));
        boolean ba = ReglasReserva.seSolapan(
                LocalTime.of(9, 0), LocalTime.of(11, 0),
                LocalTime.of(8, 0), LocalTime.of(10, 0));
        assertEquals(ab, ba);
    }
}
