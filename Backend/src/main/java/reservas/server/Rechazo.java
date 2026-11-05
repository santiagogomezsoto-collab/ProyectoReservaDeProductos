package reservas.server;

import reservas.protocol.CodigoError;

/** Error publico seleccionado por el servidor, sin detalles internos. */
final class Rechazo extends Exception {
    final CodigoError codigo;
    Rechazo(CodigoError codigo, String mensaje) { super(mensaje); this.codigo = codigo; }
}
