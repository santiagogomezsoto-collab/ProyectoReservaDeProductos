package reservas.protocol;

import java.io.Serializable;

/** Resultado de la consulta: categorias o un mensaje de error, nunca ambos. */
public record Respuesta(CategoriaDto[] categorias, String error) implements Serializable {
    private static final long serialVersionUID = 1L;

    public Respuesta {
        if ((categorias == null) == (error == null)) {
            throw new IllegalArgumentException("La respuesta requiere categorias o error");
        }
        if (categorias != null) categorias = categorias.clone();
    }

    @Override
    public CategoriaDto[] categorias() {
        return categorias == null ? null : categorias.clone();
    }
}
