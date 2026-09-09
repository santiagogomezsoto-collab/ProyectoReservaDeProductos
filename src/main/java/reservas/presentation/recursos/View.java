package reservas.presentation.recursos;

import reservas.Application;
import reservas.logic.CategoriaRecurso;
import reservas.logic.Recurso;

import javax.swing.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class View implements PropertyChangeListener {
    private JPanel panel;
    private JComboBox filtroCategoria;
    private JTextField filtroDescripcion;
    private JButton buscarBtn;
    private JTextField idFld;
    private JComboBox categoriaFld;
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
                    Recurso r = take();
                    try {
                        if (idFld.isEditable()) {
                            controller.create(r);
                        } else {
                            controller.update(r);
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
                CategoriaRecurso cat = (CategoriaRecurso) filtroCategoria.getSelectedItem();
                controller.search(cat, filtroDescripcion.getText());
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
            case Model.CATEGORIAS:
                CategoriaRecurso[] cats = model.getCategorias().toArray(new CategoriaRecurso[0]);
                categoriaFld.setModel(new DefaultComboBoxModel<>(cats));

                CategoriaRecurso[] catsConTodas = new CategoriaRecurso[cats.length + 1];
                catsConTodas[0] = null;
                System.arraycopy(cats, 0, catsConTodas, 1, cats.length);
                filtroCategoria.setModel(new DefaultComboBoxModel<>(catsConTodas));
                break;
            case Model.LIST:
                int[] cols = {TableModel.ID, TableModel.CATEGORIA, TableModel.DESCRIPCION};
                listado.setModel(new TableModel(cols, model.getList()));
                break;
            case Model.CURRENT:
                idFld.setText(model.getCurrent().getId());
                idFld.setEditable(model.getCurrent().getId() == null || model.getCurrent().getId().isEmpty());
                categoriaFld.setSelectedItem(model.getCurrent().getCategoria());
                descripcionFld.setText(model.getCurrent().getDescripcion());
                descripcionFld.setBackground(null);
                descripcionFld.setToolTipText(null);
                break;
        }
        this.panel.revalidate();
    }

    public Recurso take() {
        Recurso e = new Recurso();
        e.setId(idFld.getText());
        e.setCategoria((CategoriaRecurso) categoriaFld.getSelectedItem());
        e.setDescripcion(descripcionFld.getText());
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

        if (categoriaFld.getSelectedItem() == null) {
            valid = false;
            categoriaFld.setToolTipText("Categoria requerida");
        } else {
            categoriaFld.setToolTipText(null);
        }

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