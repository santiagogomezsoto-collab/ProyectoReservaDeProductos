package reservas.protocol;

import java.io.Serializable;

public record Solicitud(int operacion) implements Serializable {
    private static final long serialVersionUID = 1L;
}
