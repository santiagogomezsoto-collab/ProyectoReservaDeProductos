package reservas.protocol;

import java.io.Serializable;

/** Datos de transporte sin credenciales; no modifica las entidades JAXB. */
public record UsuarioDto(String id, String rol, FuncionarioDto funcionario) implements Serializable {
    private static final long serialVersionUID = 1L;

    public UsuarioDto {
        if (funcionario != null && (!java.util.Objects.equals(id, funcionario.id())
                || !java.util.Objects.equals(rol, funcionario.rol()))) {
            throw new IllegalArgumentException("Ficha de funcionario inconsistente");
        }
    }
}
