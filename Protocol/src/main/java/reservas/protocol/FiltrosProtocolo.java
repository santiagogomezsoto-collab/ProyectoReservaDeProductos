package reservas.protocol;

import java.io.ObjectInputFilter;
import java.util.HashSet;
import java.util.Set;

/** Listas cerradas por direccion. El prototipo conserva sus filtros mas reducidos. */
public final class FiltrosProtocolo {
    private FiltrosProtocolo() { }

    public static ObjectInputFilter solicitudes() {
        Set<String> tipos = datos();
        tipos.add(Solicitud.class.getName());
        for (Class<?> c : Argumentos.class.getPermittedSubclasses()) tipos.add(c.getName());
        return filtro(tipos);
    }

    public static ObjectInputFilter respuestas() {
        Set<String> tipos = datos();
        tipos.add(Respuesta.class.getName());
        tipos.add(CodigoError.class.getName());
        for (Class<?> c : Resultado.class.getPermittedSubclasses()) tipos.add(c.getName());
        return filtro(tipos);
    }

    public static ObjectInputFilter identificacion() {
        return filtro(Set.of(Identificacion.class.getName(), SesionCreada.class.getName(),
                TipoConexion.class.getName(), Enum.class.getName(), String.class.getName()));
    }

    /** Acuse de apertura o rechazo controlado; no admite resultados de negocio. */
    public static ObjectInputFilter apertura() {
        return filtro(Set.of(SesionCreada.class.getName(), Respuesta.class.getName(),
                CodigoError.class.getName(), Enum.class.getName(), String.class.getName()));
    }

    private static Set<String> datos() {
        return new HashSet<>(Set.of(UsuarioDto.class.getName(), FuncionarioDto.class.getName(),
                CategoriaDto.class.getName(), RecursoDto.class.getName(), ReservaDto.class.getName(),
                ReservaExtraccionDto.class.getName(), CantidadCategoria.class.getName(),
                CantidadSemana.class.getName(), String.class.getName(), Enum.class.getName(),
                "java.time.Ser", "java.time.LocalDate", "java.time.LocalTime"));
    }

    private static ObjectInputFilter filtro(Set<String> permitidos) {
        return info -> {
            if (info.depth() > 24 || info.references() > 100000 || info.streamBytes() > 4194304
                    || info.arrayLength() > 10000) return ObjectInputFilter.Status.REJECTED;
            Class<?> tipo = info.serialClass();
            if (tipo == null) return ObjectInputFilter.Status.UNDECIDED;
            while (tipo.isArray()) tipo = tipo.getComponentType();
            return permitidos.contains(tipo.getName()) ? ObjectInputFilter.Status.ALLOWED
                    : ObjectInputFilter.Status.REJECTED;
        };
    }
}
