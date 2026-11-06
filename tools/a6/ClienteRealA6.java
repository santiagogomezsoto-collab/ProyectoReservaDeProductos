import reservas.Application;
import reservas.FrontendApplication;
import reservas.client.*;
import reservas.logic.*;
import reservas.protocol.CodigoError;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Auxiliar manual de prueba: secretos solo por stdin, nunca por argumentos o logs. */
public class ClienteRealA6 {
    static final LocalDate FECHA = LocalDate.of(2026,11,6);
    static final String ACTIVIDAD = "A6 - Validacion integral TCP y alertas";
    static ServiceProxy proxy;
    static JFrame principal;
    static String id;
    static volatile String fallo;
    static int etapa;
    static Object campo(Object objeto, String nombre) throws Exception {
        Field f = (objeto instanceof Class<?> c ? c : objeto.getClass()).getDeclaredField(nombre);
        f.setAccessible(true); return f.get(objeto instanceof Class<?> ? null : objeto);
    }
    static void exigir(boolean ok, String codigo) { if (!ok) throw new AssertionError(codigo); }
    interface Accion { void ejecutar() throws Exception; }
    static void edt(Accion accion) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { try { accion.ejecutar(); } catch (Throwable e) { error.set(e); } });
        if (error.get()!=null) throw new AssertionError(error.get().getClass().getSimpleName());
        exigir(fallo==null, "ERROR_INTERFAZ");
    }
    static JTabbedPane tabs() { return (JTabbedPane)((BorderLayout)principal.getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER); }
    static Object buscarVista(Component c, String tipo) throws Exception {
        if (c instanceof JButton boton) for (var listener : boton.getActionListeners()) {
            for (Field f : listener.getClass().getDeclaredFields()) if (f.getType().getName().equals(tipo)) {
                f.setAccessible(true); return f.get(listener);
            }
        }
        if (c instanceof Container contenedor) for (Component hijo : contenedor.getComponents()) {
            Object vista = buscarVista(hijo,tipo); if (vista!=null) return vista;
        }
        return null;
    }
    static Object vista(String modulo) throws Exception {
        Object v = buscarVista(principal, "reservas.presentation."+modulo+".View");
        exigir(v!=null,"VISTA_NO_ENCONTRADA");
        JPanel panel = (JPanel)campo(v,"panel"); tabs().setSelectedComponent(panel); return v;
    }
    static void fecha(Object vista,String campo,LocalDate fecha) throws Exception {
        ((com.github.lgooddatepicker.components.DatePicker)campo(vista,campo)).setDate(fecha);
    }
    static void click(Object vista,String nombre) throws Exception { ((JButton)campo(vista,nombre)).doClick(); }
    static JOptionPane pane(Container root) {
        for (Component c:root.getComponents()) {
            if(c instanceof JOptionPane p) return p;
            if(c instanceof Container ct) { var p=pane(ct); if(p!=null)return p; }
        } return null;
    }
    static void dialogo(Accion accion,String titulo,String contenido,int opcion) throws Exception {
        AtomicBoolean visto = new AtomicBoolean(); AtomicReference<Throwable> error = new AtomicReference<>();
        long fin=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);
        Timer timer=new Timer(40,null);
        timer.addActionListener(e -> {
            for(Window w:Window.getWindows()) if(w.isShowing() && w instanceof JDialog d) {
                JOptionPane p=pane(d);
                if(p==null)continue;
                try {
                    exigir(titulo.equals(d.getTitle()),"TITULO_DIALOGO_INESPERADO");
                    exigir(String.valueOf(p.getMessage()).contains(contenido),"CONTENIDO_DIALOGO_INESPERADO");
                    visto.set(true); p.setValue(opcion);
                } catch(Throwable ex) { error.set(ex); d.dispose(); }
                timer.stop(); return;
            }
            if(System.nanoTime()>fin) {
                error.set(new AssertionError("TIMEOUT_DIALOGO"));
                for(Window w:Window.getWindows())if(w instanceof JDialog)w.dispose();
                timer.stop();
            }
        });
        timer.start(); try{accion.ejecutar();}finally{timer.stop();}
        exigir(visto.get() && error.get()==null,"DIALOGO_FALLO");
    }
    static reservas.presentation.alertas.Model alertas() throws Exception {
        return (reservas.presentation.alertas.Model)campo(Application.getAlertas(),"model");
    }
    static void contarAlertas(int esperado) throws Exception {
        for(int i=0;i<60;i++) {
            AtomicInteger n=new AtomicInteger(); edt(()->n.set(alertas().getList().size()));
            if(n.get()==esperado) {Thread.sleep(300);edt(()->exigir(alertas().getList().size()==esperado,"ALERTA_DUPLICADA"));return;}
            Thread.sleep(100);
        } throw new AssertionError("CONTEO_ALERTAS");
    }
    static void consultaPantallas(int activas) throws Exception {
        edt(()->{
            Object cal=vista("calendarizacion");fecha(cal,"fechaFld",FECHA);
            ((JComboBox<?>)campo(cal,"categoriaFld")).setSelectedIndex(0);click(cal,"cargarBtn");
            exigir(((reservas.presentation.calendarizacion.Model)campo(cal,"model")).getReservas().size()==activas,"CALENDARIO");
            Object act=vista("actividades");fecha(act,"fechaReferenciaFld",FECHA);click(act,"cargarBtn");
            exigir(((reservas.presentation.actividades.Model)campo(act,"model")).getReservas().size()==activas,"ACTIVIDADES");
            Object est=vista("estadisticas");
            fecha(est,"desdeRecursosFld",FECHA.minusDays(4));fecha(est,"hastaRecursosFld",FECHA.plusDays(2));click(est,"cargarRecursosBtn");
            fecha(est,"desdeActividadesFld",FECHA.minusDays(4));fecha(est,"hastaActividadesFld",FECHA.plusDays(2));click(est,"cargarActividadesBtn");
            var m=(reservas.presentation.estadisticas.Model)campo(est,"model");
            exigir(m.getRecursos().size()==activas && m.getActividades().size()==activas,"ESTADISTICAS_TABLAS");
        });
        exigir(proxy.findReservasActivasPorFecha(FECHA).size()==activas,"CONSULTA_FECHA");
        exigir(proxy.findReservasActivasPorSemana(FECHA.minusDays(4),FECHA.plusDays(2)).size()==activas,"CONSULTA_SEMANA");
        exigir(proxy.estadisticasRecursosPorCategoria(FECHA,FECHA).values().stream().mapToLong(Long::longValue).sum()==activas,"ESTADISTICAS_CATEGORIA");
        exigir(proxy.estadisticasActividadesPorSemana(FECHA,FECHA).values().stream().mapToLong(Long::longValue).sum()==activas,"ESTADISTICAS_SEMANA");
    }
    static void ejecutar(String comando) throws Exception {
        switch(comando) {
            case "CONSULTAS" -> {
                var cats=proxy.findAllCategorias();exigir(cats.size()==1 && cats.get(0).getId().equals("CAT-001"),"CATEGORIAS");
                exigir(proxy.search(new CategoriaRecurso("","Laboratorios")).size()==1,"BUSQUEDA_CATEGORIA");
                exigir(proxy.search(cats.get(0),"").get(0).getId().equals("RES-001"),"RECURSOS");
                if(id.equals("A6-ADMIN")) exigir(proxy.search(new Funcionario("","","","")).size()==2,"FUNCIONARIOS");
                else {
                    try{proxy.search(new Funcionario("","","",""));throw new AssertionError("PERMISO_FUNCIONARIOS");}
                    catch(ServiceProxyException e){exigir(e.getCodigo()==CodigoError.NO_AUTORIZADO,"CODIGO_PERMISO");}
                    try{proxy.findReservas(new Funcionario(id.equals("A6-FUN-A")?"A6-FUN-B":"A6-FUN-A","","",""));throw new AssertionError("RESERVAS_AJENAS");}
                    catch(ServiceProxyException e){exigir(e.getCodigo()==CodigoError.NO_AUTORIZADO,"CODIGO_PERMISO");}
                }
            }
            case "PANTALLAS_0" -> consultaPantallas(0);
            case "PANTALLAS_1" -> consultaPantallas(1);
            case "CREAR" -> {
                exigir(id.equals("A6-FUN-A"),"IDENTIDAD_CREACION");
                exigir(LocalDateTime.now().isBefore(FECHA.atTime(10,0)),"HORARIO_PASADO");
                exigir(proxy.findReservas((Funcionario)reservas.presentation.Sesion.getUsuario()).isEmpty(),"RESERVAS_YA_EXISTENTES");
                edt(()->{
                    Object v=vista("reservas");((JTextField)campo(v,"actividadFld")).setText(ACTIVIDAD);
                    fecha(v,"fechaFld",FECHA);((JComboBox<?>)campo(v,"horaInicioFld")).setSelectedItem(LocalTime.of(10,0));
                    ((JComboBox<?>)campo(v,"horaFinFld")).setSelectedItem(LocalTime.of(11,0));
                    ((JList<?>)campo(v,"categoriasFld")).setSelectedIndex(0);
                    dialogo(()->click(v,"reservarBtn"),"","RESERVA APLICADA",JOptionPane.OK_OPTION);
                });
            }
            case "PERSISTE" -> {
                var list=proxy.findReservas((Funcionario)reservas.presentation.Sesion.getUsuario());
                exigir(list.size()==1 && list.get(0).getId().equals("RES-000001") && list.get(0).getEstado().equals("ACTIVA"),"PERSISTENCIA");
                edt(()->{
                    Object v=vista("reservas");JTable tabla=(JTable)campo(v,"misReservas");
                    exigir(tabla.getRowCount()==1 && "RES-000001".equals(tabla.getValueAt(0,0)),"FILA_RESERVA");
                });
            }
            case "ALERTA_0" -> contarAlertas(0);
            case "ALERTA_1" -> contarAlertas(1);
            case "VER", "DESCARTAR_NO", "DESCARTAR_SI" -> edt(()->{
                Object v=campo(Application.getAlertas(),"view");JTable tabla=(JTable)campo(v,"listado");
                exigir(tabla.getRowCount()==1,"ALERTA_AUSENTE");
                boolean ver=comando.equals("VER"); Rectangle celda=tabla.getCellRect(0,ver?1:2,true);
                dialogo(()->tabla.dispatchEvent(new MouseEvent(tabla,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,celda.x+2,celda.y+2,1,false)),
                    ver?ACTIVIDAD:"Descartar?",ver?"2026-11-06":ACTIVIDAD,
                    ver?JOptionPane.OK_OPTION:comando.equals("DESCARTAR_NO")?JOptionPane.NO_OPTION:JOptionPane.YES_OPTION);
                exigir(alertas().getList().size()==(comando.equals("DESCARTAR_SI")?0:1),"DESCARTE_LOCAL");
            });
            case "CANCELAR_AJENA" -> {
                exigir(id.equals("A6-FUN-B"),"IDENTIDAD_RECHAZO");
                Reserva r=proxy.findReservasActivasPorFecha(FECHA).stream().filter(x->x.getId().equals("RES-000001")).findFirst().orElseThrow();
                try{proxy.cancelarReserva(r);throw new AssertionError("CANCELACION_AJENA_ACEPTADA");}
                catch(ServiceProxyException e){exigir(e.getCodigo()==CodigoError.NO_AUTORIZADO,"CANCELACION_AJENA_CODIGO");}
            }
            case "CANCELAR_PROPIA" -> {
                exigir(id.equals("A6-FUN-A"),"IDENTIDAD_CANCELACION");
                edt(()->{
                    Object v=vista("reservas");JTable tabla=(JTable)campo(v,"misReservas");
                    exigir(tabla.getRowCount()==1,"CANCELACION_FILA");tabla.setRowSelectionInterval(0,0);
                    dialogo(()->click(v,"cancelarBtn"),"","RESERVA CANCELADA",JOptionPane.OK_OPTION);
                });
                exigir(proxy.findReservas((Funcionario)reservas.presentation.Sesion.getUsuario()).get(0).getEstado().equals("CANCELADA"),"ESTADO_CANCELADA");
            }
            case "DESCONECTADO" -> {
                try{proxy.findAllCategorias();throw new AssertionError("SERVIDOR_NO_DESCONECTADO");}
                catch(ServiceProxyException e){exigir(e.isConexionPerdida(),"ERROR_DESCONEXION");}
                exigir(proxy.isClosed(),"PROXY_NO_CERRADO");
            }
            case "CERRAR" -> {System.out.println("OK CERRAR");System.out.flush();edt(()->principal.dispatchEvent(new WindowEvent(principal,WindowEvent.WINDOW_CLOSING)));}
            default -> throw new AssertionError("COMANDO_DESCONOCIDO");
        }
    }
    public static void main(String[] args) {
        String comando="ARRANQUE";
        try {
            for(String clase:new String[]{"reservas.logic.Service","reservas.data.Database","com.mysql.cj.jdbc.Driver"}) {
                try{Class.forName(clase);throw new AssertionError("DEPENDENCIA_BACKEND");}catch(ClassNotFoundException esperado){}
            }
            exigir(ClienteRealA6.class.getResource("/reservas.properties")==null,"CONFIGURACION_EN_FRONTEND");
            id=args[1];DataInputStream entrada=new DataInputStream(System.in);String clave=entrada.readUTF();
            CountDownLatch listo=new CountDownLatch(1);
            SwingUtilities.invokeLater(()->{
                Timer timer=new Timer(100,null);timer.setCoalesce(false);
                timer.addActionListener(e->{try{
                    for(Window w:Window.getWindows())if(w.isShowing()) {
                        if(w instanceof reservas.presentation.cambiarclave.View && etapa==1){etapa=2;click(w,"cancelarBtn");}
                        else if(w instanceof JDialog d && "Error".equals(d.getTitle()) && etapa==3){etapa=4;d.dispose();}
                        else if(w instanceof reservas.presentation.login.View) {
                            if(etapa==0){etapa=1;click(w,"cambiarClaveBtn");}
                            else if(etapa==2){etapa=3;((JTextField)campo(w,"idFld")).setText("__A6_INEXISTENTE__");((JPasswordField)campo(w,"claveFld")).setText("invalida");click(w,"ingresarBtn");}
                            else if(etapa==4){etapa=5;((JTextField)campo(w,"idFld")).setText(id);((JPasswordField)campo(w,"claveFld")).setText(clave);click(w,"ingresarBtn");}
                        } else if(w instanceof JFrame frame && etapa==5 && frame.getTitle().contains(id)) {
                            principal=frame;proxy=(ServiceProxy)campo(Application.class,"service");
                            SocketListener receptor=(SocketListener)campo(proxy,"listener");
                            if(receptor==null || !receptor.isConectado())continue;
                            exigir(tabs().getTabCount()==(id.equals("A6-ADMIN")?6:4),"PESTANAS_ROL");
                            for(int i=0;i<tabs().getTabCount();i++)tabs().setSelectedIndex(i);
                            etapa=6;timer.stop();listo.countDown();
                        }
                    }
                }catch(Throwable ex){fallo="ARRANQUE_GUI";timer.stop();listo.countDown();}});
                timer.start();
            });
            FrontendApplication.main(new String[]{"127.0.0.1",args[0]});
            exigir(listo.await(35,TimeUnit.SECONDS) && fallo==null,"TIMEOUT_LOGIN_GUI");
            System.out.println("OK ARRANQUE");System.out.flush();
            while(true){comando=entrada.readUTF();ejecutar(comando);System.out.println("OK "+comando);System.out.flush();}
        }catch(Throwable e){System.out.println("FAIL "+comando+" "+e.getClass().getSimpleName());System.out.flush();System.exit(2);}
    }
}
