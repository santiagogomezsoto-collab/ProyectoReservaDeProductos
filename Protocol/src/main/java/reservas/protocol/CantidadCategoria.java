package reservas.protocol;

import java.io.Serializable;

public record CantidadCategoria(String categoria, long cantidad) implements Serializable {
    private static final long serialVersionUID = 1L;
}
