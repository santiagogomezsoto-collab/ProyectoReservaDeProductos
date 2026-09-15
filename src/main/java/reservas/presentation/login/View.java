package reservas.presentation.login;

import reservas.logic.Usuario;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class View extends JDialog {
    private JPanel panel;
    private JTextField idFld;
    private JPasswordField claveFld;
    private JButton ingresarBtn;
    private JButton cancelarBtn;
    private JButton cambiarClaveBtn;

    Controller controller;
    Model model;

    public View() {
        setModal(true);

        ingresarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Usuario u = new Usuario();
                    u.setId(idFld.getText());
                    u.setClave(new String(claveFld.getPassword()));
                    controller.login(u);
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

        cambiarClaveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reservas.presentation.cambiarclave.View cambiarView = new reservas.presentation.cambiarclave.View();
                reservas.presentation.cambiarclave.Model cambiarModel = new reservas.presentation.cambiarclave.Model();
                new reservas.presentation.cambiarclave.Controller(cambiarView, cambiarModel);
                cambiarView.setLocationRelativeTo(panel);
                cambiarView.setVisible(true);
            }
        });

        setContentPane(panel);
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

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}