package reservas.presentation.calendarizacion;

import reservas.logic.CategoriaRecurso;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class View implements PropertyChangeListener {
    private JPanel panel;
    private com.github.lgooddatepicker.components.DatePicker fechaFld;
    private JComboBox categoriaFld;
    private JButton cargarBtn;
    private JButton imprimirBtn;
    private JTable matrizFld;

    Controller controller;
    Model model;

    public View() {
        cargarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fechaFld.getDate() == null) {
                    JOptionPane.showMessageDialog(panel, "Seleccione una fecha", "", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (categoriaFld.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(panel, "Seleccione una categoria", "", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                CategoriaRecurso categoria = (CategoriaRecurso) categoriaFld.getSelectedItem();
                controller.cargar(categoria, fechaFld.getDate());
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
        switch (evt.getPropertyName()) {
            case Model.CATEGORIAS:
                categoriaFld.setModel(new DefaultComboBoxModel<>(model.getCategorias().toArray(new CategoriaRecurso[0])));
                break;
            case Model.MATRIZ:
                matrizFld.setModel(new TableModel(model.getHoras(), model.getRecursos(), model.getReservas()));
                break;
        }
        this.panel.revalidate();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}