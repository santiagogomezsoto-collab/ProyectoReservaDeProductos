package reservas.server;

import reservas.logic.*;
import reservas.protocol.*;

final class Autenticacion {
    private Autenticacion() { }

    static Usuario login(IService service, SesionServidor sesion, Argumentos.Login a) throws Exception {
        if (sesion.usuario() != null)
            throw new Rechazo(CodigoError.SOLICITUD_INVALIDA, "La sesion ya esta autenticada");
        if (a.id() == null || a.id().isBlank() || a.clave() == null)
            throw new Rechazo(CodigoError.AUTENTICACION, "Credenciales invalidas");
        Usuario usuario = service.login(a.id(), a.clave());
        if (usuario == null || !a.id().equals(usuario.getId())
                || !(Usuario.ADMIN.equals(usuario.getRol())
                    || Usuario.FUNCIONARIO.equals(usuario.getRol()) && usuario instanceof Funcionario))
            throw new Rechazo(CodigoError.AUTENTICACION, "Credenciales invalidas");
        // Conservar identidad y subtipo, sin guardar claves en la sesion.
        Usuario copia = ConvertidorDto.usuario(ConvertidorDto.usuario(usuario));
        sesion.autenticar(copia);
        return copia;
    }
}
