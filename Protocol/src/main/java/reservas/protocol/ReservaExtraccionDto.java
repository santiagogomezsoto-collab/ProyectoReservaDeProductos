package reservas.protocol;

import java.io.Serializable;

/** Datos de transporte sin credenciales; no modifica las entidades JAXB. */
public record ReservaExtraccionDto(String actividad, String fecha, String horaInicio, String horaFinal, String[] categoriasRecurso) implements Serializable {
    private static final long serialVersionUID = 1L;

    public ReservaExtraccionDto {
        categoriasRecurso = categoriasRecurso == null ? null : categoriasRecurso.clone();
    }

    @Override public String[] categoriasRecurso() {
        return categoriasRecurso == null ? null : categoriasRecurso.clone();
    }
}
