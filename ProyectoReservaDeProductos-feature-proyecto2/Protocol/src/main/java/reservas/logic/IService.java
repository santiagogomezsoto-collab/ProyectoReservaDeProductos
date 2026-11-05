package reservas.logic;

import reservas.ia.ReservaExtraccion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface IService {
    Usuario login(String id, String clave) throws Exception;

    void changePassword(String id, String claveActual, String claveNueva) throws Exception;

    void create(Funcionario e) throws Exception;

    void update(Funcionario e) throws Exception;

    void delete(Funcionario e) throws Exception;

    List<Funcionario> search(Funcionario e);

    void create(CategoriaRecurso e) throws Exception;

    void update(CategoriaRecurso e) throws Exception;

    void delete(CategoriaRecurso e) throws Exception;

    List<CategoriaRecurso> search(CategoriaRecurso e);

    List<CategoriaRecurso> findAllCategorias();

    void create(Recurso e) throws Exception;

    void update(Recurso e) throws Exception;

    void delete(Recurso e) throws Exception;

    List<Recurso> search(CategoriaRecurso categoria, String descripcion);

    List<Reserva> findReservas(Funcionario funcionario);

    Reserva crearReserva(Funcionario funcionario, String actividad, LocalDate fecha,
                                LocalTime horaInicio, LocalTime horaFin,
                                List<CategoriaRecurso> categoriasSolicitadas) throws Exception;

    void cancelarReserva(Reserva e) throws Exception;

    List<Reserva> findReservasActivasPorFecha(LocalDate fecha);

    List<Reserva> findReservasActivasPorSemana(LocalDate inicioSemana, LocalDate finSemana);

    Map<String, Long> estadisticasRecursosPorCategoria(LocalDate desde, LocalDate hasta);

    Map<LocalDate, Long> estadisticasActividadesPorSemana(LocalDate desde, LocalDate hasta);

    ReservaExtraccion extraerReserva(String frase);
}
