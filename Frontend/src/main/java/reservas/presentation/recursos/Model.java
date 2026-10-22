package reservas.presentation.recursos;

import reservas.logic.CategoriaRecurso;
import reservas.logic.Recurso;
import reservas.presentation.AbstractModel;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class Model extends AbstractModel {
    Recurso current;
    List<Recurso> list;
    List<CategoriaRecurso> categorias;

    public static final String CURRENT = "current";
    public static final String LIST = "list";
    public static final String CATEGORIAS = "categorias";

    public Model() {
        current = new Recurso();
        list = new ArrayList<Recurso>();
        categorias = new ArrayList<CategoriaRecurso>();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
        firePropertyChange(CURRENT);
        firePropertyChange(LIST);
        firePropertyChange(CATEGORIAS);
    }

    public Recurso getCurrent() {
        return current;
    }

    public void setCurrent(Recurso current) {
        this.current = current;
        firePropertyChange(CURRENT);
    }

    public List<Recurso> getList() {
        return list;
    }

    public void setList(List<Recurso> list) {
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