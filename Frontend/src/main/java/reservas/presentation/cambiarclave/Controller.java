package reservas.presentation.cambiarclave;

import reservas.logic.IService;

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

    public void cambiarClave(String id, String claveActual, String claveNueva) throws Exception {
        service.changePassword(id, claveActual, claveNueva);
    }
}
