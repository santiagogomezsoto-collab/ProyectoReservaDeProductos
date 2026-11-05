package reservas.protocol;

import java.io.Serializable;

/** Tipos cerrados admitidos en respuestas. */
public sealed interface Resultado extends Serializable {
    record Confirmacion() implements Resultado {
        private static final long serialVersionUID = 1L;
    }
    record Usuario(UsuarioDto usuario) implements Resultado {
        private static final long serialVersionUID = 1L;
    }
    record Funcionarios(FuncionarioDto[] funcionarios) implements Resultado {
        private static final long serialVersionUID = 1L;
        public Funcionarios { funcionarios = java.util.Objects.requireNonNull(funcionarios).clone();
            for (FuncionarioDto item : funcionarios) java.util.Objects.requireNonNull(item);
        }
        @Override public FuncionarioDto[] funcionarios() { return funcionarios.clone(); }
    }
    record Categorias(CategoriaDto[] categorias) implements Resultado {
        private static final long serialVersionUID = 1L;
        public Categorias { categorias = java.util.Objects.requireNonNull(categorias).clone();
            for (CategoriaDto item : categorias) java.util.Objects.requireNonNull(item);
        }
        @Override public CategoriaDto[] categorias() { return categorias.clone(); }
    }
    record Recursos(RecursoDto[] recursos) implements Resultado {
        private static final long serialVersionUID = 1L;
        public Recursos { recursos = java.util.Objects.requireNonNull(recursos).clone();
            for (RecursoDto item : recursos) java.util.Objects.requireNonNull(item);
        }
        @Override public RecursoDto[] recursos() { return recursos.clone(); }
    }
    record Reserva(ReservaDto reserva) implements Resultado {
        private static final long serialVersionUID = 1L;
    }
    record Reservas(ReservaDto[] reservas) implements Resultado {
        private static final long serialVersionUID = 1L;
        public Reservas { reservas = java.util.Objects.requireNonNull(reservas).clone();
            for (ReservaDto item : reservas) java.util.Objects.requireNonNull(item);
        }
        @Override public ReservaDto[] reservas() { return reservas.clone(); }
    }
    record EstadisticasCategorias(CantidadCategoria[] cantidades) implements Resultado {
        private static final long serialVersionUID = 1L;
        public EstadisticasCategorias { cantidades = java.util.Objects.requireNonNull(cantidades).clone();
            for (CantidadCategoria item : cantidades) java.util.Objects.requireNonNull(item);
        }
        @Override public CantidadCategoria[] cantidades() { return cantidades.clone(); }
    }
    record EstadisticasSemanas(CantidadSemana[] cantidades) implements Resultado {
        private static final long serialVersionUID = 1L;
        public EstadisticasSemanas { cantidades = java.util.Objects.requireNonNull(cantidades).clone();
            for (CantidadSemana item : cantidades) java.util.Objects.requireNonNull(item);
        }
        @Override public CantidadSemana[] cantidades() { return cantidades.clone(); }
    }
    record Extraccion(ReservaExtraccionDto extraccion) implements Resultado {
        private static final long serialVersionUID = 1L;
    }
}
