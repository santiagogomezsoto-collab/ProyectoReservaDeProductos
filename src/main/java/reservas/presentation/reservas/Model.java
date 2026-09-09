package reservas.presentation.reservas;

import reservas.logic.CategoriaRecurso;
import reservas.logic.Reserva;
import reservas.presentation.AbstractModel;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class Model extends AbstractModel {
    NuevaReservaData current;
    List<Reserva> list;
    List<CategoriaRecurso> categorias;

    public static final String CURRENT = "current";
    public static final String LIST = "list";
    public static final String CATEGORIAS = "categorias";

    public Model() {
        current = new NuevaReservaData();
        list = new ArrayList<Reserva>();
        categorias = new ArrayList<CategoriaRecurso>();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
        firePropertyChange(CURRENT);
        firePropertyChange(LIST);
        firePropertyChange(CATEGORIAS);
    }

    public NuevaReservaData getCurrent() {
        return current;
    }

    public void setCurrent(NuevaReservaData current) {
        this.current = current;
        firePropertyChange(CURRENT);
    }

    public List<Reserva> getList() {
        return list;
    }

    public void setList(List<Reserva> list) {
        this.list = list;
        firePropertyChange(LIST);
    }

    public List<CategoriaRecurso> getCategorias() {
        return categorias;
    }

    public void setCategorias(List<CategoriaRecurso> categorias) {
        this.categorias = categorias;
        firePropertyChange(CATEGORIAS);
    }
}