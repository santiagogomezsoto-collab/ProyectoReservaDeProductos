package reservas.protocol;

import java.io.Serializable;

public record CategoriaDto(String id, String descripcion) implements Serializable {
    private static final long serialVersionUID = 1L;
}
