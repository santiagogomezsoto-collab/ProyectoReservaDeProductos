package reservas.logic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReservaIT {

    private String tempFilePath;

    @BeforeEach
    void setUp() throws IOException {
        File tempFile = Files.createTempFile("reservas-test", ".xml").toFile();
        tempFile.delete();
        tempFilePath = tempFile.getAbsolutePath();
        Service.resetForTesting(tempFilePath);
    }

    @AfterEach
    void tearDown() {
        new File(tempFilePath).delete();
    }

    @Test
    void reservaCreadaSeGuardaYSeRecuperaCorrectamenteDelXml() throws Exception {
        CategoriaRecurso categoria = new CategoriaRecurso("", "Sala de Reuniones");
        Service.instance().create(categoria);

        Recurso recurso = new Recurso("SALA-1", categoria, "Sala 1 - Piso 2");
        Service.instance().create(recurso);

        Funcionario funcionario = new Funcionario("f100", "f100", "Diana Solis", "8888-9999");
        Service.instance().create(funcionario);

        Reserva reserva = Service.instance().crearReserva(funcionario, "Sesion de Planificacion",
                LocalDate.of(2026, 9, 15), LocalTime.of(14, 0), LocalTime.of(15, 30),
                List.of(categoria));

        String idReserva = reserva.getId();

        Service.instance().stop();
        Service.resetForTesting(tempFilePath);

        Funcionario funcionarioRecargado = Service.instance().read(new Funcionario("f100", "", "", ""));
        assertEquals("Diana Solis", funcionarioRecargado.getNombre());

        List<Reserva> reservasRecargadas = Service.instance().findReservas(funcionarioRecargado);
        assertEquals(1, reservasRecargadas.size());

        Reserva reservaRecargada = reservasRecargadas.get(0);
        assertEquals(idReserva, reservaRecargada.getId());
        assertEquals("Sesion de Planificacion", reservaRecargada.getActividad());
        assertEquals(Reserva.ACTIVA, reservaRecargada.getEstado());

        assertEquals(1, reservaRecargada.getRecursos().size());
        assertEquals("SALA-1", reservaRecargada.getRecursos().get(0).getId());
        assertEquals("Sala de Reuniones", reservaRecargada.getRecursos().get(0).getCategoria().getDescripcion());
        assertEquals("Diana Solis", reservaRecargada.getFuncionario().getNombre());
    }

    @Test
    void reservaCanceladaMantieneSuEstadoTrasGuardarYRecargar() throws Exception {
        CategoriaRecurso categoria = new CategoriaRecurso("", "Proyector");
        Service.instance().create(categoria);
        Service.instance().create(new Recurso("PROY-9", categoria, "Proyector portatil"));

        Funcionario funcionario = new Funcionario("f200", "f200", "Mario Chaves", "7777-2222");
        Service.instance().create(funcionario);

        Reserva reserva = Service.instance().crearReserva(funcionario, "Capacitacion Anual",
                LocalDate.now().plusDays(10), LocalTime.of(8, 0), LocalTime.of(9, 0),
                List.of(categoria));

        Service.instance().cancelarReserva(reserva);
        Service.instance().stop();

        Service.resetForTesting(tempFilePath);

        List<Reserva> reservas = Service.instance().findReservas(funcionario);
        assertEquals(1, reservas.size());
        assertEquals(Reserva.CANCELADA, reservas.get(0).getEstado());
    }

    @Test
    void recursoAsignadoAUnaReservaNoQuedaDisponibleParaOtraEnElMismoHorario() throws Exception {
        CategoriaRecurso categoria = new CategoriaRecurso("", "Sala Pequena");
        Service.instance().create(categoria);
        Service.instance().create(new Recurso("SALA-P1", categoria, "Sala Pequena #1"));

        Funcionario func1 = new Funcionario("f300", "f300", "Funcionario Uno", "1111");
        Funcionario func2 = new Funcionario("f301", "f301", "Funcionario Dos", "2222");
        Service.instance().create(func1);
        Service.instance().create(func2);

        Service.instance().crearReserva(func1, "Reunion A", LocalDate.of(2026, 10, 1),
                LocalTime.of(10, 0), LocalTime.of(11, 0), List.of(categoria));

        Exception ex = assertThrows(Exception.class, () ->
                Service.instance().crearReserva(func2, "Reunion B", LocalDate.of(2026, 10, 1),
                        LocalTime.of(10, 30), LocalTime.of(11, 30), List.of(categoria)));

        assertTrue(ex.getMessage().contains("Sala Pequena"));
    }
}