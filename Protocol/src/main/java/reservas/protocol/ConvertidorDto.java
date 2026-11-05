package reservas.protocol;

import reservas.logic.*;
import reservas.ia.ReservaExtraccion;
import java.util.*;
import java.time.LocalDate;

/** Copias de datos; nunca transporta claves ni accede a servicios o persistencia. */
public final class ConvertidorDto {
    private ConvertidorDto() { }

    public static UsuarioDto usuario(Usuario e) {
        return e == null ? null : new UsuarioDto(e.getId(), e.getRol(),
                e instanceof Funcionario f ? funcionario(f) : null);
    }

    public static Usuario usuario(UsuarioDto d) {
        if (d == null) return null;
        return d.funcionario() == null ? new Usuario(d.id(), "", d.rol()) : funcionario(d.funcionario());
    }

    public static FuncionarioDto funcionario(Funcionario e) {
        return e == null ? null : new FuncionarioDto(e.getId(), e.getRol(), e.getNombre(), e.getTelefono());
    }

    public static Funcionario funcionario(FuncionarioDto d) {
        if (d == null) return null;
        Funcionario e = new Funcionario(d.id(), "", d.nombre(), d.telefono());
        e.setRol(d.rol());
        return e;
    }

    public static CategoriaDto categoria(CategoriaRecurso e) {
        return e == null ? null : new CategoriaDto(e.getId(), e.getDescripcion());
    }

    public static CategoriaRecurso categoria(CategoriaDto d) {
        return d == null ? null : new CategoriaRecurso(d.id(), d.descripcion());
    }

    public static RecursoDto recurso(Recurso e) {
        return e == null ? null : new RecursoDto(e.getId(), categoria(e.getCategoria()), e.getDescripcion());
    }

    public static Recurso recurso(RecursoDto d) {
        return d == null ? null : new Recurso(d.id(), categoria(d.categoria()), d.descripcion());
    }

    public static ReservaDto reserva(Reserva e) {
        return e == null ? null : new ReservaDto(e.getId(), funcionario(e.getFuncionario()),
                e.getActividad(), e.getFecha(), e.getHoraInicio(), e.getHoraFin(),
                e.getRecursos() == null ? null : e.getRecursos().stream().map(ConvertidorDto::recurso).toArray(RecursoDto[]::new),
                e.getEstado());
    }

    public static Reserva reserva(ReservaDto d) {
        if (d == null) return null;
        Reserva e = new Reserva(d.id(), funcionario(d.funcionario()), d.actividad(),
                d.fecha(), d.horaInicio(), d.horaFin());
        e.setEstado(d.estado());
        e.setRecursos(d.recursos() == null ? null : new ArrayList<>(
                Arrays.stream(d.recursos()).map(ConvertidorDto::recurso).toList()));
        return e;
    }

    public static ReservaExtraccionDto extraccion(ReservaExtraccion e) {
        return e == null ? null : new ReservaExtraccionDto(e.getActividad(), e.getFecha(),
                e.getHoraInicio(), e.getHoraFinal(),
                e.getCategoriasRecurso() == null ? null : e.getCategoriasRecurso().toArray(String[]::new));
    }

    public static ReservaExtraccion extraccion(ReservaExtraccionDto d) {
        return d == null ? null : new ReservaExtraccion(d.actividad(), d.fecha(), d.horaInicio(),
                d.horaFinal(), d.categoriasRecurso() == null ? null :
                new ArrayList<>(Arrays.asList(d.categoriasRecurso())));
    }

    public static Resultado.EstadisticasCategorias categorias(Map<String, Long> datos) {
        return new Resultado.EstadisticasCategorias(datos.entrySet().stream()
                .map(e -> new CantidadCategoria(e.getKey(), e.getValue())).toArray(CantidadCategoria[]::new));
    }

    public static Map<String, Long> categorias(Resultado.EstadisticasCategorias datos) {
        Map<String, Long> resultado = new TreeMap<>();
        for (CantidadCategoria c : datos.cantidades()) {
            if (resultado.putIfAbsent(c.categoria(), c.cantidad()) != null)
                throw new IllegalArgumentException("Categoria duplicada");
        }
        return resultado;
    }

    public static Resultado.EstadisticasSemanas semanas(Map<LocalDate, Long> datos) {
        return new Resultado.EstadisticasSemanas(datos.entrySet().stream()
                .map(e -> new CantidadSemana(e.getKey(), e.getValue())).toArray(CantidadSemana[]::new));
    }

    public static Map<LocalDate, Long> semanas(Resultado.EstadisticasSemanas datos) {
        Map<LocalDate, Long> resultado = new TreeMap<>();
        for (CantidadSemana c : datos.cantidades()) {
            if (resultado.putIfAbsent(c.semana(), c.cantidad()) != null)
                throw new IllegalArgumentException("Semana duplicada");
        }
        return resultado;
    }
}
