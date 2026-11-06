import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Ejecucion manual unica del plan autorizado; JDBC exclusivamente de lectura. */
public class ValidacionRealA6 {
    static final Path ROOT=Path.of("").toAbsolutePath();
    static final List<Cliente> clientes=new ArrayList<>();
    static final ExecutorService lecturas=Executors.newCachedThreadPool();
    static Process servidor; static Connection db; static String etapa="PRECONDICIONES";
    static String cp(String modulo)throws Exception{return ROOT.resolve(modulo+"/target/classes")+File.pathSeparator+Files.readString(ROOT.resolve(modulo+"/target/a34-classpath.txt")).trim();}
    static String java(){return Path.of(System.getProperty("java.home"),"bin","java.exe").toString();}
    static void exigir(boolean ok){if(!ok)throw new AssertionError("VALIDACION");}
    static void log(String s){System.out.println(s);System.out.flush();}
    static List<List<String>> consulta(String sql)throws Exception{
        try(var s=db.prepareStatement(sql);var r=s.executeQuery()){
            var filas=new ArrayList<List<String>>();while(r.next()){
                var fila=new ArrayList<String>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)fila.add(r.getString(i));filas.add(fila);
            }return filas;
        }
    }
    static void conteos(int reservas)throws Exception{
        String[] tablas={"usuario","funcionario","categoria_recurso","recurso","reserva","reserva_recurso"};
        int[] esperados={3,2,1,1,reservas,reservas};
        for(int i=0;i<tablas.length;i++)exigir(consulta("SELECT COUNT(*) FROM "+tablas[i]).get(0).get(0).equals(""+esperados[i]));
    }
    static void reserva(String estado)throws Exception{
        etapa="MYSQL_"+estado;conteos(1);
        exigir(consulta("SELECT id,funcionario_id,actividad,CAST(fecha AS CHAR),TIME_FORMAT(hora_inicio,'%H:%i'),TIME_FORMAT(hora_fin,'%H:%i'),estado FROM reserva").equals(List.of(List.of("RES-000001","A6-FUN-A","A6 - Validacion integral TCP y alertas","2026-11-06","10:00","11:00",estado))));
        exigir(consulta("SELECT reserva_id,recurso_id FROM reserva_recurso").equals(List.of(List.of("RES-000001","RES-001"))));
        log("OK MYSQL reserva RES-000001 "+estado+"; asociacion RES-001 conservada");
    }
    static final class Cliente {
        Process proceso; DataOutputStream entrada; BufferedReader salida;String id;
        Cliente(int puerto,String id,String clave)throws Exception{
            this.id=id;etapa=id+" ARRANQUE";
            proceso=new ProcessBuilder(java(),"-cp",ROOT.resolve("target/a6-classes")+File.pathSeparator+cp("Frontend"),"ClienteRealA6",""+puerto,id)
                .redirectError(ROOT.resolve("target/a6-"+id+".err").toFile()).start();
            clientes.add(this);entrada=new DataOutputStream(proceso.getOutputStream());salida=new BufferedReader(new InputStreamReader(proceso.getInputStream()));
            entrada.writeUTF(clave);entrada.flush();esperar("ARRANQUE");
        }
        void esperar(String cmd)throws Exception{
            Future<String> f=lecturas.submit(()->{String linea;while((linea=salida.readLine())!=null){if(linea.startsWith("OK ")||linea.startsWith("FAIL "))return linea;}return "EOF";});
            String respuesta=f.get(45,TimeUnit.SECONDS);
            if(!respuesta.equals("OK "+cmd)) {log("FALLO AUXILIAR "+id+" "+cmd+" (sin detalles sensibles)");throw new AssertionError("CLIENTE");}
            log("OK "+id+" "+cmd);
        }
        void orden(String cmd)throws Exception{etapa=id+" "+cmd;entrada.writeUTF(cmd);entrada.flush();esperar(cmd);}
        void cerrar()throws Exception{orden("CERRAR");exigir(proceso.waitFor(10,TimeUnit.SECONDS)&&proceso.exitValue()==0);}
    }
    public static void main(String[] args){
        int codigo=1;
        try{
            exigir(args.length==1&&args[0].equals("PLAN-A6-AUTORIZADO-2026-11-06"));
            exigir(LocalDateTime.now().isBefore(LocalDate.of(2026,11,6).atTime(10,0)));
            Properties p=new Properties();try(var in=Files.newInputStream(ROOT.resolve("Backend/src/main/resources/reservas.properties"))){p.load(in);}
            exigir(Set.of("localhost","127.0.0.1").contains(p.getProperty("database_server")));
            exigir(p.getProperty("database_name").equals("reservas_proyecto2"));
            Class.forName(p.getProperty("database_driver"));Properties acceso=new Properties();
            acceso.setProperty("user",p.getProperty("database_user"));acceso.setProperty("password",p.getProperty("database_password",""));acceso.setProperty("connectionTimeZone","UTC");
            db=DriverManager.getConnection("jdbc:mysql://"+p.getProperty("database_server")+":"+p.getProperty("database_port")+"/reservas_proyecto2",acceso);db.setReadOnly(true);
            exigir(consulta("SELECT @@hostname").get(0).get(0).equalsIgnoreCase(System.getenv("COMPUTERNAME")));
            conteos(0);
            exigir(consulta("SELECT id,rol FROM usuario ORDER BY id").equals(List.of(List.of("A6-ADMIN","ADMIN"),List.of("A6-FUN-A","FUNCIONARIO"),List.of("A6-FUN-B","FUNCIONARIO"))));
            exigir(consulta("SELECT id,categoria_id FROM recurso").equals(List.of(List.of("RES-001","CAT-001"))));
            // Credenciales exclusivamente en memoria y stdin de procesos; nunca salida o argumentos.
            Map<String,String> claves=new HashMap<>();for(var fila:consulta("SELECT id,clave FROM usuario"))claves.put(fila.get(0),fila.get(1));
            log("OK precondiciones; conteos 3,2,1,1,0,0; JDBC solo lectura");
            int puerto;try(ServerSocket s=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){puerto=s.getLocalPort();}
            etapa="BACKEND";Path salida=ROOT.resolve("target/a6-servidor.out");
            servidor=new ProcessBuilder(java(),"-cp",cp("Backend"),"reservas.server.Servidor",""+puerto,"127.0.0.1")
                .redirectOutput(salida.toFile()).redirectError(ROOT.resolve("target/a6-servidor.err").toFile()).start();
            boolean listo=false;for(int i=0;i<100;i++){if(Files.exists(salida)&&Files.readString(salida).contains("Servidor escuchando")){listo=true;break;}Thread.sleep(100);}
            exigir(listo);log("OK Backend en proceso independiente");
            Cliente admin=new Cliente(puerto,"A6-ADMIN",claves.get("A6-ADMIN"));
            Cliente a=new Cliente(puerto,"A6-FUN-A",claves.get("A6-FUN-A"));
            Cliente b=new Cliente(puerto,"A6-FUN-B",claves.get("A6-FUN-B"));
            for(Cliente c:List.of(admin,a,b)){c.orden("CONSULTAS");c.orden("PANTALLAS_0");c.orden("ALERTA_0");}
            conteos(0);a.orden("CREAR");reserva("ACTIVA");b.orden("ALERTA_1");a.orden("ALERTA_0");
            for(Cliente c:List.of(admin,a,b))c.orden("PANTALLAS_1");
            b.orden("ALERTA_1");a.orden("ALERTA_0");b.orden("VER");reserva("ACTIVA");
            b.orden("DESCARTAR_NO");reserva("ACTIVA");b.orden("DESCARTAR_SI");reserva("ACTIVA");b.orden("ALERTA_0");
            a.cerrar();a=new Cliente(puerto,"A6-FUN-A",claves.get("A6-FUN-A"));a.orden("PERSISTE");reserva("ACTIVA");
            b.orden("CANCELAR_AJENA");reserva("ACTIVA");a.orden("CANCELAR_PROPIA");reserva("CANCELADA");
            for(Cliente c:List.of(admin,a,b))c.orden("PANTALLAS_0");
            a.cerrar();admin.cerrar();servidor.destroy();exigir(servidor.waitFor(10,TimeUnit.SECONDS));
            b.orden("DESCONECTADO");b.cerrar();reserva("CANCELADA");
            log("APROBADO PLAN A6; conteos finales 3,2,1,1,1,1; evidencia conservada");codigo=0;
        }catch(Throwable e){log("DETENIDO en "+etapa+"; tipo "+e.getClass().getSimpleName()+"; no se ejecutaran mas escrituras");}
        finally{
            for(Cliente c:clientes)if(c.proceso.isAlive())c.proceso.destroy();
            if(servidor!=null&&servidor.isAlive())servidor.destroy();
            if(db!=null)try{db.close();}catch(Exception ignored){}
            lecturas.shutdownNow();
        }
        System.exit(codigo);
    }
}
