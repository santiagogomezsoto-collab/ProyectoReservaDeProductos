package reservas.protocol;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class NotificacionTest {
    @Test void eventoMinimoConFiltroYCopiasDefensivas() throws Exception {
        String[] recursos = {"Proyector (Equipo)"};
        ReservaCreada evento = new ReservaCreada("V1", "Ana", "Clase", LocalDate.of(2030, 1, 7),
                LocalTime.of(8, 0), LocalTime.of(9, 0), recursos);
        recursos[0] = "cambiado";
        evento.recursos()[0] = "cambiado";
        ReservaCreada copia = (ReservaCreada) ProtocoloTest.vuelta(evento, FiltrosProtocolo.notificaciones());
        assertEquals(evento.id(), copia.id());
        assertEquals(evento.encargado(), copia.encargado());
        assertEquals(evento.actividad(), copia.actividad());
        assertEquals(evento.fecha(), copia.fecha());
        assertEquals(evento.desde(), copia.desde());
        assertEquals(evento.hasta(), copia.hasta());
        assertArrayEquals(new String[]{"Proyector (Equipo)"}, copia.recursos());
        for (var campo : ReservaCreada.class.getRecordComponents()) {
            assertFalse(campo.getName().toLowerCase().contains("clave"));
            assertFalse(campo.getName().toLowerCase().contains("telefono"));
        }
    }

    @Test void canalesNoAdmitenMensajesDelOtroCanal() throws Exception {
        ReservaCreada evento = new ReservaCreada("V1", "Ana", "Clase", LocalDate.now(),
                LocalTime.NOON, LocalTime.NOON, new String[0]);
        assertThrows(InvalidClassException.class, () -> ProtocoloTest.vuelta(evento, FiltrosProtocolo.respuestas()));
        assertThrows(InvalidClassException.class, () -> ProtocoloTest.vuelta(new Solicitud(1), FiltrosProtocolo.notificaciones()));
        assertThrows(InvalidClassException.class, () -> ProtocoloTest.vuelta(
                Respuesta.fallo(CodigoError.INTERNO, "error"), FiltrosProtocolo.notificaciones()));
        assertThrows(InvalidClassException.class, () -> ProtocoloTest.vuelta(new File("ajeno"), FiltrosProtocolo.notificaciones()));
    }
}
