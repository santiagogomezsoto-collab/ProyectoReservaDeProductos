package reservas.presentation.estadisticas;

public class CategoriaCantidad {
    private String categoria;
    private long cantidad;

    public CategoriaCantidad(String categoria, long cantidad) {
        this.categoria = categoria;
        this.cantidad = cantidad;
    }

    public String getCategoria() {
        return categoria;
    }

    public long getCantidad() {
        return cantidad;
    }
}