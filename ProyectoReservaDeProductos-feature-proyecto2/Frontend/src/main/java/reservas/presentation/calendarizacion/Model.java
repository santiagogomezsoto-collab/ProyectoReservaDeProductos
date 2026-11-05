package reservas.presentation.calendarizacion;

import reservas.logic.CategoriaRecurso;
import reservas.logic.Recurso;
import reservas.logic.Reserva;
import reservas.presentation.AbstractModel;

import java.beans.PropertyChangeListener;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Model extends AbstractModel {
    List<CategoriaRecurso> categorias;
    List<LocalTime> horas;
    List<Recurso> recursos;
    List<Reserva> reservas;

    public static final String CATEGORIAS = "categorias";
    public static final String MATRIZ = "matriz";

    public Model() {
        categorias = new ArrayList<>();
        horas = new ArrayList<>();
        for (int h = 6; h <= 22; h++) {
            horas.add(LocalTime.of(h, 0));
            horas.add(LocalTime.of(h, 30));
        }
        recursos = new ArrayList<>();
        reservas = new ArrayList<>();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
        firePropertyChange(CATEGORIAS);
        firePropertyChange(MATRIZ);
    }

    public List<CategoriaRecurso> getCategorias() {
        return categorias;
    }

    public void setCategorias(List<CategoriaRecurso> categorias) {
        this.categorias = categorias;
        firePropertyChange(CATEGORIAS);
    }

    public List<LocalTime> getHoras() {
        return horas;
    }

    public List<Recurso> getRecursos() {
        return recursos;
    }

    public List<Reserva> getReservas() {
        return reservas;
    }

    public void setMatriz(List<Recurso> recursos, List<Reserva> reservas) {
        this.recursos = recursos;
        this.reservas = reservas;
        firePropertyChange(MATRIZ);
    }
}
