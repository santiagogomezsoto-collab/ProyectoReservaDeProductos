package reservas.protocol;

import java.io.Serializable;

/** Datos de transporte sin credenciales; no modifica las entidades JAXB. */
public record ReservaDto(String id, FuncionarioDto funcionario, String actividad, java.time.LocalDate fecha, java.time.LocalTime horaInicio, java.time.LocalTime horaFin, RecursoDto[] recursos, String estado) implements Serializable {
    private static final long serialVersionUID = 1L;

    public ReservaDto {
        recursos = recursos == null ? null : recursos.clone();
    }

    @Override public RecursoDto[] recursos() {
        return recursos == null ? null : recursos.clone();
    }
}
