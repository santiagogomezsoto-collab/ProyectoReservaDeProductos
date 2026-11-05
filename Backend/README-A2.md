# A2: servidor persistente, sesiones y despacho

## Alcance

Implementado sin modificar IService, Service, DAO, Database, reglas de negocio,
Swing o datos. No incluye ServiceProxy, SocketListener ni notificaciones.
El cliente de consola ClienteCategorias incorpora el saludo SYNC de A2.

## Flujo de conexion

Ambos extremos crean ObjectOutputStream y envian su cabecera antes de esperar
ObjectInputStream. CanalObjetos centraliza este orden y conserva el formato
ObjectStream de A1.

1. El cliente envia Identificacion(SYNC, null).
2. Backend crea una sesion tecnica y responde SesionCreada(id). El id contiene
   32 bytes de SecureRandom codificados en Base64 URL; el cliente no lo elige.
   Todavia no existe usuario autenticado.
3. Se intercambian Solicitud y Respuesta, una solicitud pendiente por conexion.
4. LOGIN delega en IService.login. Solo un resultado valido establece la identidad
   y el rol de la sesion. Se conserva el subtipo Funcionario sin guardar claves.
   Un login fallido deja la sesion anonima; repetir LOGIN tras autenticar se rechaza.
5. Un segundo socket envia Identificacion(ASYNC, id). Se exige una sesion vigente,
   autenticada y sin otro ASYNC asociado. Se responde SesionCreada(id) como acuse.
   El identificador es un secreto de acceso: no se imprime ni se guarda en reportes.
6. ASYNC no admite Solicitud. En A2 solo se mantiene la conexion y se detecta EOF;
   no se envian notificaciones. Sus streams nunca se mezclan con los de SYNC.
7. Cerrar SYNC invalida la sesion y cierra ASYNC. Cerrar ASYNC libera su asociacion,
   pero mantiene SYNC; se permite asociar otro ASYNC a esa sesion autenticada.

Esta secuencia concreta la propuesta de A1: SesionCreada se emite al abrir SYNC
como sesion tecnica, no como prueba de login. El cliente antiguo que envia
Solicitud sin saludo se rechaza; ClienteCategorias ya esta adaptado.
No cambiaron las firmas de solicitudes ni los 23 codigos.

## Autenticacion, autorizacion y despacho

- Autenticacion: usa exclusivamente el usuario devuelto por IService.login,
  verifica id/rol/subtipo y guarda una copia sin credenciales.
- Autorizacion: aplica la matriz aprobada, antes de llamar al servicio.
- Despachador: convierte DTO a entidades, invoca IService y convierte resultados.
  Confirmacion representa void. No se serializan Throwable ni entidades JAXB.

| Codigo | Operacion | Acceso |
|---:|---|---|
| 1 | findAllCategorias | Publico |
| 2 | login | Sesion no autenticada |
| 3 | changePassword | Publico, validando clave actual; autenticado solo su propio id |
| 4-7 | create/update/delete/search Funcionario | ADMIN |
| 8-10 | create/update/delete CategoriaRecurso | ADMIN |
| 11 | search CategoriaRecurso | ADMIN y FUNCIONARIO |
| 12-14 | create/update/delete Recurso | ADMIN |
| 15 | search Recursos | ADMIN y FUNCIONARIO |
| 16 | findReservas | FUNCIONARIO, propio id |
| 17 | crearReserva | FUNCIONARIO, propio id |
| 18 | cancelarReserva | FUNCIONARIO, reserva propia verificada en el servicio |
| 19 | findReservasActivasPorFecha | Ambos roles |
| 20 | findReservasActivasPorSemana | Ambos roles |
| 21 | estadisticasRecursosPorCategoria | Ambos roles |
| 22 | estadisticasActividadesPorSemana | Ambos roles |
| 23 | extraerReserva | FUNCIONARIO |

Consultar/crear reservas valida el id del argumento y utiliza una copia de la
identidad autenticada para llamar a IService. El rol, nombre o telefono recibidos
en el DTO no sustituyen la identidad de sesion.

Cancelar valida primero el id del funcionario declarado; luego obtiene las
reservas propias mediante IService.findReservas y selecciona la reserva real por
id y propietario. Envia esa reserva a cancelarReserva. Falsificar propietario,
estado o fecha en el DTO no habilita cancelar otra reserva.

No se agregaron privilegios de reservas para ADMIN ni operaciones nuevas.

## Concurrencia, filtros y errores

Servidor acepta hasta 64 conexiones atendidas simultaneamente, incluidos ASYNC
y saludos en progreso; no deja conexiones persistentes esperando en una cola.
Al saturarse, cierra las conexiones adicionales. Cada Worker posee sus streams.

La instancia real Database utiliza una Connection compartida. El Despachador
sincroniza llamadas completas sobre Database.instance(), tambien la consulta de
pertenencia junto con la cancelacion. Los DAO conservan sus transacciones y el
bloqueo es reentrante. La espera de red y la serializacion de respuestas estan
fuera de ese bloqueo. La extraccion IA tambien queda serializada con el servicio;
puede demorar otras llamadas, una limitacion de esta estrategia conservadora.

CanalObjetos utiliza filtros distintos para saludo, solicitudes y respuestas.
Los limites son por mensaje: 4 MiB leidos, profundidad 24, 100000 referencias y
10000 elementos por arreglo. El contador de entrada tambien limita cadenas
que no pasan por callbacks del filtro. Cada envio hace reset para evitar
referencias obsoletas entre mensajes. Hay un lector y un escritor por conexion.

Saludo: timeout de lectura de 10 segundos. SYNC: timeout de inactividad de lectura
de 5 minutos. ASYNC espera EOF y se cierra al invalidar SYNC o apagar el servidor.
Los timeouts son de lectura del socket, no plazos absolutos de procesamiento.

Los filtros y mensajes invalidos provocan rechazo controlado cuando es posible
y cierre; TCP puede cerrar mientras el emisor aun escribe el objeto prohibido.
Un error de operacion normal no rompe el ciclo SYNC.

Rechazo diferencia AUTENTICACION, NO_AUTORIZADO, SESION_INVALIDA y formato invalido.
Excepciones de negocio se traducen a NEGOCIO con texto generico; errores SQL o de
ejecucion a INTERNO, sin trazas, SQL ni credenciales. No se cambian throws de
IService: las consultas siguen devolviendo sus tipos y los fallos de transporte
viajan en Respuesta. El futuro Proxy debera traducir estos errores sin agregar
excepciones checked a las consultas.

Se conserva una limitacion previa: si Service captura un fallo JDBC y devuelve
una lista vacia, el servidor no puede distinguir ese fallo de un resultado vacio.
El protocolo TCP sigue sin cifrado; A2 no incorpora TLS ni cambia el almacenamiento
actual de claves. La identidad queda fijada por el login durante esa sesion.

## Clases

- Servidor: aceptar conexiones, recursos compartidos, arranque y cierre.
- Worker: saludo, ciclo SYNC o permanencia ASYNC y limpieza en finally.
- RegistroSesiones / SesionServidor: token, identidad, asociacion y ciclo de vida.
- Autenticacion / Autorizacion: validacion de identidad y permisos por separado.
- Despachador / Rechazo: 23 rutas, bloqueo comun y errores publicos.
- Protocol: CanalObjetos, filtro de apertura, NO_AUTORIZADO y semantica de SesionCreada.
- ClienteCategorias: saludo y canal comun, sin ServiceProxy.

## Pruebas realizadas

- Protocol: 7 pruebas de A1 aprobadas y clean install correcto.
- Backend: 31 pruebas aprobadas (21 anteriores y 10 ServidorTest).
- Frontend: clean test correcto e instrumentacion Swing ejecutada; no tiene tests propios.
- ServidorTest utiliza sockets reales con IService simulado. Cubre las 23 rutas,
  argumentos relevantes, cambios/errores sin BD, permisos, identidad falsificada,
  reservas ajenas, dos clientes concurrentes, bloqueo comun, SYNC/ASYNC, duplicados,
  sesiones invalidas, limpieza, shutdown, filtros, ausencia de claves y limites.
- Una sesion transmite mas de 4 MiB acumulados correctamente; un solo mensaje
  superior al limite se rechaza.
- MySQL, solo lectura: Servidor PID 30892 y ClienteCategorias PID 17216 como
  procesos separados; cliente salida 0, categoria CAT-001 / Laboratorios.
- Otra conexion real verifico solicitudes sucesivas, login inexistente rechazado,
  consulta protegida rechazada y una consulta publica posterior correcta.
- Conteos y categoria iguales antes/despues: usuario 0, funcionario 0,
  categoria_recurso 1, recurso 1, reserva 0, reserva_recurso 0.

Con cero usuarios no se probo un login exitoso ni una asociacion ASYNC autenticada
contra MySQL; ambos se probaron con IService simulado. No se ejecutaron altas,
bajas, cambios de clave o reservas reales, ni la llamada externa de IA.
No se ejecutaron pruebas *IT que limpian tablas. No se crearon usuarios.

## Siguiente incremento (no implementado)

A3: ServiceProxy y arranque remoto, incluyendo conversiones, correlacion secuencial,
gestion de desconexion y traduccion de errores compatible con IService.
La autenticacion exitosa real requiere que el operador prepare usuarios aparte.
A5: SocketListener, emision de eventos por ASYNC a otros clientes y actualizacion
MVC del panel de alertas en EDT. A2 no envia esos eventos.
