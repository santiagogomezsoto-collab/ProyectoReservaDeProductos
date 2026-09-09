package reservas.presentation.categorias;

import reservas.logic.CategoriaRecurso;
import reservas.presentation.AbstractModel;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class Model extends AbstractModel {
    CategoriaRecurso current;
    List<CategoriaRecurso> list;

    public static final String CURRENT = "current";
    public static final String LIST = "list";

    public Model() {
        current = new CategoriaRecurso();
        list = new ArrayList<CategoriaRecurso>();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
        firePropertyChange(CURRENT);
        firePropertyChange(LIST);
    }

    public CategoriaRecurso getCurrent() {
        return current;
    }

    public void setCurrent(CategoriaRecurso current) {
        this.current = current;
        firePropertyChange(CURRENT);
    }

    public List<CategoriaRecurso> getList() {
        return list;
    }

    public void setList(List<CategoriaRecurso> list) {
        this.list = list;
        firePropertyChange(LIST);
    }
}
