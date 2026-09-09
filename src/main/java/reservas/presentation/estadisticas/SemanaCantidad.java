package reservas.presentation.estadisticas;

import java.time.LocalDate;

public class SemanaCantidad {
    private LocalDate semanaInicio;
    private long cantidad;

    public SemanaCantidad(LocalDate semanaInicio, long cantidad) {
        this.semanaInicio = semanaInicio;
        this.cantidad = cantidad;
    }

    public LocalDate getSemanaInicio() {
        return semanaInicio;
    }

    public long getCantidad() {
        return cantidad;
    }
}