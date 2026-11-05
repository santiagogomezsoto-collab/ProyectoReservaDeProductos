# A5: alertas mediante un segundo socket TCP

## Flujo implementado

1. FrontendApplication conserva el arranque y login de A3/A4. Cuando el login
   termina correctamente y Application ya construyo el panel, llama a
   ServiceProxy.iniciarAlertas. No cambia las nueve pantallas ni sus formularios.
2. SocketListener abre otro socket en su hilo receptor, envia
   Identificacion(ASYNC, sid) y verifica que SesionCreada corresponda al mismo sid.
   No lee ni escribe en el socket SYNC. A2 sigue validando que la sesion exista,
   este autenticada y no tenga otro ASYNC asociado.
3. Worker despacha CREAR_RESERVA normalmente mediante IService. Solo si recibe
   una respuesta exitosa con la reserva resultante prepara ReservaCreada.
   El Service real retorna despues de reservaDao.create, que confirma su
   transaccion antes de retornar. No se modificaron Service, DAO o transacciones.
4. RegistroSesiones ofrece el evento a sesiones vigentes, autenticadas como
   FUNCIONARIO, con ASYNC disponible. Excluye el id autenticado del creador,
   incluso si ese usuario tiene otra sesion. ADMIN no recibe estos avisos.
5. Cada EmisorAlertas envia exclusivamente por el ASYNC de su destinatario.
   Envia primero el acuse de sesion y luego los eventos. Una cola de 128 eventos
   y un hilo escritor por conexion separan los envios del Worker SYNC y del
   bloqueo compartido de IService. Si la cola se llena, se cierra ese ASYNC.
6. SocketListener recibe ReservaCreada y el callback de FrontendApplication lo
   convierte en Alerta. Controller.agregar, ya existente, usa invokeLater para
   actualizar Model y View en el EDT. Ver y Descartar permanecen intactos.

La confirmacion que activa el aviso es el retorno exitoso del servicio, no la
recepcion del acuse por el cliente creador. Si se pierde ese acuse SYNC despues
de crear, los demas clientes aun pueden recibir el aviso. No se reintenta crear
la reserva. Un rechazo de permisos o de negocio no produce notificacion.

## Clases nuevas y modificadas

| Archivo/clase | Responsabilidad y metodos |
|---|---|
| Protocol/ReservaCreada (nuevo) | Record serializable con id de reserva, encargado, actividad, fecha, desde/hasta y descripciones de recursos con categoria. `de(ReservaDto)` toma el resultado confirmado; copia defensivamente el arreglo. Sin claves, telefono, rol ni entidades JAXB. |
| Protocol/FiltrosProtocolo | `notificaciones()` admite solo ese evento, strings y fechas/horas. No amplia los filtros SYNC. |
| Backend/EmisorAlertas (nuevo) | `iniciar`, `ofrecer`, `close`: un escritor ASYNC con cola acotada, saludo primero y cierre sin esperar a la red. |
| Backend/SesionServidor | `iniciarEnvios`, `notificar`, `separar`, `cerrar`: seleccion de destinatario por identidad real y ciclo de vida del emisor. |
| Backend/RegistroSesiones | `notificar`: distribuye una vez por sesion elegible. |
| Backend/Worker | Publica tras CREAR_RESERVA exitosa; activa el emisor despues de asociar ASYNC. Mantiene el lector de desconexion de ese canal. |
| Frontend/SocketListener (nuevo) | `iniciar`, `run`, `close`, `isConectado`: saludo, filtro, recepcion persistente y reconexion en hilo propio. |
| Frontend/ServiceProxy | Guarda host/puerto y confirmacion local de login; `iniciarAlertas`, `detenerAlertas` y `close` gestionan el receptor sin cambiar IService. Backend sigue siendo la autoridad de autenticacion. |
| Frontend/FrontendApplication | Inicia el receptor tras login y construccion del panel; convierte el evento en Alerta y utiliza el cierre existente del proxy. Los errores se muestran en el EDT. |

El Controller, Model, View y Alerta de Parte C no necesitaron cambios.

## Desconexion y limites de entrega

- El cierre del proxy cierra SYNC y SocketListener; no espera un join en el EDT.
  Backend elimina la sesion y cierra su ASYNC y su emisor.
- Una interrupcion de transporte ASYNC produce un aviso y un intento de
  reconexion cada segundo usando la misma sesion. SYNC permanece disponible.
- Un rechazo de asociacion o un mensaje no permitido detiene el receptor;
  no intenta autenticar ni crear otra sesion. Para una sesion rechazada se indica
  reiniciar el Frontend. La API tambien permite detener/iniciar ASYNC explicitamente.
- No hay historial persistente, reenvios ni recuperacion de avisos perdidos
  durante desconexiones, saturacion de cola o antes de terminar la asociacion.
  La entrega unica se comprueba con canales activos; no se promete entrega
  exactamente una vez ante fallos de red. Descartar no provoca reenvio.
- Se conservan limites de filtros y timeout SYNC de A2/A3. ASYNC no hace heartbeat
  ni mantiene viva la sesion SYNC: el timeout de inactividad existente sigue vigente.
- Los mensajes ASYNC se envian fuera del bloqueo JDBC. Un fallo de entrega no
  revierte ni modifica la reserva ya creada.

## Pruebas de A5 (IService simulado; sin MySQL)

`Protocol/NotificacionTest`: dos pruebas nuevas. Serializacion y deserializacion
con filtro, datos del evento, copias defensivas y rechazo cruzado de mensajes
SYNC/ASYNC y de clases ajenas.

`src/test/java/reservas/server/AlertasAsyncTest`: cinco pruebas nuevas con sockets,
ServiceProxy, Worker, SocketListener y panel reales, inyectando solo IService:

1. A, B y C autenticados; diez creaciones de A, diez eventos distintos para B y C,
   ninguno para A, otra sesion del mismo A ni ADMIN. Consultas SYNC concurrentes
   de B/C mientras se transmiten los eventos.
2. Creacion ajena rechazada y excepcion de negocio: cero eventos. Servicio
   detenido con latch: cero eventos hasta que confirma la creacion.
3. Detener/reabrir ASYNC y perdida de socket con reconexion automatica; sin
   reenvio del historial, consultas SYNC operativas y limpieza al cerrar la sesion.
4. Rechazo de sesiones inexistentes, cerradas y anonimas, incluido SocketListener.
5. Evento recibido hasta el modelo Swing en EDT, detalle Ver con encargado,
   fecha, horario y recursos; Descartar con NO conserva la alerta y con SI la
   elimina localmente. Cero llamadas a cancelarReserva.

Tambien pasan las pruebas anteriores de A1-A4, incluida la ejecucion Swing en
procesos independientes sin Backend/JDBC en su classpath.

## Compilacion y resultado

Desde la raiz, con Maven y escritorio grafico para las pruebas Swing:

```powershell
mvn -f Protocol/pom.xml clean install
mvn -f Backend/pom.xml clean install -DskipITs dependency:build-classpath '-Dmdep.outputFile=target/a34-classpath.txt'
mvn -f Frontend/pom.xml clean install dependency:build-classpath '-Dmdep.outputFile=target/a34-classpath.txt'
mvn test
```

`skipITs` evita las pruebas de Backend que limpian tablas. No se cambiaron POMs
ni dependencias. Los comandos de arranque independiente siguen siendo los de
[README-A3-A4.md](README-A3-A4.md); ASYNC se inicia automaticamente tras login.

Resultado: Protocol 9 pruebas, Backend 31, raiz 14 (5 de A5 y 9 anteriores),
sin fallos ni pruebas unitarias omitidas. Frontend compila e instrumenta formularios.

## MySQL y estado del incremento

No se ejecutaron pruebas de A5 contra MySQL real ni escrituras de ningun tipo.
Los resultados de MySQL documentados en A1-A4 son historicos y no prueban A5.
La entrega real de alertas con usuarios y reservas persistidos queda sin verificar;
requiere cuentas preparadas por el operador y autorizacion para crear reservas.

IService, entidades, Service, DAO, Database, permisos A2, pantallas, formularios,
data.xml y reservas.properties permanecen sin cambios. El cambio previo de
.idea/encodings.xml se preservo con el mismo SHA-256. No se hace commit ni push.
Este incremento termina en A5; no se ejecuta la fase de pruebas finales A6.
