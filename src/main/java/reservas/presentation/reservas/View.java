package reservas.presentation.reservas;

import reservas.Application;
import reservas.logic.CategoriaRecurso;
import reservas.logic.Reserva;

import javax.swing.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class View implements PropertyChangeListener {
    private JPanel panel;
    private JTextField fraseFld;
    private JButton extraerBtn;
    private JTextField actividadFld;
    private com.github.lgooddatepicker.components.DatePicker fechaFld;
    private JComboBox horaInicioFld;
    private JComboBox horaFinFld;
    private JList categoriasFld;
    private JButton reservarBtn;
    private JButton cancelarBtn;
    private JButton limpiarBtn;
    private JButton imprimirBtn;
    private JTable misReservas;

    Controller controller;
    Model model;

    public View() {
        for (int h = 6; h <= 22; h++) {
            horaInicioFld.addItem(LocalTime.of(h, 0));
            horaInicioFld.addItem(LocalTime.of(h, 30));
            horaFinFld.addItem(LocalTime.of(h, 0));
            horaFinFld.addItem(LocalTime.of(h, 30));
        }

        reservarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validate_()) {
                    try {
                        List<CategoriaRecurso> seleccionadas = categoriasFld.getSelectedValuesList();
                        controller.reservar(actividadFld.getText(), fechaFld.getDate(),
                                (LocalTime) horaInicioFld.getSelectedItem(), (LocalTime) horaFinFld.getSelectedItem(),
                                seleccionadas);
                        JOptionPane.showMessageDialog(panel, "RESERVA APLICADA", "", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(panel, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        cancelarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = misReservas.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(panel, "Seleccione una reserva", "", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                Reserva r = ((TableModel) misReservas.getModel()).getRowAt(row);
                try {
                    controller.cancelar(r);
                    JOptionPane.showMessageDialog(panel, "RESERVA CANCELADA", "", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        limpiarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.limpiar();
            }
        });

        extraerBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    controller.extraerConIA(fraseFld.getText());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
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
                categoriasFld.setListData(model.getCategorias().toArray(new CategoriaRecurso[0]));
                break;
            case Model.LIST:
                int[] cols = {TableModel.ID, TableModel.ACTIVIDAD, TableModel.FECHA, TableModel.HORARIO, TableModel.RECURSOS, TableModel.ESTADO};
                misReservas.setModel(new TableModel(cols, model.getList()));
                break;
            case Model.CURRENT:
                NuevaReservaData d = model.getCurrent();
                actividadFld.setText(d.getActividad());
                fechaFld.setDate(d.getFecha());
                horaInicioFld.setSelectedItem(d.getHoraInicio());
                horaFinFld.setSelectedItem(d.getHoraFin());

                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < model.getCategorias().size(); i++) {
                    if (d.getCategoriasSeleccionadas().contains(model.getCategorias().get(i))) {
                        indices.add(i);
                    }
                }
                int[] arr = new int[indices.size()];
                for (int i = 0; i < arr.length; i++) arr[i] = indices.get(i);
                categoriasFld.setSelectedIndices(arr);

                actividadFld.setBackground(null);
                break;
        }
        this.panel.revalidate();
    }

    private boolean validate_() {
        boolean valid = true;
        if (actividadFld.getText().isEmpty()) {
            valid = false;
            actividadFld.setBackground(Application.BACKGROUND_ERROR);
            actividadFld.setToolTipText("Actividad requerida");
        } else {
            actividadFld.setBackground(null);
            actividadFld.setToolTipText(null);
        }

        if (fechaFld.getDate() == null) {
            valid = false;
            JOptionPane.showMessageDialog(panel, "Debe seleccionar una fecha", "Error", JOptionPane.ERROR_MESSAGE);
        }

        if (horaInicioFld.getSelectedItem() == null || horaFinFld.getSelectedItem() == null) {
            valid = false;
            JOptionPane.showMessageDialog(panel, "Debe seleccionar hora de inicio y fin", "Error", JOptionPane.ERROR_MESSAGE);
        }

        if (categoriasFld.isSelectionEmpty()) {
            valid = false;
            JOptionPane.showMessageDialog(panel, "Debe seleccionar al menos una categoria", "Error", JOptionPane.ERROR_MESSAGE);
        }

        return valid;
    }
}