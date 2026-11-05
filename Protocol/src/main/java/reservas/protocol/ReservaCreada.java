package reservas.protocol;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Objects;

/** Evento ASYNC: solo los datos necesarios para el detalle de una alerta. */
public record ReservaCreada(String id, String encargado, String actividad, LocalDate fecha,
                            LocalTime desde, LocalTime hasta, String[] recursos) implements Serializable {
    private static final long serialVersionUID = 1L;

    public ReservaCreada {
        Objects.requireNonNull(id);
        Objects.requireNonNull(encargado);
        Objects.requireNonNull(actividad);
        Objects.requireNonNull(fecha);
        Objects.requireNonNull(desde);
        Objects.requireNonNull(hasta);
        recursos = recursos.clone();
        for (String recurso : recursos) Objects.requireNonNull(recurso);
    }
    @Override public String[] recursos() { return recursos.clone(); }

    public static ReservaCreada de(ReservaDto reserva) {
        return new ReservaCreada(reserva.id(), reserva.funcionario().nombre(), reserva.actividad(),
                reserva.fecha(), reserva.horaInicio(), reserva.horaFin(),
                Arrays.stream(reserva.recursos()).map(r -> r.descripcion() + " (" + r.categoria().descripcion() + ")")
                        .toArray(String[]::new));
    }
}
