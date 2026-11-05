package reservas.protocol;

public final class Operaciones {
    public static final int FIND_ALL_CATEGORIAS = 1;
    public static final int LOGIN = 2;
    public static final int CHANGE_PASSWORD = 3;
    public static final int CREATE_FUNCIONARIO = 4;
    public static final int UPDATE_FUNCIONARIO = 5;
    public static final int DELETE_FUNCIONARIO = 6;
    public static final int SEARCH_FUNCIONARIO = 7;
    public static final int CREATE_CATEGORIA = 8;
    public static final int UPDATE_CATEGORIA = 9;
    public static final int DELETE_CATEGORIA = 10;
    public static final int SEARCH_CATEGORIA = 11;
    public static final int CREATE_RECURSO = 12;
    public static final int UPDATE_RECURSO = 13;
    public static final int DELETE_RECURSO = 14;
    public static final int SEARCH_RECURSOS = 15;
    public static final int FIND_RESERVAS = 16;
    public static final int CREAR_RESERVA = 17;
    public static final int CANCELAR_RESERVA = 18;
    public static final int FIND_RESERVAS_ACTIVAS_POR_FECHA = 19;
    public static final int FIND_RESERVAS_ACTIVAS_POR_SEMANA = 20;
    public static final int ESTADISTICAS_RECURSOS_POR_CATEGORIA = 21;
    public static final int ESTADISTICAS_ACTIVIDADES_POR_SEMANA = 22;
    public static final int EXTRAER_RESERVA = 23;

    private Operaciones() { }

    public static void validarArgumentos(int operacion, Argumentos argumentos) {
        Class<? extends Argumentos> tipo = switch (operacion) {
            case FIND_ALL_CATEGORIAS -> null;
            case LOGIN -> Argumentos.Login.class;
            case CHANGE_PASSWORD -> Argumentos.CambioClave.class;
            case CREATE_FUNCIONARIO -> Argumentos.Funcionario.class;
            case UPDATE_FUNCIONARIO -> Argumentos.Funcionario.class;
            case DELETE_FUNCIONARIO -> Argumentos.Funcionario.class;
            case SEARCH_FUNCIONARIO -> Argumentos.Funcionario.class;
            case CREATE_CATEGORIA -> Argumentos.Categoria.class;
            case UPDATE_CATEGORIA -> Argumentos.Categoria.class;
            case DELETE_CATEGORIA -> Argumentos.Categoria.class;
            case SEARCH_CATEGORIA -> Argumentos.Categoria.class;
            case CREATE_RECURSO -> Argumentos.Recurso.class;
            case UPDATE_RECURSO -> Argumentos.Recurso.class;
            case DELETE_RECURSO -> Argumentos.Recurso.class;
            case SEARCH_RECURSOS -> Argumentos.BuscarRecursos.class;
            case FIND_RESERVAS -> Argumentos.Funcionario.class;
            case CREAR_RESERVA -> Argumentos.CrearReserva.class;
            case CANCELAR_RESERVA -> Argumentos.Reserva.class;
            case FIND_RESERVAS_ACTIVAS_POR_FECHA -> Argumentos.Fecha.class;
            case FIND_RESERVAS_ACTIVAS_POR_SEMANA -> Argumentos.Periodo.class;
            case ESTADISTICAS_RECURSOS_POR_CATEGORIA -> Argumentos.Periodo.class;
            case ESTADISTICAS_ACTIVIDADES_POR_SEMANA -> Argumentos.Periodo.class;
            case EXTRAER_RESERVA -> Argumentos.ExtraerReserva.class;
            default -> throw new IllegalArgumentException("Operacion desconocida: " + operacion);
        };
        if (tipo == null ? argumentos != null : !tipo.isInstance(argumentos))
            throw new IllegalArgumentException("Argumentos incompatible con operacion " + operacion);
    }

    public static void validarResultado(int operacion, Resultado resultado) {
        Class<? extends Resultado> tipo = switch (operacion) {
            case FIND_ALL_CATEGORIAS -> Resultado.Categorias.class;
            case LOGIN -> Resultado.Usuario.class;
            case CHANGE_PASSWORD -> Resultado.Confirmacion.class;
            case CREATE_FUNCIONARIO -> Resultado.Confirmacion.class;
            case UPDATE_FUNCIONARIO -> Resultado.Confirmacion.class;
            case DELETE_FUNCIONARIO -> Resultado.Confirmacion.class;
            case SEARCH_FUNCIONARIO -> Resultado.Funcionarios.class;
            case CREATE_CATEGORIA -> Resultado.Confirmacion.class;
            case UPDATE_CATEGORIA -> Resultado.Confirmacion.class;
            case DELETE_CATEGORIA -> Resultado.Confirmacion.class;
            case SEARCH_CATEGORIA -> Resultado.Categorias.class;
            case CREATE_RECURSO -> Resultado.Confirmacion.class;
            case UPDATE_RECURSO -> Resultado.Confirmacion.class;
            case DELETE_RECURSO -> Resultado.Confirmacion.class;
            case SEARCH_RECURSOS -> Resultado.Recursos.class;
            case FIND_RESERVAS -> Resultado.Reservas.class;
            case CREAR_RESERVA -> Resultado.Reserva.class;
            case CANCELAR_RESERVA -> Resultado.Confirmacion.class;
            case FIND_RESERVAS_ACTIVAS_POR_FECHA -> Resultado.Reservas.class;
            case FIND_RESERVAS_ACTIVAS_POR_SEMANA -> Resultado.Reservas.class;
            case ESTADISTICAS_RECURSOS_POR_CATEGORIA -> Resultado.EstadisticasCategorias.class;
            case ESTADISTICAS_ACTIVIDADES_POR_SEMANA -> Resultado.EstadisticasSemanas.class;
            case EXTRAER_RESERVA -> Resultado.Extraccion.class;
            default -> throw new IllegalArgumentException("Operacion desconocida: " + operacion);
        };
        if (tipo == null ? resultado != null : !tipo.isInstance(resultado))
            throw new IllegalArgumentException("Resultado incompatible con operacion " + operacion);
    }
}
