package reservas;

import reservas.presentation.Sesion;

import javax.swing.*;

public class Application {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        doLogin();
        if (Sesion.isLoggedIn()) {
            doRun();
        }
    }

    private static void doLogin() {
        reservas.presentation.login.View loginView = new reservas.presentation.login.View();
        loginView.setTitle("Sistema de Reservas");
        loginView.pack();
        loginView.setLocationRelativeTo(null);
        reservas.presentation.login.Model loginModel = new reservas.presentation.login.Model();
        reservas.presentation.login.Controller loginController = new reservas.presentation.login.Controller(loginView, loginModel);
        loginView.setVisible(true);
    }

    private static void doRun() {
        JFrame window = new JFrame("Sistema de Reservas");
        JTabbedPane tabbedPane = new JTabbedPane();
        window.setContentPane(tabbedPane);

        window.setTitle("Sistema de Reservas - " + Sesion.getUsuario().getId() + " (" + Sesion.getUsuario().getRol() + ")");

        switch (Sesion.getUsuario().getRol()) {
            case "ADMIN":
                tabbedPane.addTab("Funcionarios", buildFuncionariosTab());
                tabbedPane.addTab("Categorias", buildCategoriasTab());
                tabbedPane.addTab("Recursos", buildRecursosTab());
                tabbedPane.addTab("Calendarizacion", buildCalendarizacionTab());
                tabbedPane.addTab("Actividades", buildActividadesTab());
                tabbedPane.addTab("Estadisticas", buildEstadisticasTab());
                break;
            case "FUNCIONARIO":
                tabbedPane.addTab("Reservas", buildReservasTab());
                tabbedPane.addTab("Calendarizacion", buildCalendarizacionTab());
                tabbedPane.addTab("Actividades", buildActividadesTab());
                tabbedPane.addTab("Estadisticas", buildEstadisticasTab());
                break;
        }

        window.setSize(800, 500);
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                reservas.logic.Service.instance().stop();
                System.exit(0);
            }
        });
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    private static JPanel buildReservasTab() {
        reservas.presentation.reservas.View view = new reservas.presentation.reservas.View();
        reservas.presentation.reservas.Model model = new reservas.presentation.reservas.Model();
        new reservas.presentation.reservas.Controller(view, model);
        return view.getPanel();
    }

    private static JPanel buildFuncionariosTab() {
        reservas.presentation.funcionarios.View view = new reservas.presentation.funcionarios.View();
        reservas.presentation.funcionarios.Model model = new reservas.presentation.funcionarios.Model();
        new reservas.presentation.funcionarios.Controller(view, model);
        return view.getPanel();
    }

    private static JPanel buildCategoriasTab() {
        reservas.presentation.categorias.View view = new reservas.presentation.categorias.View();
        reservas.presentation.categorias.Model model = new reservas.presentation.categorias.Model();
        new reservas.presentation.categorias.Controller(view, model);
        return view.getPanel();
    }

    private static JPanel buildRecursosTab() {
        reservas.presentation.recursos.View view = new reservas.presentation.recursos.View();
        reservas.presentation.recursos.Model model = new reservas.presentation.recursos.Model();
        new reservas.presentation.recursos.Controller(view, model);
        return view.getPanel();
    }

    private static JPanel buildCalendarizacionTab() {
        reservas.presentation.calendarizacion.View view = new reservas.presentation.calendarizacion.View();
        reservas.presentation.calendarizacion.Model model = new reservas.presentation.calendarizacion.Model();
        new reservas.presentation.calendarizacion.Controller(view, model);
        return view.getPanel();
    }

    private static JPanel buildActividadesTab() {
        reservas.presentation.actividades.View view = new reservas.presentation.actividades.View();
        reservas.presentation.actividades.Model model = new reservas.presentation.actividades.Model();
        new reservas.presentation.actividades.Controller(view, model);
        return view.getPanel();
    }

    private static JPanel buildEstadisticasTab() {
        reservas.presentation.estadisticas.View view = new reservas.presentation.estadisticas.View();
        reservas.presentation.estadisticas.Model model = new reservas.presentation.estadisticas.Model();
        new reservas.presentation.estadisticas.Controller(view, model);
        return view.getPanel();
    }

    public static final java.awt.Color BACKGROUND_ERROR = new java.awt.Color(255, 102, 102);
}