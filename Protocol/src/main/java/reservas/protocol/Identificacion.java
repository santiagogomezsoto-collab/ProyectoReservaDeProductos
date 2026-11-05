package reservas.protocol;

import java.io.Serializable;
import java.util.Objects;

/**
 * Propuesta de apertura: SYNC sin sesion inicia autenticacion; ASYNC requiere
 * el identificador emitido por Backend. La validacion de pertenencia es del servidor.
 */
public record Identificacion(TipoConexion tipo, String sesionId) implements Serializable {
    private static final long serialVersionUID = 1L;

    public Identificacion {
        Objects.requireNonNull(tipo);
        if (sesionId != null && sesionId.isBlank())
            throw new IllegalArgumentException("Sesion vacia");
        if (tipo == TipoConexion.ASYNC && sesionId == null)
            throw new IllegalArgumentException("ASYNC requiere sesion");
    }

    @Override public String toString() { return "Identificacion[tipo=" + tipo + "]"; }
}
