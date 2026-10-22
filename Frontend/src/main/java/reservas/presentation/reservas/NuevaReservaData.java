package reservas.presentation.reservas;

import reservas.logic.CategoriaRecurso;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class NuevaReservaData {
    private String actividad;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private List<CategoriaRecurso> categoriasSeleccionadas;

    public NuevaReservaData() {
        actividad = "";
        categoriasSeleccionadas = new ArrayList<>();
    }

    public String getActividad() {
        return actividad;
    }

    public void setActividad(String actividad) {
        this.actividad = actividad;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public List<CategoriaRecurso> getCategoriasSeleccionadas() {
        return categoriasSeleccionadas;
    }

    public void setCategoriasSeleccionadas(List<CategoriaRecurso> categoriasSeleccionadas) {
        this.categoriasSeleccionadas = categoriasSeleccionadas;
    }
}