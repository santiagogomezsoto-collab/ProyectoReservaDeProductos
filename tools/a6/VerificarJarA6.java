import java.nio.file.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.jar.*;
class VerificarJarA6 {
 public static void main(String[] args)throws Exception {
  Path root=Path.of("").toAbsolutePath();
  for(String mod:new String[]{"Backend","Frontend","Protocol"}) {
   Path archivo=root.resolve(mod+"/target/reservas-"+mod.toLowerCase()+"-1.0-SNAPSHOT.jar");
   try(JarFile jar=new JarFile(archivo.toFile())) {
    if(jar.stream().anyMatch(e->e.getName().equals("data.xml")||e.getName().endsWith("/data.xml")||e.getName().endsWith(".properties")&&!e.getName().startsWith("META-INF/maven/")))throw new AssertionError("ARCHIVO_PRIVADO");
   }
  }
  String java=Path.of(System.getProperty("java.home"),"bin","java.exe").toString();
  String backend=root.resolve("Backend/src/main/resources")+File.pathSeparator+root.resolve("Backend/target/reservas-backend-1.0-SNAPSHOT.jar")+File.pathSeparator+Files.readString(root.resolve("Backend/target/a34-classpath.txt")).trim();
  String frontend=root.resolve("Frontend/target/reservas-frontend-1.0-SNAPSHOT.jar")+File.pathSeparator+Files.readString(root.resolve("Frontend/target/a34-classpath.txt")).trim();
  int puerto;try(ServerSocket s=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){puerto=s.getLocalPort();}
  Process server=new ProcessBuilder(java,"-cp",backend,"reservas.server.Servidor",""+puerto,"127.0.0.1").redirectError(root.resolve("target/a6-jar-server.err").toFile()).start();
  try {
   var out=new BufferedReader(new InputStreamReader(server.getInputStream()));
   var espera=Executors.newSingleThreadExecutor();
   try{if(!espera.submit(out::readLine).get(10,TimeUnit.SECONDS).startsWith("Servidor escuchando"))throw new AssertionError();}finally{espera.shutdownNow();}
   Process client=new ProcessBuilder(java,"-cp",frontend,"reservas.client.ClienteCategorias","127.0.0.1",""+puerto).redirectError(root.resolve("target/a6-jar-client.err").toFile()).start();
   if(!client.waitFor(15,TimeUnit.SECONDS)){client.destroy();throw new AssertionError("TIMEOUT");}
   String respuesta=new String(client.getInputStream().readAllBytes());
   if(client.exitValue()!=0||!respuesta.contains("Categorias recibidas: 1")||!respuesta.contains("CAT-001"))throw new AssertionError("CONSULTA");
   System.out.println("OK: tres JAR sin configuracion privada ni data.xml; Backend JAR con configuracion externa; ClienteCategorias JAR independiente; CAT-001 recibida por TCP.");
  }finally{server.destroy();server.waitFor(10,TimeUnit.SECONDS);}
 }
}
