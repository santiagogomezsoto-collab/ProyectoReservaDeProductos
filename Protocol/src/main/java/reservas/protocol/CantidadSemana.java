package reservas.protocol;

import java.io.Serializable;
import java.time.LocalDate;

public record CantidadSemana(LocalDate semana, long cantidad) implements Serializable {
    private static final long serialVersionUID = 1L;
}
