package reservas.presentation.funcionarios;

import reservas.Application;
import reservas.logic.Funcionario;

import javax.swing.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class View implements PropertyChangeListener {
    private JPanel panel;
    private JTextField buscarIdFld;
    private JTextField buscarNombreFld;
    private JButton buscarBtn;
    private JTextField idFld;
    private JTextField nombreFld;
    private JTextField telefonoFld;
    private JButton guardarFld;
    private JButton borrarFld;
    private JButton limpiarFld;
    private JButton imprimirFld;
    private JTable listado;

    Controller controller;
    Model model;

    public View() {
        guardarFld.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validate_()) {
                    Funcionario f = take();
                    try {
                        if (idFld.isEditable()) {
                            controller.create(f);
                        } else {
                            controller.update(f);
                        }
                        JOptionPane.showMessageDialog(panel, "REGISTRO APLICADO", "", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(panel, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        borrarFld.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    controller.delete(take());
                    JOptionPane.showMessageDialog(panel, "REGISTRO BORRADO", "", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        limpiarFld.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.clear();
            }
        });

        buscarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.search(buscarIdFld.getText(), buscarNombreFld.getText());
            }
        });

        imprimirFld.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    controller.print();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        listado.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = listado.getSelectedRow();
                if (row >= 0) {
                    controller.edit(row);
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
            case Model.LIST:
                int[] cols = {TableModel.ID, TableModel.NOMBRE, TableModel.TELEFONO};
                listado.setModel(new TableModel(cols, model.getList()));
                break;
            case Model.CURRENT:
                idFld.setText(model.getCurrent().getId());
                idFld.setEditable(model.getCurrent().getId() == null || model.getCurrent().getId().isEmpty());
                nombreFld.setText(model.getCurrent().getNombre());
                telefonoFld.setText(model.getCurrent().getTelefono());
                nombreFld.setBackground(null);
                nombreFld.setToolTipText(null);
                telefonoFld.setBackground(null);
                telefonoFld.setToolTipText(null);
                break;
        }
        this.panel.revalidate();
    }

    public Funcionario take() {
        Funcionario e = new Funcionario();
        e.setId(idFld.getText());
        e.setNombre(nombreFld.getText());
        e.setTelefono(telefonoFld.getText());
        return e;
    }

    private boolean validate_() {
        boolean valid = true;
        if (idFld.getText().isEmpty()) {
            valid = false;
            idFld.setBackground(Application.BACKGROUND_ERROR);
            idFld.setToolTipText("Id requerido");
        } else {
            idFld.setBackground(null);
            idFld.setToolTipText(null);
        }

        if (nombreFld.getText().isEmpty()) {
            valid = false;
            nombreFld.setBackground(Application.BACKGROUND_ERROR);
            nombreFld.setToolTipText("Nombre requerido");
        } else {
            nombreFld.setBackground(null);
            nombreFld.setToolTipText(null);
        }

        if (telefonoFld.getText().isEmpty()) {
            valid = false;
            telefonoFld.setBackground(Application.BACKGROUND_ERROR);
            telefonoFld.setToolTipText("Telefono requerido");
        } else {
            telefonoFld.setBackground(null);
            telefonoFld.setToolTipText(null);
        }
        return valid;
    }
}