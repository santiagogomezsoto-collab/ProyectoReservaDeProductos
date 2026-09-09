package reservas.presentation.estadisticas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class View implements PropertyChangeListener {
    private JPanel panel;
    private com.github.lgooddatepicker.components.DatePicker desdeRecursosFld;
    private com.github.lgooddatepicker.components.DatePicker hastaRecursosFld;
    private JButton cargarRecursosBtn;
    private JTable tablaRecursosFld;
    private JPanel graficoRecursosPanel;

    private com.github.lgooddatepicker.components.DatePicker desdeActividadesFld;
    private com.github.lgooddatepicker.components.DatePicker hastaActividadesFld;
    private JButton cargarActividadesBtn;
    private JTable tablaActividadesFld;
    private JPanel graficoActividadesPanel;

    private JButton imprimirBtn;

    Controller controller;
    Model model;

    public View() {
        cargarRecursosBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (desdeRecursosFld.getDate() == null || hastaRecursosFld.getDate() == null) {
                    JOptionPane.showMessageDialog(panel, "Seleccione ambas fechas", "", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                controller.cargarRecursos(desdeRecursosFld.getDate(), hastaRecursosFld.getDate());
            }
        });

        cargarActividadesBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (desdeActividadesFld.getDate() == null || hastaActividadesFld.getDate() == null) {
                    JOptionPane.showMessageDialog(panel, "Seleccione ambas fechas", "", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                controller.cargarActividades(desdeActividadesFld.getDate(), hastaActividadesFld.getDate());
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
            case Model.RECURSOS:
                tablaRecursosFld.setModel(new TableModel.Recursos(model.getRecursos()));
                graficoRecursosPanel.removeAll();
                graficoRecursosPanel.setLayout(new BorderLayout());
                graficoRecursosPanel.add(controller.construirGraficoRecursos(), BorderLayout.CENTER);
                graficoRecursosPanel.revalidate();
                graficoRecursosPanel.repaint();
                break;
            case Model.ACTIVIDADES:
                tablaActividadesFld.setModel(new TableModel.Actividades(model.getActividades()));
                graficoActividadesPanel.removeAll();
                graficoActividadesPanel.setLayout(new BorderLayout());
                graficoActividadesPanel.add(controller.construirGraficoActividades(), BorderLayout.CENTER);
                graficoActividadesPanel.revalidate();
                graficoActividadesPanel.repaint();
                break;
        }
        this.panel.revalidate();
    }
}