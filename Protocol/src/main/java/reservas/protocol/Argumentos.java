package reservas.protocol;

import java.io.Serializable;

/** Tipos cerrados admitidos en solicitudes. */
public sealed interface Argumentos extends Serializable {
    record Login(String id, String clave) implements Argumentos {
        private static final long serialVersionUID = 1L;
        @Override public String toString() { return "Login[credenciales omitidas]"; }
    }
    record CambioClave(String id, String claveActual, String claveNueva) implements Argumentos {
        private static final long serialVersionUID = 1L;
        @Override public String toString() { return "CambioClave[credenciales omitidas]"; }
    }
    record Funcionario(FuncionarioDto funcionario) implements Argumentos {
        private static final long serialVersionUID = 1L;
    }
    record Categoria(CategoriaDto categoria) implements Argumentos {
        private static final long serialVersionUID = 1L;
    }
    record Recurso(RecursoDto recurso) implements Argumentos {
        private static final long serialVersionUID = 1L;
    }
    record BuscarRecursos(CategoriaDto categoria, String descripcion) implements Argumentos {
        private static final long serialVersionUID = 1L;
    }
    record CrearReserva(FuncionarioDto funcionario, String actividad, java.time.LocalDate fecha, java.time.LocalTime horaInicio, java.time.LocalTime horaFin, CategoriaDto[] categoriasSolicitadas) implements Argumentos {
        private static final long serialVersionUID = 1L;
        public CrearReserva { categoriasSolicitadas = java.util.Objects.requireNonNull(categoriasSolicitadas).clone();
            for (CategoriaDto item : categoriasSolicitadas) java.util.Objects.requireNonNull(item);
        }
        @Override public CategoriaDto[] categoriasSolicitadas() { return categoriasSolicitadas.clone(); }
    }
    record Reserva(ReservaDto reserva) implements Argumentos {
        private static final long serialVersionUID = 1L;
    }
    record Fecha(java.time.LocalDate fecha) implements Argumentos {
        private static final long serialVersionUID = 1L;
    }
    record Periodo(java.time.LocalDate desde, java.time.LocalDate hasta) implements Argumentos {
        private static final long serialVersionUID = 1L;
    }
    record ExtraerReserva(String frase) implements Argumentos {
        private static final long serialVersionUID = 1L;
    }
}
