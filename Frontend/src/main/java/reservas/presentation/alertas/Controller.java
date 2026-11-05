package reservas.presentation.alertas;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

public class Controller {
    View view;
    Model model;

    public Controller(View view, Model model) {
        this.view = view;
        this.model = model;
        view.setController(this);
        view.setModel(model);
    }

    // Se puede llamar desde cualquier hilo (por ejemplo, el receptor de red).
    public void agregar(Alerta a) {
        SwingUtilities.invokeLater(() -> {
            List<Alerta> nueva = new ArrayList<>(model.getList());
            nueva.add(a);
            model.setList(nueva);
        });
    }

    // Solo descarta localmente; no toca el Backend.
    public void descartar(int row) {
        List<Alerta> nueva = new ArrayList<>(model.getList());
        nueva.remove(row);
        model.setList(nueva);
    }
}
