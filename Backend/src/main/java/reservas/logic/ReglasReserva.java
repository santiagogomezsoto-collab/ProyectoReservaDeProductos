package reservas.logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Reglas de una reserva que NO dependen de la base de datos.
 *
 * Estan separadas del Service a proposito: asi se pueden comprobar con pruebas
 * unitarias (Surefire) sin necesidad de MySQL. Todo lo que si necesita
 * consultar datos (disponibilidad de recursos) se queda en Service y en el DAO,
 * y se comprueba con pruebas de integracion (Failsafe).
 *
 * La fecha y la hora actuales se reciben como parametro en lugar de llamar a
 * LocalDate.now() aqui dentro. Eso hace que las reglas sean deterministas: una
 * prueba puede fijar "hoy" y verificar el comportamiento sin depender del
 * momento en que se ejecute.
 */
public final class ReglasReserva {

    private ReglasReserva() {
    }

    /**
     * Valida los datos de una solicitud de reserva antes de consultar nada.
     * Lanza Exception con el mensaje para el usuario si algo no cumple.
     */
    public static void validarSolicitud(List<CategoriaRecurso> categoriasSolicitadas,
                                        LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                                        LocalDate hoy, LocalTime ahora) throws Exception {

        if (categoriasSolicitadas == null || categoriasSolicitadas.isEmpty())
            throw new Exception("Debe seleccionar al menos una categoria");

        if (horaFin.isBefore(horaInicio) || horaFin.equals(horaInicio))
            throw new Exception("La hora de fin debe ser posterior a la hora de inicio");

        if (esPasado(fecha, horaInicio, hoy, ahora))
            throw new Exception("No se puede crear una reserva en una fecha u hora pasada");
    }

    /**
     * Indica si ese instante ya paso. Una reserva para hoy mismo exige que la
     * hora de inicio sea estrictamente posterior a la hora actual.
     */
    public static boolean esPasado(LocalDate fecha, LocalTime horaInicio,
                                   LocalDate hoy, LocalTime ahora) {
        return fecha.isBefore(hoy) || (fecha.equals(hoy) && !horaInicio.isAfter(ahora));
    }

    /**
     * Indica si dos intervalos de tiempo se cruzan.
     *
     * Dos intervalos se solapan si cada uno empieza antes de que el otro
     * termine. Intervalos contiguos NO se solapan: una reserva de 8:00 a 9:00
     * y otra de 9:00 a 10:00 pueden usar el mismo recurso.
     *
     * Es la misma condicion que aplica la consulta SQL de disponibilidad en
     * ReservaDao; aqui queda expresada en Java para poder probarla sola.
     */
    public static boolean seSolapan(LocalTime inicioA, LocalTime finA,
                                    LocalTime inicioB, LocalTime finB) {
        return inicioA.isBefore(finB) && finA.isAfter(inicioB);
    }
}
