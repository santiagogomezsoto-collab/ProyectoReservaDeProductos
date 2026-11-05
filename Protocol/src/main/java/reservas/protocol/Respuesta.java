package reservas.protocol;

import java.io.Serializable;

/** Un resultado o un error. Los campos originales conservan el intercambio de categorias. */
public record Respuesta(CategoriaDto[] categorias, String error, Resultado resultado,
                        CodigoError codigoError) implements Serializable {
    private static final long serialVersionUID = 1L;

    public Respuesta {
        int presentes = (categorias == null ? 0 : 1) + (error == null ? 0 : 1)
                + (resultado == null ? 0 : 1);
        if (presentes != 1 || (codigoError != null && error == null)) {
            throw new IllegalArgumentException("La respuesta requiere exactamente un resultado o error");
        }
        if (categorias != null) categorias = categorias.clone();
    }

    public Respuesta(CategoriaDto[] categorias, String error) {
        this(categorias, error, null, null);
    }

    public static Respuesta exito(int operacion, Resultado resultado) {
        Operaciones.validarResultado(operacion, resultado);
        if (resultado instanceof Resultado.Categorias c) return new Respuesta(c.categorias(), null);
        return new Respuesta(null, null, resultado, null);
    }

    public static Respuesta fallo(CodigoError codigo, String mensaje) {
        java.util.Objects.requireNonNull(codigo);
        if (mensaje == null || mensaje.isBlank()) throw new IllegalArgumentException("Mensaje requerido");
        return new Respuesta(null, mensaje, null, codigo);
    }

    public Resultado resultadoTipado() {
        return categorias == null ? resultado : new Resultado.Categorias(categorias);
    }

    public CodigoError codigoErrorControlado() {
        return error == null ? null : codigoError == null ? CodigoError.INTERNO : codigoError;
    }

    @Override
    public CategoriaDto[] categorias() {
        return categorias == null ? null : categorias.clone();
    }
}
