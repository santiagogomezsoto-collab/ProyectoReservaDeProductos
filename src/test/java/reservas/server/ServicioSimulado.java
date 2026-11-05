package reservas.server;

import reservas.logic.*;
import reservas.ia.ReservaExtraccion;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Solo fixtures en memoria; nunca crea usuarios ni accede a MySQL. */
public final class ServicioSimulado implements InvocationHandler {
    public final List<Method> llamadas = new CopyOnWriteArrayList<>();
    public volatile Object[] ultimosArgumentos;
    public volatile RuntimeException fallo;
    public volatile long demoraMillis;
    public final CountDownLatch entrada = new CountDownLatch(1);
    public final CategoriaRecurso categoria = new CategoriaRecurso("C1", "Equipo");
    public final Funcionario funcionario = new Funcionario("f1", "solo-prueba", "Persona de prueba", "123");
    public final Recurso recurso = new Recurso("R1", categoria, "Proyector");
    public final LocalDate fecha = LocalDate.of(2030, 1, 7);
    public final Reserva reserva;

    public ServicioSimulado() {
        reserva = new Reserva("V1", funcionario, "Clase", fecha, LocalTime.of(8, 0), LocalTime.of(9, 0));
        reserva.getRecursos().add(recurso);
    }
    public IService service() {
        return (IService) Proxy.newProxyInstance(IService.class.getClassLoader(), new Class<?>[]{IService.class}, this);
    }
    @Override public Object invoke(Object proxy, Method metodo, Object[] args) throws Throwable {
        llamadas.add(metodo);
        ultimosArgumentos = args;
        entrada.countDown();
        if (demoraMillis > 0) Thread.sleep(demoraMillis);
        if (fallo != null) throw fallo;
        return switch (metodo.getName()) {
            case "login" -> {
                if (!"solo-prueba".equals(args[1])) throw new Exception("Clave invalida");
                yield switch ((String) args[0]) {
                    case "admin" -> new Usuario("admin", "NO_TRANSMITIR", Usuario.ADMIN);
                    case "f1" -> funcionario;
                    default -> throw new Exception("No existe");
                };
            }
            case "changePassword" -> {
                if (!"solo-prueba".equals(args[1])) throw new Exception("Clave invalida");
                yield null;
            }
            case "create", "update", "delete", "cancelarReserva" -> null;
            case "findAllCategorias" -> List.of(categoria);
            case "search" -> {
                if (metodo.getParameterCount() == 2) yield List.of(recurso);
                if (metodo.getParameterTypes()[0] == Funcionario.class) yield List.of(funcionario);
                yield List.of(categoria);
            }
            case "findReservas", "findReservasActivasPorFecha", "findReservasActivasPorSemana" -> List.of(reserva);
            case "crearReserva" -> reserva;
            case "estadisticasRecursosPorCategoria" -> Map.of("Equipo", 3L);
            case "estadisticasActividadesPorSemana" -> Map.of(fecha, 2L);
            case "extraerReserva" -> new ReservaExtraccion("Clase", fecha.toString(), "08:00", "09:00", List.of("Equipo"));
            default -> throw new AssertionError(metodo);
        };
    }
}
