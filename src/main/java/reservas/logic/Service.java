package reservas.logic;

import reservas.data.Data;
import reservas.data.XmlPersister;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Service {
    private static Service theInstance;

    public static Service instance() {
        if (theInstance == null) theInstance = new Service();
        return theInstance;
    }

    private Data data;
    private String path;

    private Service() {
        this("data.xml");
    }

    private Service(String path) {
        this.path = path;
        try {
            data = new XmlPersister(path).load();
        } catch (Exception e) {
            data = new Data();
            seedAdmin();
        }
    }

    private void seedAdmin() {
        data.getUsuarios().add(new Usuario("admin", "admin", Usuario.ADMIN));
    }

    public void stop() {
        try {
            new XmlPersister(path).store(data);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    static void resetForTesting() {
        theInstance = new Service();
        theInstance.data = new Data();
    }

    static void resetForTesting(String path) {
        theInstance = new Service(path);
    }

    // =============== LOGIN ===============
    public Usuario login(String id, String clave) throws Exception {
        Usuario result = data.getUsuarios().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (result == null) throw new Exception("Usuario no existe");
        if (!result.getClave().equals(clave)) throw new Exception("Clave incorrecta");
        return result;
    }

    public void changePassword(String id, String claveActual, String claveNueva) throws Exception {
        Usuario u = data.getUsuarios().stream()
                .filter(i -> i.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (u == null) throw new Exception("Usuario no existe");
        if (!u.getClave().equals(claveActual)) throw new Exception("Clave actual incorrecta");
        u.setClave(claveNueva);
    }

    // =============== FUNCIONARIOS ===============
    public void create(Funcionario e) throws Exception {
        Usuario result = data.getUsuarios().stream()
                .filter(i -> i.getId().equals(e.getId()))
                .findFirst()
                .orElse(null);
        if (result == null) {
            e.setClave(e.getId());
            data.getUsuarios().add(e);
        } else {
            throw new Exception("Funcionario ya existe");
        }
    }

    public Funcionario read(Funcionario e) throws Exception {
        Funcionario result = findFuncionario(e.getId());
        if (result != null) return result;
        throw new Exception("Funcionario no existe");
    }

    public void update(Funcionario e) throws Exception {
        Funcionario result = findFuncionario(e.getId());
        if (result == null) throw new Exception("Funcionario no existe");
        result.setNombre(e.getNombre());
        result.setTelefono(e.getTelefono());
    }

    public void delete(Funcionario e) throws Exception {
        Funcionario result = findFuncionario(e.getId());
        if (result == null) throw new Exception("Funcionario no existe");
        data.getUsuarios().remove(result);
    }

    public List<Funcionario> search(Funcionario e) {
        return data.getUsuarios().stream()
                .filter(u -> u instanceof Funcionario)
                .map(u -> (Funcionario) u)
                .filter(f -> e.getId().isEmpty() || f.getId().toLowerCase().contains(e.getId().toLowerCase()))
                .filter(f -> e.getNombre().isEmpty() || f.getNombre().toLowerCase().contains(e.getNombre().toLowerCase()))
                .sorted(Comparator.comparing(Funcionario::getNombre))
                .collect(Collectors.toList());
    }

    private Funcionario findFuncionario(String id) {
        return data.getUsuarios().stream()
                .filter(u -> u instanceof Funcionario)
                .map(u -> (Funcionario) u)
                .filter(f -> f.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // =============== CATEGORIAS DE RECURSO ===============
    public void create(CategoriaRecurso e) throws Exception {
        e.setId(nextCategoriaId());
        data.getCategorias().add(e);
    }

    public CategoriaRecurso read(CategoriaRecurso e) throws Exception {
        CategoriaRecurso result = data.getCategorias().stream()
                .filter(i -> i.getId().equals(e.getId()))
                .findFirst()
                .orElse(null);
        if (result != null) return result;
        throw new Exception("Categoria no existe");
    }

    public void update(CategoriaRecurso e) throws Exception {
        CategoriaRecurso result = read(e);
        result.setDescripcion(e.getDescripcion());
    }

    public void delete(CategoriaRecurso e) throws Exception {
        CategoriaRecurso result = read(e);
        data.getCategorias().remove(result);
    }

    public List<CategoriaRecurso> search(CategoriaRecurso e) {
        return data.getCategorias().stream()
                .filter(i -> i.getDescripcion().toLowerCase().contains(e.getDescripcion().toLowerCase()))
                .sorted(Comparator.comparing(CategoriaRecurso::getDescripcion))
                .collect(Collectors.toList());
    }

    public List<CategoriaRecurso> findAllCategorias() {
        return data.getCategorias();
    }

    private String nextCategoriaId() {
        int max = data.getCategorias().stream()
                .map(c -> c.getId().replace("CAT-", ""))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return String.format("CAT-%06d", max + 1);
    }

    // =============== RECURSOS ===============
    public void create(Recurso e) throws Exception {
        Recurso result = data.getRecursos().stream()
                .filter(i -> i.getId().equals(e.getId()))
                .findFirst()
                .orElse(null);
        if (result == null) {
            data.getRecursos().add(e);
        } else {
            throw new Exception("Recurso ya existe");
        }
    }

    public Recurso read(Recurso e) throws Exception {
        Recurso result = data.getRecursos().stream()
                .filter(i -> i.getId().equals(e.getId()))
                .findFirst()
                .orElse(null);
        if (result != null) return result;
        throw new Exception("Recurso no existe");
    }

    public void update(Recurso e) throws Exception {
        Recurso result = read(e);
        result.setCategoria(e.getCategoria());
        result.setDescripcion(e.getDescripcion());
    }

    public void delete(Recurso e) throws Exception {
        Recurso result = read(e);
        data.getRecursos().remove(result);
    }

    public List<Recurso> search(CategoriaRecurso categoria, String descripcion) {
        return data.getRecursos().stream()
                .filter(r -> categoria == null || categoria.getId() == null || categoria.getId().isEmpty()
                        || r.getCategoria().getId().equals(categoria.getId()))
                .filter(r -> descripcion == null || r.getDescripcion().toLowerCase().contains(descripcion.toLowerCase()))
                .sorted(Comparator.comparing(Recurso::getDescripcion))
                .collect(Collectors.toList());
    }

    // =============== RESERVAS ===============
    public List<Reserva> findReservas(Funcionario funcionario) {
        return data.getReservas().stream()
                .filter(r -> r.getFuncionario().getId().equals(funcionario.getId()))
                .sorted(Comparator.comparing(Reserva::getFecha).thenComparing(Reserva::getHoraInicio))
                .collect(Collectors.toList());
    }

    public Reserva crearReserva(Funcionario funcionario, String actividad, LocalDate fecha,
                                LocalTime horaInicio, LocalTime horaFin,
                                List<CategoriaRecurso> categoriasSolicitadas) throws Exception {

        if (categoriasSolicitadas.isEmpty()) throw new Exception("Debe seleccionar al menos una categoria");
        if (horaFin.isBefore(horaInicio) || horaFin.equals(horaInicio))
            throw new Exception("La hora de fin debe ser posterior a la hora de inicio");

        List<CategoriaRecurso> noDisponibles = categoriasSolicitadas.stream()
                .filter(cat -> buscarRecursoLibre(cat, fecha, horaInicio, horaFin, null) == null)
                .collect(Collectors.toList());

        if (!noDisponibles.isEmpty()) {
            String nombres = noDisponibles.stream()
                    .map(CategoriaRecurso::getDescripcion)
                    .collect(Collectors.joining(", "));
            throw new Exception("No hay disponibilidad para: " + nombres);
        }

        Reserva reserva = new Reserva(nextReservaId(), funcionario, actividad, fecha, horaInicio, horaFin);
        for (CategoriaRecurso cat : categoriasSolicitadas) {
            reserva.getRecursos().add(buscarRecursoLibre(cat, fecha, horaInicio, horaFin, null));
        }
        data.getReservas().add(reserva);
        return reserva;
    }

    public void cancelarReserva(Reserva e) throws Exception {
        Reserva result = data.getReservas().stream()
                .filter(i -> i.getId().equals(e.getId()))
                .findFirst()
                .orElse(null);
        if (result == null) throw new Exception("Reserva no existe");
        if (result.getFecha().isBefore(LocalDate.now()))
            throw new Exception("No se puede cancelar una reserva pasada");
        result.setEstado(Reserva.CANCELADA);
    }

    private Recurso buscarRecursoLibre(CategoriaRecurso categoria, LocalDate fecha,
                                       LocalTime horaInicio, LocalTime horaFin, String reservaIdExcluida) {
        List<Recurso> recursosDeCategoria = data.getRecursos().stream()
                .filter(r -> r.getCategoria().getId().equals(categoria.getId()))
                .collect(Collectors.toList());

        for (Recurso r : recursosDeCategoria) {
            boolean ocupado = data.getReservas().stream()
                    .filter(res -> res.getEstado().equals(Reserva.ACTIVA))
                    .filter(res -> !res.getId().equals(reservaIdExcluida))
                    .filter(res -> res.getFecha().equals(fecha))
                    .filter(res -> res.getRecursos().stream().anyMatch(rec -> rec.getId().equals(r.getId())))
                    .anyMatch(res -> horaInicio.isBefore(res.getHoraFin()) && horaFin.isAfter(res.getHoraInicio()));
            if (!ocupado) return r;
        }
        return null;
    }

    private String nextReservaId() {
        int max = data.getReservas().stream()
                .map(r -> r.getId().replace("RES-", ""))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return String.format("RES-%06d", max + 1);
    }

    // =============== CALENDARIZACION / ACTIVIDADES ===============
    public List<Reserva> findReservasActivasPorFecha(LocalDate fecha) {
        return data.getReservas().stream()
                .filter(r -> r.getEstado().equals(Reserva.ACTIVA))
                .filter(r -> r.getFecha().equals(fecha))
                .collect(Collectors.toList());
    }

    public List<Reserva> findReservasActivasPorSemana(LocalDate inicioSemana, LocalDate finSemana) {
        return data.getReservas().stream()
                .filter(r -> r.getEstado().equals(Reserva.ACTIVA))
                .filter(r -> !r.getFecha().isBefore(inicioSemana) && !r.getFecha().isAfter(finSemana))
                .collect(Collectors.toList());
    }

    // =============== ESTADISTICAS ===============
    public Map<String, Long> estadisticasRecursosPorCategoria(LocalDate desde, LocalDate hasta) {
        List<Reserva> reservas = findReservasActivasPorSemana(desde, hasta);
        return reservas.stream()
                .flatMap(r -> r.getRecursos().stream())
                .collect(Collectors.groupingBy(rec -> rec.getCategoria().getDescripcion(),
                        TreeMap::new, Collectors.counting()));
    }

    public Map<LocalDate, Long> estadisticasActividadesPorSemana(LocalDate desde, LocalDate hasta) {
        List<Reserva> reservas = findReservasActivasPorSemana(desde, hasta);
        return reservas.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getFecha().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                        TreeMap::new, Collectors.counting()));
    }
}