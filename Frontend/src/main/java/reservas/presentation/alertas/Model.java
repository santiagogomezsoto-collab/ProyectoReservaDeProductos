package reservas.presentation.alertas;

import reservas.presentation.AbstractModel;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class Model extends AbstractModel {
    public static final String LIST = "list";
    List<Alerta> list = new ArrayList<>();

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
        firePropertyChange(LIST);
    }

    public List<Alerta> getList() { return list; }

    public void setList(List<Alerta> list) {
        this.list = list;
        firePropertyChange(LIST);
    }
}
