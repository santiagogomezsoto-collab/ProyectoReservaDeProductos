# A3 y A4: proxy completo y Frontend independiente

## Implementacion

FrontendApplication.main crea ServiceProxy fuera del EDT y lo inyecta mediante
Application.start(IService, Runnable). Application, las nueve pantallas, los
formularios y el panel derecho de alertas no cambian. Cancelar el login, cerrar
la ventana principal o terminar la JVM cierra el proxy.

ServiceProxy implementa las 23 firmas exactas de IService, incluidas sobrecargas
y declaraciones throws. Abre un unico socket, envia Identificacion(SYNC, null)
y conserva SesionCreada.sesionId. Ese identificador es secreto y no autentica al
usuario: login sigue siendo una operacion independiente validada por Backend.
No se abre ASYNC en este incremento.

Cada llamada convierte entidades a argumentos DTO, envia Solicitud y valida
Respuesta contra la operacion pendiente antes de convertir el resultado mediante
ConvertidorDto. Las listas reconstruidas son mutables; fechas, relaciones y
estadisticas conservan sus tipos. Las respuestas de usuario no contienen claves.
Confirmacion representa el resultado void.

Un executor de un hilo serializa el intercambio completo, incluso si varios
hilos llaman al mismo proxy. Se conserva CanalObjetos y sus filtros de A2.
ServiceProxyException transporta CodigoError y la indicacion conexionPerdida.
Es unchecked para respetar las consultas de IService sin excepciones checked;
un error nunca se sustituye por una lista vacia en el proxy. Los rechazos normales
mantienen la conexion; respuestas invalidas, timeout o desconexion la cierran.
No hay reintentos automaticos: una escritura cuyo acuse se pierde puede haberse
ejecutado. close es idempotente y desbloquea lecturas pendientes.

EsperaRemota conserva el contrato sincronico del MVC: la red se ejecuta en el
executor y, si llama el EDT, un dialogo modal de progreso procesa eventos mientras
espera. Impide nuevas acciones de las pantallas durante esa llamada sin modificar
sus controladores. Fuera del EDT se espera directamente el Future.

FrontendApplication instala el manejo de errores remotos no capturados en el EDT.
Las pantallas que ya capturan Exception conservan sus mensajes existentes.
LocalApplication sigue siendo una herramienta opcional del proyecto raiz; el
arranque remoto no incluye ese proyecto ni Backend en su classpath.

## Compilar y ejecutar (PowerShell, desde la raiz)

Usar `mvn` disponible en PATH; en este equipo tambien puede invocarse el mvn.cmd
incluido con IntelliJ. Primero instalar Protocol. La opcion skipITs evita ejecutar
las pruebas de Backend que limpian tablas; las pruebas unitarias si se ejecutan.

```powershell
mvn -f Protocol/pom.xml clean install
mvn -f Backend/pom.xml clean install -DskipITs dependency:build-classpath '-Dmdep.outputFile=target/a34-classpath.txt'
mvn -f Frontend/pom.xml clean install dependency:build-classpath '-Dmdep.outputFile=target/a34-classpath.txt'
```

Backend, en una terminal (usa su configuracion JDBC local ignorada por Git):

```powershell
$backendCp = (Resolve-Path Backend/target/classes).Path + ';' + (Get-Content Backend/target/a34-classpath.txt -Raw).Trim()
java -cp $backendCp reservas.server.Servidor 5000 127.0.0.1
```

Frontend, en otra terminal:

```powershell
$frontendCp = (Resolve-Path Frontend/target/classes).Path + ';' + (Get-Content Frontend/target/a34-classpath.txt -Raw).Trim()
java -cp $frontendCp reservas.FrontendApplication 127.0.0.1 5000
```

Para otra maquina, configurar la direccion de escucha del servidor y pasar su
host al Frontend. Ningun archivo JDBC ni jar Backend debe copiarse al Frontend.
El transporte conserva las caracteristicas de seguridad de A2 (TCP sin TLS).

## Pruebas reproducibles

Tras los tres comandos de compilacion, ejecutar `mvn test` en la raiz, en una
sesion con escritorio grafico. El pom raiz agrega solo JUnit para pruebas y
Surefire. Usa sus dependencias preexistentes de ambos modulos para probar la
integracion, sin agregar Backend a Frontend/pom.xml.

- ServicioSimulado: fixtures exclusivamente en memoria, sin JDBC.
- ServiceProxyTest: ocho pruebas, incluyendo las 23 firmas, conversiones, void,
  credenciales ausentes en resultados, rechazos, solicitudes sucesivas y
  concurrentes, filtros, respuesta incorrecta, timeout, desconexion, cierre,
  invalidacion de sesion y procesamiento de eventos del EDT durante la espera.
- FrontendArranqueTest: una prueba lanza dos procesos Java de Frontend (ADMIN y
  FUNCIONARIO), comprueba login, seis/cuatro pestañas, alertas visibles en todas
  ellas y liberacion de sesiones al cerrar.
- FrontendSmokeProcess: auxiliar solo de pruebas. Comprueba que Service,
  Database, el driver MySQL y reservas.properties no son accesibles desde el
  classpath del proceso Frontend. Automatiza las ventanas instrumentadas.
  El modo `real` solo consulta categorias y prueba un login inexistente; no escribe.

## Resultado por operacion

Todas las rutas siguientes se probaron con ServiceProxy, TCP y Worker reales,
inyectando IService simulado. Esto verifica transporte y conversiones, no valida
las reglas JDBC ni el proveedor externo de IA.

| Codigo | Firma | Simulado | MySQL en A3/A4 |
|---:|---|---|---|
| 1 | findAllCategorias() | OK | OK, CAT-001 / Laboratorios |
| 2 | login(String, String) | OK, ambos roles y rechazo | Solo rechazo de usuario inexistente |
| 3 | changePassword(String, String, String) | OK y rechazo | No ejecutada |
| 4 | create(Funcionario) | OK | No ejecutada |
| 5 | update(Funcionario) | OK | No ejecutada |
| 6 | delete(Funcionario) | OK | No ejecutada |
| 7 | search(Funcionario) | OK | No ejecutada |
| 8 | create(CategoriaRecurso) | OK | No ejecutada |
| 9 | update(CategoriaRecurso) | OK | No ejecutada |
| 10 | delete(CategoriaRecurso) | OK | No ejecutada |
| 11 | search(CategoriaRecurso) | OK | No ejecutada |
| 12 | create(Recurso) | OK | No ejecutada |
| 13 | update(Recurso) | OK | No ejecutada |
| 14 | delete(Recurso) | OK | No ejecutada |
| 15 | search(CategoriaRecurso, String) | OK | No ejecutada |
| 16 | findReservas(Funcionario) | OK | No ejecutada |
| 17 | crearReserva(Funcionario, String, LocalDate, LocalTime, LocalTime, List<CategoriaRecurso>) | OK | No ejecutada |
| 18 | cancelarReserva(Reserva) | OK | No ejecutada |
| 19 | findReservasActivasPorFecha(LocalDate) | OK | No ejecutada |
| 20 | findReservasActivasPorSemana(LocalDate, LocalDate) | OK | No ejecutada |
| 21 | estadisticasRecursosPorCategoria(LocalDate, LocalDate) | OK | No ejecutada |
| 22 | estadisticasActividadesPorSemana(LocalDate, LocalDate) | OK | No ejecutada |
| 23 | extraerReserva(String) | OK | No ejecutada |

## Cierre de verificacion, 2026-11-05

- Protocol: compilacion y siete pruebas aprobadas.
- Backend: compilacion y 31 pruebas aprobadas; se omitieron explicitamente *IT.
- Frontend: compilacion limpia e instrumentacion Swing correctas.
- Raiz: nueve pruebas aprobadas, cero omitidas.
- MySQL real: Backend PID 33332 y Frontend PID 1608, procesos independientes;
  Frontend termino con codigo 0. Dos consultas de categorias en el mismo proxy,
  con login fallido entre ellas; luego arranque Swing, error de login controlado
  y cancelacion. Classpath Frontend sin Backend, JDBC ni configuracion de BD.
- Solo SELECT para verificar conteos antes/despues, sin cambios: usuario 0,
  funcionario 0, categoria_recurso 1, recurso 1, reserva 0, reserva_recurso 0.
- No se crearon usuarios ni se ejecutaron escrituras reales o scripts SQL.
- reservas.properties sigue ignorado; .idea/encodings.xml conserva su hash previo.

## Limites y siguiente incremento

Con cero usuarios falta verificar login exitoso y operaciones protegidas contra
MySQL cuando el operador prepare cuentas reales por separado. Las escrituras
solo se probaron con IService simulado. No se probo el proveedor real de IA.

El timeout de lectura del proxy es 30 segundos (configurable en su constructor).
A2 cierra SYNC tras cinco minutos sin solicitudes; no se implementa reconexion
automatica ni recuperacion de autenticacion. Una extraccion IA lenta puede agotar
el timeout. Las consultas de Service que internamente ocultan fallos JDBC como
listas vacias mantienen esa limitacion. Los void no devuelven ids asignados a
argumentos, tal como establece A1; las pantallas existentes recargan sus datos.

A5 queda pendiente: asociar ASYNC usando la sesion autenticada, definir y emitir
eventos a otros clientes, implementar SocketListener y actualizar el modelo de
alertas en el EDT. No se implementa ni se inicia A5 en este incremento.
