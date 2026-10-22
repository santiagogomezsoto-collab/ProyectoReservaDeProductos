package reservas.presentation.actividades;

import reservas.logic.Reserva;
import reservas.presentation.AbstractModel;

import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Model extends AbstractModel {
    List<LocalTime> horas;
    List<LocalDate> dias;
    List<Reserva> reservas;

    public static final String MATRIZ = "matriz";

    public Model() {
        horas = new ArrayList<>();
        for (int h = 6; h <= 22; h++) {
            horas.add(LocalTime.of(h, 0));
            horas.add(LocalTime.of(h, 30));
        }
        dias = new ArrayList<>();
        reservas = new ArrayList<>();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
        firePropertyChange(MATRIZ);
    }

    public List<LocalTime> getHoras() {
        return horas;
    }

    public List<LocalDate> getDias() {
        return dias;
    }

    public List<Reserva> getReservas() {
        return reservas;
    }

    public void setMatriz(List<LocalDate> dias, List<Reserva> reservas) {
        this.dias = dias;
        this.reservas = reservas;
        firePropertyChange(MATRIZ);
    }
}
