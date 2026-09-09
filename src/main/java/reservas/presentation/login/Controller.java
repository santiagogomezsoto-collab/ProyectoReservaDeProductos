package reservas.presentation.login;

import reservas.logic.Service;
import reservas.logic.Usuario;
import reservas.presentation.Sesion;

public class Controller {
    View view;
    Model model;

    public Controller(View view, Model model) {
        this.view = view;
        this.model = model;
        view.setController(this);
        view.setModel(model);
    }

    public void login(Usuario usuario) throws Exception {
        Usuario logged = Service.instance().login(usuario.getId(), usuario.getClave());
        Sesion.setUsuario(logged);
    }
}