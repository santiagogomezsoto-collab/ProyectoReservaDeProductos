package reservas.logic;

import reservas.data.CategoriaRecursoDao;
import reservas.data.Database;
import reservas.data.FuncionarioDao;
import reservas.data.RecursoDao;
import reservas.data.ReservaDao;
import reservas.data.UsuarioDao;
import reservas.ia.ReservaExtraccion;
import reservas.ia.ReservaExtractorService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Logica de negocio. Unico lugar donde viven las reglas del sistema.
 *
 * La persistencia ahora es MySQL: el Service no sabe SQL, le pide los datos a
 * los DAO. Las 23 operaciones de IService conservan exactamente sus firmas y
 * sus reglas, asi que ni los controladores del Frontend ni el Worker del
 * servidor necesitan cambios.
 *
 * Nota sobre errores: las operaciones de consulta de IService no declaran
 * throws, asi que un fallo de base de datos se informa por consola y se
 * devuelve una coleccion vacia, igual que en el ejemplo visto en clase. Las
 * operaciones que modifican datos si propagan el error al usuario.
 */
public class Service implements IService {
    private static Service theInstance;

    public static Service instance() {
        if (theInstance == null) theInstance = new Service();
        return theInstance;
    }

    private final UsuarioDao usuarioDao;
    private final FuncionarioDao funcionarioDao;
    private final CategoriaRecursoDao categoriaDao;
    private final RecursoDao recursoDao;
    private final ReservaDao reservaDao;

    private Service() {
        usuarioDao = new UsuarioDao();
        funcionarioDao = new FuncionarioDao();
        categoriaDao = new CategoriaRecursoDao();
        recursoDao = new RecursoDao();
        reservaDao = new ReservaDao();
    }

    /**
     * Antes volcaba los datos al archivo XML. Con MySQL cada operacion queda
     * guardada en el momento, asi que no hay nada que persistir al cerrar.
     * Se conserva el metodo porque el Frontend lo recibe como accion de cierre.
     */
    public void stop() {
    }

    static void resetForTesting() {
        Database.resetForTesting();
        theInstance = new Service();
    }

    /**
     * Se conserva la firma que usan las pruebas existentes. El parametro era la
     * ruta del XML y ya no se usa: la conexion viene de reservas.properties.
     */
    static void resetForTesting(String path) {
        Database.resetForTesting();
        theInstance = new Service();
    }

    // =============== LOGIN ===============
    @Override
    public Usuario login(String id, String clave) throws Exception {
        Usuario result = usuarioDao.read(id); // lanza "Usuario no existe"
        if (!result.getClave().equals(clave)) throw new Exception("Clave incorrecta");
        return result;
    }

    @Override
    public void changePassword(String id, String claveActual, String claveNueva) throws Exception {
        Usuario u = usuarioDao.read(id); // lanza "Usuario no existe"
        if (!u.getClave().equals(claveActual)) throw new Exception("Clave actual incorrecta");
        usuarioDao.updateClave(id, claveNueva);
    }

    // =============== FUNCIONARIOS ===============
    @Override
    public void create(Funcionario e) throws Exception {
        funcionarioDao.create(e);
    }

    public Funcionario read(Funcionario e) throws Exception {
        return funcionarioDao.read(e);
    }

    @Override
    public void update(Funcionario e) throws Exception {
        funcionarioDao.update(e);
    }

    @Override
    public void delete(Funcionario e) throws Exception {
        funcionarioDao.delete(e);
    }

    @Override
    public List<Funcionario> search(Funcionario e) {
        try {
            return funcionarioDao.search(e);
        } catch (Exception ex) {
            System.err.println("Error buscando funcionarios: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    // =============== CATEGORIAS DE RECURSO ===============
    @Override
    public void create(CategoriaRecurso e) throws Exception {
        categoriaDao.create(e);
    }

    public CategoriaRecurso read(CategoriaRecurso e) throws Exception {
        return categoriaDao.read(e);
    }

    @Override
    public void update(CategoriaRecurso e) throws Exception {
        categoriaDao.update(e);
    }

    @Override
    public void delete(CategoriaRecurso e) throws Exception {
        categoriaDao.delete(e);
    }

    @Override
    public List<CategoriaRecurso> search(CategoriaRecurso e) {
        try {
            return categoriaDao.findByDescripcion(e);
        } catch (Exception ex) {
            System.err.println("Error buscando categorias: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<CategoriaRecurso> findAllCategorias() {
        try {
            return categoriaDao.findAll();
        } catch (Exception ex) {
            System.err.println("Error consultando categorias: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    // =============== RECURSOS ===============
    @Override
    public void create(Recurso e) throws Exception {
        recursoDao.create(e);
    }

    public Recurso read(Recurso e) throws Exception {
        return recursoDao.read(e);
    }

    @Override
    public void update(Recurso e) throws Exception {
        recursoDao.update(e);
    }

    @Override
    public void delete(Recurso e) throws Exception {
        recursoDao.delete(e);
    }

    @Override
    public List<Recurso> search(CategoriaRecurso categoria, String descripcion) {
        try {
            return recursoDao.search(categoria, descripcion);
        } catch (Exception ex) {
            System.err.println("Error buscando recursos: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    // =============== RESERVAS ===============
    @Override
    public List<Reserva> findReservas(Funcionario funcionario) {
        try {
            return reservaDao.findByFuncionario(funcionario);
        } catch (Exception ex) {
            System.err.println("Error consultando reservas: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Reserva crearReserva(Funcionario funcionario, String actividad, LocalDate fecha,
                                LocalTime horaInicio, LocalTime horaFin,
                                List<CategoriaRecurso> categoriasSolicitadas) throws Exception {

        // Reglas que no necesitan consultar datos. Viven en ReglasReserva para
        // poder probarlas sin base de datos.
        ReglasReserva.validarSolicitud(categoriasSolicitadas, fecha, horaInicio, horaFin,
                LocalDate.now(), LocalTime.now());

        // Comprobar disponibilidad y guardar tienen que ser indivisibles: si se
        // separan, dos Frontend podrian quedarse con el mismo recurso.
        synchronized (reservaDao.lock()) {
            List<Recurso> asignados = new ArrayList<>();
            List<CategoriaRecurso> noDisponibles = new ArrayList<>();

            for (CategoriaRecurso cat : categoriasSolicitadas) {
                Recurso libre = reservaDao.buscarRecursoLibre(cat, fecha, horaInicio, horaFin, null);
                if (libre == null || yaAsignado(asignados, libre)) {
                    noDisponibles.add(cat);
                } else {
                    asignados.add(libre);
                }
            }

            if (!noDisponibles.isEmpty()) {
                String nombres = noDisponibles.stream()
                        .map(CategoriaRecurso::getDescripcion)
                        .collect(Collectors.joining(", "));
                throw new Exception("No hay disponibilidad para: " + nombres);
            }

            Reserva reserva = new Reserva(reservaDao.nextId(), funcionario, actividad,
                    fecha, horaInicio, horaFin);
            reserva.getRecursos().addAll(asignados);
            reservaDao.create(reserva);
            return reserva;
        }
    }

    /**
     * Evita asignar dos veces el mismo recurso a una reserva. La version con
     * XML lo permitia en silencio; en MySQL la llave primaria compuesta de
     * reserva_recurso lo rechazaria con un error poco claro.
     */
    private boolean yaAsignado(List<Recurso> asignados, Recurso candidato) {
        return asignados.stream().anyMatch(r -> r.getId().equals(candidato.getId()));
    }

    @Override
    public void cancelarReserva(Reserva e) throws Exception {
        Reserva result = reservaDao.read(e.getId()); // lanza "Reserva no existe"
        if (ReglasReserva.esPasado(result.getFecha(), result.getHoraInicio(),
                LocalDate.now(), LocalTime.now()))
            throw new Exception("No se puede cancelar una reserva pasada o que ya comenzó");
        reservaDao.cancelar(result);
    }

    // =============== CALENDARIZACION / ACTIVIDADES ===============
    @Override
    public List<Reserva> findReservasActivasPorFecha(LocalDate fecha) {
        try {
            return reservaDao.findActivasPorFecha(fecha);
        } catch (Exception ex) {
            System.err.println("Error consultando la calendarizacion: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Reserva> findReservasActivasPorSemana(LocalDate inicioSemana, LocalDate finSemana) {
        try {
            return reservaDao.findActivasEntre(inicioSemana, finSemana);
        } catch (Exception ex) {
            System.err.println("Error consultando las actividades: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    // =============== ESTADISTICAS ===============
    @Override
    public Map<String, Long> estadisticasRecursosPorCategoria(LocalDate desde, LocalDate hasta) {
        List<Reserva> reservas = findReservasActivasPorSemana(desde, hasta);
        return reservas.stream()
                .flatMap(r -> r.getRecursos().stream())
                .collect(Collectors.groupingBy(rec -> rec.getCategoria().getDescripcion(),
                        TreeMap::new, Collectors.counting()));
    }

    @Override
    public Map<LocalDate, Long> estadisticasActividadesPorSemana(LocalDate desde, LocalDate hasta) {
        List<Reserva> reservas = findReservasActivasPorSemana(desde, hasta);
        return reservas.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getFecha().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                        TreeMap::new, Collectors.counting()));
    }

    // =============== INTELIGENCIA ARTIFICIAL ===============
    @Override
    public ReservaExtraccion extraerReserva(String frase) {
        dev.langchain4j.model.openai.OpenAiChatModel aiModel = dev.langchain4j.model.openai.OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1") // LangChain4j free proxy
                .apiKey("demo")                                    // Free demo key
                .modelName("gpt-4o-mini")                          // Restricted model
                .build();
        ReservaExtractorService aiService = dev.langchain4j.service.AiServices.create(
                ReservaExtractorService.class, aiModel);

        String listaCategorias = findAllCategorias().stream()
                .map(CategoriaRecurso::getDescripcion)
                .collect(Collectors.joining(", "));

        return aiService.extraer(frase, listaCategorias, LocalDate.now().toString());
    }
}
