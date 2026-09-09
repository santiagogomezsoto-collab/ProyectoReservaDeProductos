package reservas.presentation.funcionarios;

import reservas.logic.Funcionario;
import reservas.presentation.AbstractModel;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class Model extends AbstractModel {
    Funcionario current;
    List<Funcionario> list;

    public static final String CURRENT = "current";
    public static final String LIST = "list";

    public Model() {
        current = new Funcionario();
        list = new ArrayList<Funcionario>();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
        firePropertyChange(CURRENT);
        firePropertyChange(LIST);
    }

    public Funcionario getCurrent() {
        return current;
    }

    public void setCurrent(Funcionario current) {
        this.current = current;
        firePropertyChange(CURRENT);
    }

    public List<Funcionario> getList() {
        return list;
    }

    public void setList(List<Funcionario> list) {
        this.list = list;
        firePropertyChange(LIST);
    }
}