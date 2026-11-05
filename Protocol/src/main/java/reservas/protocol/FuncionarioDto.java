package reservas.protocol;

import java.io.Serializable;

/** Datos de transporte sin credenciales; no modifica las entidades JAXB. */
public record FuncionarioDto(String id, String rol, String nombre, String telefono) implements Serializable {
    private static final long serialVersionUID = 1L;
}
