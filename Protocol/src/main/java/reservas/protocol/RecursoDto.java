package reservas.protocol;

import java.io.Serializable;

/** Datos de transporte sin credenciales; no modifica las entidades JAXB. */
public record RecursoDto(String id, CategoriaDto categoria, String descripcion) implements Serializable {
    private static final long serialVersionUID = 1L;
}
