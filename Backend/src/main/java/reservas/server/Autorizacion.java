package reservas.server;

import reservas.logic.*;
import reservas.protocol.*;
import static reservas.protocol.Operaciones.*;

final class Autorizacion {
    private Autorizacion() { }

    static void comprobar(SesionServidor sesion, Solicitud solicitud) throws Rechazo {
        if (!sesion.vigente()) throw new Rechazo(CodigoError.SESION_INVALIDA, "Sesion cerrada");
        int op = solicitud.operacion();
        Usuario usuario = sesion.usuario();
        if (op == LOGIN || op == FIND_ALL_CATEGORIAS) return;
        if (op == CHANGE_PASSWORD) {
            Argumentos.CambioClave a = (Argumentos.CambioClave) solicitud.argumentos();
            if (usuario != null && !usuario.getId().equals(a.id())) denegar();
            return; // Service siempre verifica la clave actual, incluso antes del login.
        }
        if (usuario == null) throw new Rechazo(CodigoError.AUTENTICACION, "Debe iniciar sesion");
        boolean admin = Usuario.ADMIN.equals(usuario.getRol());
        boolean funcionario = Usuario.FUNCIONARIO.equals(usuario.getRol()) && usuario instanceof Funcionario;
        switch (op) {
            case CREATE_FUNCIONARIO, UPDATE_FUNCIONARIO, DELETE_FUNCIONARIO, SEARCH_FUNCIONARIO,
                 CREATE_CATEGORIA, UPDATE_CATEGORIA, DELETE_CATEGORIA,
                 CREATE_RECURSO, UPDATE_RECURSO, DELETE_RECURSO -> { if (!admin) denegar(); }
            case SEARCH_CATEGORIA, SEARCH_RECURSOS, FIND_RESERVAS_ACTIVAS_POR_FECHA,
                 FIND_RESERVAS_ACTIVAS_POR_SEMANA, ESTADISTICAS_RECURSOS_POR_CATEGORIA,
                 ESTADISTICAS_ACTIVIDADES_POR_SEMANA -> { if (!admin && !funcionario) denegar(); }
            case FIND_RESERVAS -> {
                if (!funcionario) denegar();
                propio(usuario, ((Argumentos.Funcionario) solicitud.argumentos()).funcionario());
            }
            case CREAR_RESERVA -> {
                if (!funcionario) denegar();
                propio(usuario, ((Argumentos.CrearReserva) solicitud.argumentos()).funcionario());
            }
            case CANCELAR_RESERVA -> {
                if (!funcionario) denegar();
                ReservaDto reserva = ((Argumentos.Reserva) solicitud.argumentos()).reserva();
                propio(usuario, reserva == null ? null : reserva.funcionario());
            }
            case EXTRAER_RESERVA -> { if (!funcionario) denegar(); }
            default -> throw new Rechazo(CodigoError.OPERACION_NO_SOPORTADA, "Operacion no soportada");
        }
    }

    private static void propio(Usuario usuario, FuncionarioDto dto) throws Rechazo {
        if (dto == null || !usuario.getId().equals(dto.id())) denegar();
    }
    private static void denegar() throws Rechazo {
        throw new Rechazo(CodigoError.NO_AUTORIZADO, "Operacion no autorizada");
    }
}
