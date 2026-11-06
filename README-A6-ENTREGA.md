# A6: validacion real y entrega

Fecha: 2026-11-05. Rama: `integracion/proyecto2-final`, HEAD `8664484` (A5).
No se hizo commit ni push. Este reporte actualiza los resultados historicos de
A1-A5; no transforma sus pruebas simuladas en pruebas de MySQL.

## Resultado funcional real

Se ejecuto exactamente el plan autorizado de
[auditoria A6](Backend/README-A6-AUDITORIA.md), con Backend y tres Frontends
como procesos Java independientes, usando Service real y MySQL 8.0.46 local.
La automatizacion acciona los formularios Swing reales en EDT. El auxiliar
JDBC solamente consulta; todas las escrituras pasan por Swing/ServiceProxy/TCP.
Las claves temporales se transportaron en memoria y stdin, nunca como argumentos,
salida de consola o archivos. No se cambio la configuracion de logs de MySQL.

| Prueba real | Resultado |
|---|---|
| Login ADMIN, FUNCIONARIO A/B y usuario inexistente | Aprobada; error controlado |
| Nueve pantallas | Abiertas; seis pestanas ADMIN y cuatro FUNCIONARIO; cambio de clave solo abrir/cerrar |
| Consultas de funcionarios, categorias y recursos | Aprobadas |
| Permisos | FUNCIONARIO no puede buscar funcionarios ni consultar reservas de otro funcionario |
| Crear reserva desde formulario A | Una sola reserva, confirmada y persistida |
| ASYNC con B conectado antes de crear | Una alerta en B, ninguna propia en A; conteo repetido tras consultas SYNC |
| Ver y Descartar | Detalle abierto; NO conserva alerta, SI elimina solo alerta local; BD sigue ACTIVA |
| Reiniciar Frontend A | Misma reserva visible en tabla y consulta remota |
| Cancelar desde B | NO_AUTORIZADO; reserva y asociacion intactas |
| Cancelar desde A | CANCELADA; asociacion conservada |
| Calendarizacion, actividades, estadisticas | Sin reservas al inicio; una activa despues de crear; cero activas despues de cancelar |
| Cierre y perdida del servidor | Ventanas cierran sus procesos; proxy detecta perdida de conexion y queda cerrado |
| Independencia del Frontend | Service, Database, driver MySQL y reservas.properties inaccesibles en su classpath |

Evidencia conservada: `RES-000001`, propietario `A6-FUN-A`, actividad
`A6 - Validacion integral TCP y alertas`, **2026-11-06 de 10:00 a 11:00**,
recurso `RES-001` de `CAT-001`, estado final **CANCELADA**.
Conteos finales: usuario **3**, funcionario **2**, categoria_recurso **1**,
recurso **1**, reserva **1**, reserva_recurso **1**. No se modificaron usuarios,
claves, roles, funcionarios, categorias ni recursos. No hubo fallos funcionales
en el plan ni escrituras adicionales de recuperacion.

El [registro sanitizado](tools/a6/resultado-2026-11-05.txt) conserva los pasos
comprobados. Los auxiliares estan en `tools/a6`; no forman parte de Maven ni
de la aplicacion. No volver a ejecutar la prueba de escritura en esta base:
requiere cero reservas y una fecha futura, y se detiene si no se cumplen.
La evidencia existente no se borra para poder repetirla.

## Compilacion y pruebas seguras

Validadas con Windows, JDK 25.0.4 y Maven local; los POM compilan a Java 17.
La reconstruccion fue limpia en los tres modulos, usando dependencias ya
disponibles en la cache Maven. No se verifico una descarga desde una cache vacia.
No se requieren IntelliJ ni Backend en el classpath del Frontend: la
instrumentacion de los nueve `.form` la ejecuta Maven.

Desde la raiz, con JDK y Maven en PATH y escritorio grafico:

```powershell
mvn -f Protocol/pom.xml clean install
mvn -f Backend/pom.xml clean install -DskipITs dependency:build-classpath '-Dmdep.outputFile=target/a34-classpath.txt'
mvn -f Frontend/pom.xml clean install dependency:build-classpath '-Dmdep.outputFile=target/a34-classpath.txt'
mvn test
```

Comprobar codigo de salida de cada comando antes del siguiente. En esta maquina
se uso Maven de IntelliJ con `-o` y `-Dmaven.repo.local=C:/Users/Ale/.m2/repository`;
estas dos opciones no son necesarias en una instalacion nueva con acceso a los
repositorios declarados en los POM.

Resultado: **54 pruebas aprobadas**, cero fallos/errores/omitidas de Surefire:
Protocol **9**, Backend **31**, raiz **14**. Frontend compila e instrumenta sus
formularios. Los mensajes de Failsafe `Tests are skipped` corresponden a
`-DskipITs`, no a pruebas unitarias omitidas.

Las pruebas de servidor, proxy y alertas inyectan `IService` simulado, con sockets
y Swing reales; cubren las 23 firmas, permisos, filtros, serializacion, sesiones,
concurrencia, desconexion/reconexion ASYNC y limpieza. Las pruebas puras de
reglas/configuracion tampoco escriben MySQL. **No se ejecutaron las clases IT**:
limpian tablas, exigen una base `_test` y no existe una configurada en este entorno.
No ejecutar `mvn verify/install` en Backend sin `-DskipITs` sobre este entorno.

Las operaciones verificadas contra MySQL son 1, 2, 7, 11, 15, 16, 17, 18,
19, 20, 21 y 22 del protocolo. Cambio efectivo de clave (3), CRUD de funcionarios
(4-6), categorias (8-10), recursos (12-14) y proveedor IA (23) solo tienen la
cobertura simulada o unitaria documentada; no se presentan como validados contra
MySQL/proveedor real en A6.

## Arranque independiente

Preparar MySQL y las seis tablas segun [README-MYSQL](Backend/README-MYSQL.md)
solo en una instalacion nueva. **No ejecutar nuevamente los scripts en la base
actual.** `002_datos_iniciales.sql` contiene datos y claves publicas de demostracion,
no es una migracion idempotente ni es necesario para arrancar esta base.

Configurar privadamente `Backend/src/main/resources/reservas.properties` a partir
de su `.example`, sin versionarlo. En esta maquina el archivo ya existe y se
conservo. Despues de compilar, iniciar en terminales separadas:

```powershell
# Terminal Backend, desde la raiz
$cpBackend = 'Backend/target/classes;' + (Get-Content Backend/target/a34-classpath.txt -Raw).Trim()
java -cp $cpBackend reservas.server.Servidor 5000 127.0.0.1
```

```powershell
# Terminal Frontend, desde la raiz; repetir para otro cliente
$cpFrontend = 'Frontend/target/classes;' + (Get-Content Frontend/target/a34-classpath.txt -Raw).Trim()
java -cp $cpFrontend reservas.FrontendApplication 127.0.0.1 5000
```

En equipos diferentes indicar una direccion de escucha accesible en Backend,
su IP en Frontend y habilitar el puerto en la red de prueba. Esa topologia no
se probo en A6: los procesos reales se comunicaron por loopback. El transporte
actual es TCP sin TLS; la sesion tecnica no reemplaza el login de usuario.

Para ejecutar el JAR limpio de Backend, proporcionar la configuracion como
directorio externo en el classpath. El siguiente comando reutiliza el archivo
local sin copiarlo ni incluirlo en el JAR:

```powershell
$cpBackendJar = 'Backend/src/main/resources;Backend/target/reservas-backend-1.0-SNAPSHOT.jar;' + (Get-Content Backend/target/a34-classpath.txt -Raw).Trim()
java -cp $cpBackendJar reservas.server.Servidor 5000 127.0.0.1
```

En una distribucion binaria, reemplazar el primer directorio por uno privado
del operador con su `reservas.properties`, y suministrar las dependencias de
ejecucion. Los JAR no son fat JAR ni se ejecutan con `java -jar`.
Se verifico ademas el arranque del JAR Backend con configuracion externa y el
JAR Frontend ejecutando ClienteCategorias en otro proceso: recibio CAT-001 por
TCP. Para repetir esa comprobacion de solo lectura despues de compilar:

```powershell
java tools/a6/VerificarJarA6.java
```

Este auxiliar inspecciona los tres JAR y rechaza configuraciones privadas o
data.xml; no inicia Swing ni realiza escrituras de negocio.
La raiz incluye LocalApplication/pruebas entre modulos: **no usarla como
classpath del Frontend remoto**.

## Empaquetado y Git

Fallo comprobado y corregido: el JAR anterior de Backend incluia la configuracion
JDBC real. `Backend/pom.xml` ahora configura `maven-jar-plugin` 3.5.0 para excluir
`**/*.properties` y `**/data.xml` del contenido de aplicacion. Las plantillas
`.properties.example` se conservan; `META-INF/maven/.../pom.properties` es metadata
de Maven, no configuracion JDBC. El archivo original y su copia local en
`target/classes` siguen disponibles; **no distribuir target/classes**.

Generar el ZIP de fuentes desde este repositorio:

```powershell
powershell -NoProfile -File tools/Empaquetar-Entrega.ps1
```

El script usa una lista permitida de fuentes, formularios, POM, SQL, plantillas,
reportes y auxiliares. Comprueba rutas y contenido del ZIP. Excluye `.idea`,
`.git`, `.env`, `target`, binarios, configuraciones locales y `data.xml`.
No borra ni altera los originales. El ZIP queda en `target/entrega`.
No usar `git archive` sin exclusiones: **data.xml con claves sigue versionado**.
Excluirlo del ZIP no elimina su presencia historica en Git; retirarlo o sanear
el historial requeriria una tarea autorizada aparte. No publicar el repositorio
completo como si ya estuviera libre de datos privados.

Archivos candidatos a incluir en un futuro commit revisado:

- `Backend/pom.xml`.
- `Backend/README-A6-AUDITORIA.md` (historial de auditoria y autorizaciones).
- `README-A6-ENTREGA.md`.
- `tools/Empaquetar-Entrega.ps1`.
- `tools/a6/ClienteRealA6.java`.
- `tools/a6/ValidacionRealA6.java`.
- `tools/a6/VerificarJarA6.java`.
- `tools/a6/resultado-2026-11-05.txt`.

Excluir del commit el cambio preexistente de `.idea/encodings.xml`; no se toco.
No agregar configuraciones privadas, logs, `data.xml`, auxiliares antiguos de
creacion de cuentas ni archivos generados. `reservas.properties` sigue ignorado.
Comprobacion final: `git diff --check` sin errores; el hash de `.idea/encodings.xml`
coincide con el registrado al iniciar y `data.xml` no tiene diferencias con Git.
El unico archivo de produccion modificado es `Backend/pom.xml`; los demas cambios
de A6 son documentacion, auxiliares manuales y empaquetado. El ZIP de fuentes
verificado contiene 148 archivos; los tres JAR inspeccionados no contienen
configuracion privada ni data.xml. No se hizo commit, merge ni push.

## Matriz de requisitos y limites

La ponderacion se toma de `PROJECT_CONTEXT.md`; no se encontro el PDF oficial
en este checkout. No se atribuye una nota ni se afirma cotejo directo con ese PDF.

| Requisito | Peso | Estado de validacion y evidencia |
|---|---:|---|
| Login y cambio de clave | 5% | Parcial: login real ambos roles; cambio de clave solo formulario y transporte simulado |
| Reservas | 20% | Cumplido en flujo autorizado: crear, persistir, listar, cancelar propia, rechazar ajena; IA real pendiente |
| Funcionarios | 8% | Parcial: pantalla y consultas reales; CRUD con escritura no autorizado/probado en MySQL |
| Categorias | 8% | Parcial: pantalla y consultas reales; CRUD con escritura pendiente |
| Recursos | 8% | Parcial: pantalla y consultas reales; CRUD con escritura pendiente |
| Calendarizacion | 8% | Cumplido para el caso real antes/despues de creacion/cancelacion |
| Actividades | 8% | Cumplido para el caso real y su semana |
| Estadisticas | 15% | Cumplido para una reserva real: tablas, consultas y sumas antes/despues |
| Alertas | 20% | Cumplido: ASYNC separado, otro funcionario, sin duplicado observado, detalle y descarte local |
| Arquitectura distribuida/MVC/MySQL | — | Cumplido en procesos independientes; prueba entre maquinas pendiente |
| JUnit/Surefire/Failsafe | — | Unitarias y simuladas aprobadas; IT JDBC destructivas no ejecutadas |
| Fuentes/SQL/instrucciones | — | ZIP filtrado y comandos disponibles; configuracion privada externa |
| GitHub actualizado/entrega oficial | — | Pendiente de autorizacion para commit/push; no ejecutados |

No aparecieron fallos funcionales que bloqueen el flujo autorizado. El bloqueo
de credenciales en el JAR se corrige aqui. Quedan pendientes para declarar
validacion integral completa las escrituras CRUD/cambio de clave fuera del plan,
IA real y las IT en una base aislada. `data.xml` impide publicar sin revision el
repositorio completo, aunque queda fuera del entregable filtrado. No se modifican
reglas de negocio, IService, DAO, Database, Swing ni arquitectura.

## Reproduccion del auxiliar manual A6

La ejecucion original se hizo compilando `ClienteRealA6.java` con **solo** el
classpath Frontend, y ejecutando `ValidacionRealA6.java` con el de Backend/JDBC.
El orquestador lee privadamente las claves de las tres cuentas locales, crea
procesos separados y comunica las claves por stdin. Sus SELECT son read-only;
no contiene INSERT/UPDATE/DELETE JDBC. Las dos escrituras se accionan en Swing.

Solo en un entorno y fecha compatibles, despues de otra autorizacion expresa:

```powershell
New-Item -ItemType Directory -Force target/a6-classes | Out-Null
$cpFrontend = 'Frontend/target/classes;' + (Get-Content Frontend/target/a34-classpath.txt -Raw).Trim()
javac -cp $cpFrontend -d target/a6-classes tools/a6/ClienteRealA6.java
$cpJdbc = (Get-Content Backend/target/a34-classpath.txt -Raw).Trim()
java -cp $cpJdbc tools/a6/ValidacionRealA6.java PLAN-A6-AUTORIZADO-2026-11-06
```

El token no sustituye la autorizacion del operador. No cambiar fecha/ids ni
limpiar tablas para hacer pasar sus precondiciones. El log conservado describe
la ejecucion unica aprobada, no una prueba que deba repetirse al compilar.
