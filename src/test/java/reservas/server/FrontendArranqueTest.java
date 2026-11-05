package reservas.server;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class FrontendArranqueTest {
    @Test void procesosSwingIndependientesAmbosRoles() throws Exception {
        try (Servidor servidor = new Servidor(new ServicioSimulado().service(), new Object(),
                java.net.InetAddress.getLoopbackAddress(), 0)) {
        Thread aceptador = new Thread(() -> {
            try { servidor.servir(); } catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
        aceptador.start();
        String cp = Path.of("target/test-classes").toAbsolutePath() + java.io.File.pathSeparator
                + Path.of("Frontend/target/classes").toAbsolutePath() + java.io.File.pathSeparator
                + Files.readString(Path.of("Frontend/target/a34-classpath.txt")).trim();
        for (String rol : new String[]{"admin", "f1"}) {
            Path log = Path.of("target/swing-" + rol + ".log");
            Process proceso = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-cp", cp, "reservas.client.FrontendSmokeProcess", Integer.toString(servidor.puerto()), rol)
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            try {
                assertTrue(proceso.waitFor(30, TimeUnit.SECONDS), "Proceso Swing no termino");
                String salida = Files.readString(log);
                assertEquals(0, proceso.exitValue(), salida);
                assertTrue(salida.contains("PASS Swing"), salida);
                ServiceProxyTest.esperar(() -> servidor.sesionesActivas() == 0);
            } finally { if (proceso.isAlive()) proceso.destroyForcibly(); }
        }
        }
    }
}
