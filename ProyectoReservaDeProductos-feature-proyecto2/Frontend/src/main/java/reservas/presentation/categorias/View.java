package reservas.presentation.categorias;

import reservas.Application;
import reservas.logic.CategoriaRecurso;

import javax.swing.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class View implements PropertyChangeListener {
    private JPanel panel;
    private JTextField buscarFld;
    private JButton buscarBtn;
    private JTextField idFld;
    private JTextField descripcionFld;
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
                    CategoriaRecurso c = take();
                    try {
                        if (c.getId() == null || c.getId().isEmpty()) {
                            controller.create(c);
                        } else {
                            controller.update(c);
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
                controller.search(buscarFld.getText());
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
                int[] cols = {TableModel.ID, TableModel.DESCRIPCION};
                listado.setModel(new TableModel(cols, model.getList()));
                break;
            case Model.CURRENT:
                idFld.setText(model.getCurrent().getId());
                idFld.setEditable(model.getCurrent().getId() == null || model.getCurrent().getId().isEmpty());
                descripcionFld.setText(model.getCurrent().getDescripcion());
                descripcionFld.setBackground(null);
                descripcionFld.setToolTipText(null);
                break;
        }
        this.panel.revalidate();
    }

    public CategoriaRecurso take() {
        CategoriaRecurso e = new CategoriaRecurso();
        e.setId(idFld.getText());
        e.setDescripcion(descripcionFld.getText());
        return e;
    }

    private boolean validate_() {
        boolean valid = true;
        if (descripcionFld.getText().isEmpty()) {
            valid = false;
            descripcionFld.setBackground(Application.BACKGROUND_ERROR);
            descripcionFld.setToolTipText("Descripcion requerida");
        } else {
            descripcionFld.setBackground(null);
            descripcionFld.setToolTipText(null);
        }
        return valid;
    }
}