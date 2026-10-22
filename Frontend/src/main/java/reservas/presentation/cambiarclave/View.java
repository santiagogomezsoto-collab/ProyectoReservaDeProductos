package reservas.presentation.cambiarclave;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class View extends JDialog {
    private JPanel panel;
    private JTextField idFld;
    private JPasswordField claveActualFld;
    private JPasswordField claveNuevaFld;
    private JButton aceptarBtn;
    private JButton cancelarBtn;

    Controller controller;
    Model model;

    public View() {
        setModal(true);
        setTitle("Cambiar Clave");

        aceptarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    controller.cambiarClave(idFld.getText(),
                            new String(claveActualFld.getPassword()),
                            new String(claveNuevaFld.getPassword()));
                    JOptionPane.showMessageDialog(panel, "Clave actualizada", "", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        cancelarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        setContentPane(panel);
        pack();
    }

    public JPanel getPanel() {
        return panel;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void setModel(Model model) {
        this.model = model;
    }
}