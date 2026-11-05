package reservas.client;

import reservas.protocol.CodigoError;

/** No checked para respetar las consultas sin throws de IService. */
public final class ServiceProxyException extends RuntimeException {
    private final CodigoError codigo;
    private final boolean conexionPerdida;

    ServiceProxyException(CodigoError codigo, String mensaje, boolean conexionPerdida) {
        super(mensaje);
        this.codigo = codigo;
        this.conexionPerdida = conexionPerdida;
    }

    public CodigoError getCodigo() { return codigo; }
    public boolean isConexionPerdida() { return conexionPerdida; }
}
