package reservas.presentation.cambiarclave;

import reservas.logic.Service;

public class Controller {
    View view;
    Model model;

    public Controller(View view, Model model) {
        this.view = view;
        this.model = model;
        view.setController(this);
        view.setModel(model);
    }

    public void cambiarClave(String id, String claveActual, String claveNueva) throws Exception {
        Service.instance().changePassword(id, claveActual, claveNueva);
    }
}