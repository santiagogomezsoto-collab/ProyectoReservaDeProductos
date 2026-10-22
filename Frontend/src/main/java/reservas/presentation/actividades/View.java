package reservas.presentation.actividades;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class View implements PropertyChangeListener {
    private JPanel panel;
    private com.github.lgooddatepicker.components.DatePicker fechaReferenciaFld;
    private JButton cargarBtn;
    private JButton imprimirBtn;
    private JTable matrizFld;

    Controller controller;
    Model model;

    public View() {
        cargarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fechaReferenciaFld.getDate() == null) {
                    JOptionPane.showMessageDialog(panel, "Seleccione una fecha de referencia", "", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                controller.cargar(fechaReferenciaFld.getDate());
            }
        });

        imprimirBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    controller.print();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    public JPanel getPanel() {
        return panel;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void setModel(Model model) {
        this.model = model;
        model.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Model.MATRIZ)) {
            matrizFld.setModel(new TableModel(model.getHoras(), model.getDias(), model.getReservas()));
        }
        this.panel.revalidate();
    }
}