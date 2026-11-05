package reservas.presentation.alertas;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class View implements PropertyChangeListener {
    private static final int ACTIVIDAD = 0, VER = 1, DESC = 2;

    private final JPanel panel = new JPanel(new BorderLayout());
    private final JTable listado = new JTable();

    Controller controller;
    Model model;

    public View() {
        panel.setPreferredSize(new Dimension(220, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Alertas"));
        panel.add(new JScrollPane(listado), BorderLayout.CENTER);

        listado.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = listado.rowAtPoint(e.getPoint());
                int col = listado.columnAtPoint(e.getPoint());
                if (row < 0) return;
                if (col == VER) ver(row);
                else if (col == DESC) descartar(row);
            }
        });
    }

    private void ver(int row) {
        Alerta a = model.getList().get(row);
        StringBuilder sb = new StringBuilder();
        sb.append("Encargado: ").append(a.encargado()).append("\n");
        sb.append("Fecha: ").append(a.fecha()).append("\n");
        sb.append("Desde: ").append(a.desde()).append("\n");
        sb.append("Hasta: ").append(a.hasta()).append("\n");
        sb.append("Recursos:\n");
        for (String r : a.recursos()) sb.append("  - ").append(r).append("\n");
        JOptionPane.showMessageDialog(panel, sb.toString(), a.actividad(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void descartar(int row) {
        Alerta a = model.getList().get(row);
        int r = JOptionPane.showConfirmDialog(panel, a.actividad(), "Descartar?",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            controller.descartar(row);
        }
    }

    public JPanel getPanel() { return panel; }

    public void setController(Controller controller) { this.controller = controller; }

    public void setModel(Model model) {
        this.model = model;
        model.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (Model.LIST.equals(evt.getPropertyName())) {
            DefaultTableModel tm = new DefaultTableModel(new Object[]{"Actividad", "Ver", "Desc."}, 0) {
                @Override
                public boolean isCellEditable(int row, int col) { return false; }
            };
            for (Alerta a : model.getList()) {
                tm.addRow(new Object[]{a.encargado() + " - " + a.actividad(), "Ver", "X"});
            }
            listado.setModel(tm);
            listado.getColumnModel().getColumn(VER).setMaxWidth(45);
            listado.getColumnModel().getColumn(DESC).setMaxWidth(45);
        }
        panel.revalidate();
    }
}
