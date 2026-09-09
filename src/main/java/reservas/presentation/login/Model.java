package reservas.presentation.login;

import reservas.logic.Usuario;
import reservas.presentation.AbstractModel;

public class Model extends AbstractModel {
    Usuario current;

    public static final String CURRENT = "current";

    public Model() {
        current = new Usuario();
    }

    public Usuario getCurrent() {
        return current;
    }

    public void setCurrent(Usuario current) {
        this.current = current;
        firePropertyChange(CURRENT);
    }
}