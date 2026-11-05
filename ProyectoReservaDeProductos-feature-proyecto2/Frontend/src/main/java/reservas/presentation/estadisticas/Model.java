package reservas.presentation.estadisticas;

import reservas.presentation.AbstractModel;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class Model extends AbstractModel {
    List<CategoriaCantidad> recursos;
    List<SemanaCantidad> actividades;

    public static final String RECURSOS = "recursos";
    public static final String ACTIVIDADES = "actividades";

    public Model() {
        recursos = new ArrayList<>();
        actividades = new ArrayList<>();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
        firePropertyChange(RECURSOS);
        firePropertyChange(ACTIVIDADES);
    }

    public List<CategoriaCantidad> getRecursos() {
        return recursos;
    }

    public void setRecursos(List<CategoriaCantidad> recursos) {
        this.recursos = recursos;
        firePropertyChange(RECURSOS);
    }

    public List<SemanaCantidad> getActividades() {
        return actividades;
    }

    public void setActividades(List<SemanaCantidad> actividades) {
        this.actividades = actividades;
        firePropertyChange(ACTIVIDADES);
    }
}