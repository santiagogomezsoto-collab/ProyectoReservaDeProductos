package reservas.server;

import reservas.logic.*;
import reservas.protocol.*;
import java.sql.SQLException;
import java.util.Arrays;
import static reservas.protocol.Operaciones.*;

/** Coordina llamadas completas, no solo sentencias, sobre la conexion compartida. */
final class Despachador {
    private final IService service;
    private final Object bloqueo;

    Despachador(IService service, Object bloqueo) { this.service = service; this.bloqueo = bloqueo; }

    Respuesta atender(SesionServidor sesion, Solicitud solicitud) {
        synchronized (bloqueo) {
            try {
                Autorizacion.comprobar(sesion, solicitud);
                return Respuesta.exito(solicitud.operacion(), ejecutar(sesion, solicitud));
            } catch (Rechazo e) {
                return Respuesta.fallo(e.codigo, e.getMessage());
            } catch (Exception e) {
                if (esInterno(e)) return Respuesta.fallo(CodigoError.INTERNO, "No se pudo completar la operacion");
                if (solicitud.operacion() == LOGIN || solicitud.operacion() == CHANGE_PASSWORD)
                    return Respuesta.fallo(CodigoError.AUTENTICACION, "No se pudieron validar las credenciales");
                return Respuesta.fallo(CodigoError.NEGOCIO, "La operacion fue rechazada por el servicio");
            }
        }
    }

    private static boolean esInterno(Exception e) {
        if (e instanceof RuntimeException) return true;
        for (Throwable causa = e; causa != null; causa = causa.getCause())
            if (causa instanceof SQLException) return true;
        return false;
    }

    private Resultado ejecutar(SesionServidor sesion, Solicitud s) throws Exception {
        Argumentos a = s.argumentos();
        switch (s.operacion()) {
            case FIND_ALL_CATEGORIAS:
                return new Resultado.Categorias(service.findAllCategorias().stream()
                        .map(ConvertidorDto::categoria).toArray(CategoriaDto[]::new));
            case LOGIN:
                return new Resultado.Usuario(ConvertidorDto.usuario(
                        Autenticacion.login(service, sesion, (Argumentos.Login) a)));
            case CHANGE_PASSWORD: {
                Argumentos.CambioClave p = (Argumentos.CambioClave) a;
                service.changePassword(p.id(), p.claveActual(), p.claveNueva());
                break;
            }
            case CREATE_FUNCIONARIO:
                service.create(ConvertidorDto.funcionario(((Argumentos.Funcionario) a).funcionario())); break;
            case UPDATE_FUNCIONARIO:
                service.update(ConvertidorDto.funcionario(((Argumentos.Funcionario) a).funcionario())); break;
            case DELETE_FUNCIONARIO:
                service.delete(ConvertidorDto.funcionario(((Argumentos.Funcionario) a).funcionario())); break;
            case SEARCH_FUNCIONARIO:
                return new Resultado.Funcionarios(service.search(
                        ConvertidorDto.funcionario(((Argumentos.Funcionario) a).funcionario())).stream()
                        .map(ConvertidorDto::funcionario).toArray(FuncionarioDto[]::new));
            case CREATE_CATEGORIA:
                service.create(ConvertidorDto.categoria(((Argumentos.Categoria) a).categoria())); break;
            case UPDATE_CATEGORIA:
                service.update(ConvertidorDto.categoria(((Argumentos.Categoria) a).categoria())); break;
            case DELETE_CATEGORIA:
                service.delete(ConvertidorDto.categoria(((Argumentos.Categoria) a).categoria())); break;
            case SEARCH_CATEGORIA:
                return new Resultado.Categorias(service.search(
                        ConvertidorDto.categoria(((Argumentos.Categoria) a).categoria())).stream()
                        .map(ConvertidorDto::categoria).toArray(CategoriaDto[]::new));
            case CREATE_RECURSO:
                service.create(ConvertidorDto.recurso(((Argumentos.Recurso) a).recurso())); break;
            case UPDATE_RECURSO:
                service.update(ConvertidorDto.recurso(((Argumentos.Recurso) a).recurso())); break;
            case DELETE_RECURSO:
                service.delete(ConvertidorDto.recurso(((Argumentos.Recurso) a).recurso())); break;
            case SEARCH_RECURSOS: {
                Argumentos.BuscarRecursos p = (Argumentos.BuscarRecursos) a;
                return new Resultado.Recursos(service.search(ConvertidorDto.categoria(p.categoria()), p.descripcion())
                        .stream().map(ConvertidorDto::recurso).toArray(RecursoDto[]::new));
            }
            case FIND_RESERVAS:
                return reservas(service.findReservas(funcionario(sesion)));
            case CREAR_RESERVA: {
                Argumentos.CrearReserva p = (Argumentos.CrearReserva) a;
                return new Resultado.Reserva(ConvertidorDto.reserva(service.crearReserva(funcionario(sesion),
                        p.actividad(), p.fecha(), p.horaInicio(), p.horaFin(),
                        Arrays.stream(p.categoriasSolicitadas()).map(ConvertidorDto::categoria).toList())));
            }
            case CANCELAR_RESERVA: {
                String id = ((Argumentos.Reserva) a).reserva().id();
                Funcionario propietario = funcionario(sesion);
                // Buscar en datos del servicio; un DTO puede falsificar el propietario.
                Reserva real = service.findReservas(propietario).stream()
                        .filter(r -> r.getId() != null && r.getId().equals(id)
                                && r.getFuncionario() != null
                                && propietario.getId().equals(r.getFuncionario().getId()))
                        .findFirst().orElseThrow(() -> new Rechazo(
                                CodigoError.NO_AUTORIZADO, "Reserva no disponible para este usuario"));
                service.cancelarReserva(real);
                break;
            }
            case FIND_RESERVAS_ACTIVAS_POR_FECHA:
                return reservas(service.findReservasActivasPorFecha(((Argumentos.Fecha) a).fecha()));
            case FIND_RESERVAS_ACTIVAS_POR_SEMANA: {
                Argumentos.Periodo p = (Argumentos.Periodo) a;
                return reservas(service.findReservasActivasPorSemana(p.desde(), p.hasta()));
            }
            case ESTADISTICAS_RECURSOS_POR_CATEGORIA: {
                Argumentos.Periodo p = (Argumentos.Periodo) a;
                return ConvertidorDto.categorias(service.estadisticasRecursosPorCategoria(p.desde(), p.hasta()));
            }
            case ESTADISTICAS_ACTIVIDADES_POR_SEMANA: {
                Argumentos.Periodo p = (Argumentos.Periodo) a;
                return ConvertidorDto.semanas(service.estadisticasActividadesPorSemana(p.desde(), p.hasta()));
            }
            case EXTRAER_RESERVA:
                return new Resultado.Extraccion(ConvertidorDto.extraccion(
                        service.extraerReserva(((Argumentos.ExtraerReserva) a).frase())));
            default: throw new Rechazo(CodigoError.OPERACION_NO_SOPORTADA, "Operacion no soportada");
        }
        return new Resultado.Confirmacion();
    }

    private Funcionario funcionario(SesionServidor sesion) {
        // El servicio recibe una copia y no puede cambiar la identidad guardada.
        return (Funcionario) ConvertidorDto.usuario(ConvertidorDto.usuario(sesion.usuario()));
    }

    private Resultado.Reservas reservas(java.util.List<Reserva> reservas) {
        return new Resultado.Reservas(reservas.stream().map(ConvertidorDto::reserva).toArray(ReservaDto[]::new));
    }
}
