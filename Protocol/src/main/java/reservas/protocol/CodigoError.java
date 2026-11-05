package reservas.protocol;

/** No transportar excepciones, trazas SQL ni credenciales en los mensajes. */
public enum CodigoError {
    SOLICITUD_INVALIDA, OPERACION_NO_SOPORTADA, AUTENTICACION,
    SESION_INVALIDA, NEGOCIO, INTERNO
}
