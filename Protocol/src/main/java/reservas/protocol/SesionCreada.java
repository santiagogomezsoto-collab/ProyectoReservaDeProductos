package reservas.protocol;

import java.io.Serializable;

/** Acuse de sesion tecnica SYNC o de asociacion ASYNC; no concede autenticacion. */
public record SesionCreada(String sesionId) implements Serializable {
    private static final long serialVersionUID = 1L;
    public SesionCreada {
        if (sesionId == null || sesionId.isBlank()) throw new IllegalArgumentException("Sesion requerida");
    }
    @Override public String toString() { return "SesionCreada[identificador omitido]"; }
}
