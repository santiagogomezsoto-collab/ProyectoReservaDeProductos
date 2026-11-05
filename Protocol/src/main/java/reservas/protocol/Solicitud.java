package reservas.protocol;

import java.io.Serializable;

public record Solicitud(int operacion, Argumentos argumentos) implements Serializable {
    private static final long serialVersionUID = 1L;

    public Solicitud(int operacion) {
        this(operacion, null);
    }

    public Solicitud {
        Operaciones.validarArgumentos(operacion, argumentos);
    }
}
