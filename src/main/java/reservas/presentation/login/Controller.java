package reservas.presentation.login;

import reservas.logic.IService;
import reservas.logic.Usuario;
import reservas.presentation.Sesion;

public class Controller {
    View view;
    Model model;
    private final IService service;

    public Controller(View view, Model model, IService service) {
        this.service = service;
        this.view = view;
        this.model = model;
        view.setController(this);
        view.setModel(model);
    }

    public void login(Usuario usuario) throws Exception {
        Usuario logged = service.login(usuario.getId(), usuario.getClave());
        Sesion.setUsuario(logged);
    }

    public void abrirCambioClave() {
        reservas.presentation.cambiarclave.View cambiarView = new reservas.presentation.cambiarclave.View();
        reservas.presentation.cambiarclave.Model cambiarModel = new reservas.presentation.cambiarclave.Model();
        new reservas.presentation.cambiarclave.Controller(cambiarView, cambiarModel, service);
        cambiarView.setLocationRelativeTo(view.getPanel());
        cambiarView.setVisible(true);
    }
}
