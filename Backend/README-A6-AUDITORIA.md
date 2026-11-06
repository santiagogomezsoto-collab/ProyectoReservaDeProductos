# A6: auditoria inicial y propuesta de usuarios (sin ejecutar inserciones)

> Documento historico por fases. El plan de reserva descrito al final fue
> autorizado y ejecutado el 2026-11-05. Resultado: RES-000001 CANCELADA y su
> asociacion RES-001 conservada; conteos finales 3, 2, 1, 1, 1, 1. Consultar
> [reporte final A6](../README-A6-ENTREGA.md) para pruebas y empaquetado actuales.

Fecha: 2026-11-05. Esta es la salida de las fases 1 y 2, no el reporte final
de validacion integral. No se ejecutaron las pruebas funcionales con escrituras
ni se modifico codigo de produccion. Se requiere autorizacion para continuar.

## Git y alcance revisado

Rama comprobada: `integracion/proyecto2-final`. Incrementos guardados:

| Incremento | Commit |
|---|---|
| Parte B, ultimo commit integrado | e0fb9ee |
| Parte C | a29ed90 |
| A1 | dc50b25 |
| A2 | 24b4678 |
| A3/A4 | 27a28f2 |
| A5 / HEAD | 8664484 |

Al iniciar, el unico cambio local era `.idea/encodings.xml`. Se conserva intacto,
con SHA-256 `3220D20A05D1D13DB3F32194FEE970FB3317E076D96A4EA9FF1762C3FA4D561B`.
Este reporte es el unico archivo nuevo fuera de target. El auxiliar de consulta
en target es temporal, ignorado por Git y no forma parte del entregable.

Se revisaron PROJECT_CONTEXT, los reportes A1-A5, los POM, scripts SQL,
README-MYSQL, Database, Service, DAO de funcionarios, autorizacion y flujo de
notificaciones. No se cambiaron reglas, firmas, configuraciones ni datos.

## MySQL comprobado ahora, solo lectura

Conexion JDBC exitosa a `reservas_proyecto2`, MySQL **8.0.46**. La consulta se
realizo con conexion marcada read-only y se cerro con rollback. No se consulto
la columna clave. Se leyeron conteos, ids/roles, integridad de fichas y metadatos.

| Tabla | Registros |
|---|---:|
| usuario | 0 |
| funcionario | 0 |
| categoria_recurso | 1 |
| recurso | 1 |
| reserva | 0 |
| reserva_recurso | 0 |

Las seis tablas usan InnoDB. No hay fichas con rol incorrecto ni funcionarios
sin ficha. No hay colisiones con los tres identificadores propuestos abajo.
`reservas_proyecto2_test` no aparece entre los esquemas accesibles y no existe
`Backend/src/main/resources/reservas-test.properties`; por tanto no hay una
base separada correctamente configurada que podamos utilizar ahora.

## Como se crean y autentican usuarios actualmente

- `001_schema.sql` crea tablas sobre una base vacia. No crea cuentas de la
  aplicacion y no debe ejecutarse de nuevo sobre esta base.
- `002_datos_iniciales.sql` inserta usuarios, fichas, categorias y recursos;
  utiliza claves publicas de demostracion y no es idempotente. No se ejecuto.
  Sus comentarios incluyen instrucciones de limpieza que tampoco deben usarse.
- Un ADMIN necesita solo una fila en usuario. Un FUNCIONARIO necesita una fila
  en usuario y otra en funcionario con el mismo id y rol FUNCIONARIO.
- Service.login compara directamente la clave almacenada. No admite introducir
  hashes unilateralmente; hacerlo impediria iniciar sesion con la clave original.
- Service.changePassword exige la clave actual. FuncionarioDao.create asigna
  inicialmente la clave igual al id; esa ruta no sirve para elegir una clave
  inicial privada. La propuesta SQL siguiente permite suministrarla directamente
  sin modificar el DAO ni la regla de alta de la aplicacion.
- Los roles de las sesiones proceden de IService.login. La propuesta no otorga
  permisos de MySQL ni modifica la cuenta JDBC: crea cuentas de la aplicacion.

## Propuesta concreta para autorizar

Maximo **cinco filas nuevas**, exclusivamente en usuario y funcionario:

| id | rol en usuario | ficha funcionario | nombre de ficha | telefono |
|---|---|---|---|---|
| A6-ADMIN | ADMIN | no | no aplica | no aplica |
| A6-FUN-A | FUNCIONARIO | si | Validacion A6 A | cadena vacia |
| A6-FUN-B | FUNCIONARIO | si | Validacion A6 B | cadena vacia |

Estos ids y nombres son una propuesta de cuentas de validacion; aun no existen.
Los campos telefono admiten una cadena vacia segun el esquema actual. No se
inventaran datos personales ni contrasenas. El operador puede cambiar los ids
propuestos antes de autorizar; en ese caso hay que repetir la comprobacion de
colisiones y actualizar este plan.

### Procedimiento transaccional e idempotente

El siguiente SQL es una **plantilla para PreparedStatement de JDBC**, no un
script para pegar directamente en Workbench. Los tres `?` representan las
claves privadas que el operador suministrara despues de autorizar. No deben
sustituirse por literales en este documento, Git, argumentos de proceso, logs,
terminal o historial SQL. La herramienta de ejecucion debe recogerlas en campos
locales de contrasena, enlazarlas en memoria y no registrar parametros.

No se ha implementado ni ejecutado una herramienta de escritura en esta fase.

Antes de insertar, la herramienta debe:

1. Verificar que la conexion apunta exactamente a reservas_proyecto2.
2. Iniciar la transaccion y leer los ids propuestos con FOR UPDATE.
3. Si un id existe, comprobar rol y ficha: si no coinciden con esta propuesta,
   abortar y hacer rollback. Nunca corregir roles, claves o datos existentes
   automaticamente. Una ficha ausente de un usuario existente requiere nueva
   revision, no se completa silenciosamente.
4. Pedir una clave no vacia para cada usuario que realmente falte, maximo 255
   caracteres. Para cuentas existentes no pedir ni cambiar la clave.
5. Ejecutar las inserciones condicionales siguientes en la misma conexion.
   Si ocurre cualquier error, incluida una colision concurrente, hacer rollback
   de la transaccion completa y detenerse.

```sql
START TRANSACTION;

SELECT u.id, u.rol, f.id AS ficha_id, f.nombre, f.telefono
FROM usuario u LEFT JOIN funcionario f ON f.id = u.id
WHERE u.id IN ('A6-ADMIN', 'A6-FUN-A', 'A6-FUN-B')
FOR UPDATE;

-- Ejecutar cada INSERT de usuario solo si falta, enlazando su clave privada.
INSERT INTO usuario (id, clave, rol)
SELECT 'A6-ADMIN', ?, 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE id = 'A6-ADMIN');

INSERT INTO usuario (id, clave, rol)
SELECT 'A6-FUN-A', ?, 'FUNCIONARIO'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE id = 'A6-FUN-A');

INSERT INTO usuario (id, clave, rol)
SELECT 'A6-FUN-B', ?, 'FUNCIONARIO'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE id = 'A6-FUN-B');

INSERT INTO funcionario (id, nombre, telefono)
SELECT 'A6-FUN-A', 'Validacion A6 A', ''
WHERE EXISTS (SELECT 1 FROM usuario WHERE id = 'A6-FUN-A' AND rol = 'FUNCIONARIO')
  AND NOT EXISTS (SELECT 1 FROM funcionario WHERE id = 'A6-FUN-A');

INSERT INTO funcionario (id, nombre, telefono)
SELECT 'A6-FUN-B', 'Validacion A6 B', ''
WHERE EXISTS (SELECT 1 FROM usuario WHERE id = 'A6-FUN-B' AND rol = 'FUNCIONARIO')
  AND NOT EXISTS (SELECT 1 FROM funcionario WHERE id = 'A6-FUN-B');

SELECT u.id, u.rol, f.id AS ficha_id, f.nombre, f.telefono
FROM usuario u LEFT JOIN funcionario f ON f.id = u.id
WHERE u.id IN ('A6-ADMIN', 'A6-FUN-A', 'A6-FUN-B')
ORDER BY u.id;

-- La herramienta verifica las tres cuentas y las dos fichas antes de decidir:
-- COMMIT;    solamente si todas las condiciones anteriores se cumplen.
-- ROLLBACK;  ante cualquier error, diferencia o cancelacion.
```

Verificar antes de commit que ADMIN no tiene ficha, ambos FUNCIONARIO tienen
su ficha correcta y que se insertaron como maximo las cinco filas autorizadas.
En la primera ejecucion, con el estado actual, los conteos esperados son
3, 2, 1, 1, 0, 0. En una repeticion correcta se insertan cero filas, no se
modifican claves y los conteos permanecen iguales. No se usa INSERT IGNORE,
REPLACE, UPDATE, DELETE, TRUNCATE ni DDL.

Esta autorizacion propuesta **no incluye** reservas, categorias, recursos,
cambios de clave, CRUD adicional ni limpieza posterior. Para la fase 3 se
presentara por separado el listado exacto de registros y operaciones de prueba.

## Pruebas que escriben o eliminan registros

`ServiceIT`, `ServiceRecursosIT`, `ServiceCalendarizacionIT` y `ReservaIT` llaman
a PruebaBase.reiniciar antes de cada prueba. PruebaBase.limpiar borra todas las
filas de las seis tablas; ReservaIT tambien limpia despues. El guard exige un
nombre de base terminado en `_test`, pero no sustituye la revision de su destino.
No se ejecutaron estas pruebas ni `mvn verify` o `install` de Backend sin skipITs.

Los reportes previos registran 9 pruebas Protocol, 31 Backend y 14 de integracion
con servicios simulados: 54 aprobadas al cerrar A5. **No se volvieron a ejecutar
en esta auditoria**. Las integraciones TCP simuladas no escriben en MySQL.

## Entrega: hallazgos comprobados

- Frontend/pom.xml depende de Protocol y librerias de interfaz, no de Backend
  ni MySQL. No se hallaron imports de Service real, reservas.data o java.sql
  en el codigo Frontend. El lanzador LocalApplication pertenece solo a la raiz.
- reservas.properties existe, esta ignorado y no esta versionado. No aparece
  en el historial consultado de esa ruta. La busqueda literal de la clave JDBC
  local en archivos de texto versionados no encontro coincidencias; esto no
  equivale a una auditoria exhaustiva de todos los secretos historicos.
- El JAR local de Backend **si contiene reservas.properties**, porque Maven
  copia resources. No debe entregarse ese JAR ni target ni el directorio de
  trabajo completo. Para entregar fuentes, preparar una exportacion revisada
  con las plantillas .example y sin configuraciones reales ni artefactos locales.
- `.idea` y `data.xml` ya tienen archivos versionados. data.xml contiene cinco
  elementos clave del legado XML. No se mostro su contenido ni se modifico.
  No puede afirmarse que el repositorio carezca de datos de cuentas. La entrega
  debe excluir el XML legado y la configuracion del IDE sin alterar sus originales.
- 002_datos_iniciales y README-MYSQL contienen credenciales de demostracion;
  no utilizarlas para estas cuentas. No son las credenciales JDBC locales.
- README-MYSQL esta desactualizado: indica otra version de servidor y conteos
  antiguos de tests. Su instruccion Backend install omite skipITs. Usar los
  comandos de README-A5 para compilacion segura cuando corresponda la fase 4.
- La advertencia de 002 sobre CAT-001 no concuerda con el DAO: nextId convierte
  el sufijo numerico mediante CAST. No hay fundamento para renombrar la categoria
  existente; se conserva.
- La extraccion IA utiliza un servicio externo de demostracion. Su disponibilidad
  no se comprobo y no se presenta como funcional contra el proveedor real.

## Matriz provisional (no es certificacion final de A6)

| Requisito | Estado | Evidencia y pendiente |
|---|---|---|
| Modulos separados, 23 operaciones, sesiones y dos sockets | Cumplido en implementacion | Codigo y pruebas previas A1-A5; falta repetir validacion integral real |
| JDBC y seis tablas | Cumplido para conexion/lectura | Consulta real de esta auditoria |
| Login, cambio de clave y permisos | Parcial | Probados con simulacion; faltan cuentas reales |
| CRUD funcionarios, categorias y recursos | Parcial | Implementado; escritura real pendiente de autorizacion |
| Crear, persistir, consultar y cancelar reserva propia | Parcial | Transporte probado; ciclo JDBC real pendiente |
| Calendarizacion, actividades y estadisticas | Parcial | Implementadas; verificar con reservas reales |
| Alertas a otros funcionarios, Ver y Descartar | Parcial | Pruebas TCP/Swing simuladas aprobadas; falta escenario MySQL con dos usuarios |
| Extraccion IA | Parcial | Contrato probado con simulacion; proveedor no verificado |
| Compilacion desde instalacion limpia | Pendiente A6 | Las compilaciones previas usaron la cache Maven local |
| Pruebas JDBC aisladas | Pendiente | Sin configuracion separada de prueba verificada |
| Entregable sin credenciales/datos locales | Pendiente | Preparar exportacion filtrada; no distribuir JAR local ni XML legado |

Bloqueo inmediato: faltan cuentas y autorizacion para crearlas. La demostracion
de escritura real tambien requiere una autorizacion posterior especifica.
Antes de entregar debe resolverse el empaquetado que excluya secretos locales.
No se identifico en esta auditoria un fallo funcional reproducido que justifique
modificar las reglas de negocio o la arquitectura.

## Intento autorizado de alta: detenido antes de escribir

El operador autorizo exclusivamente las tres cuentas y dos fichas propuestas.
Se preparo un auxiliar temporal en target con formulario privado, parametros
JDBC y validacion transaccional. La comprobacion previa detecto
`general_log=0` y `slow_query_log=1`; el auxiliar se detuvo antes de solicitar
contrasenas, iniciar la transaccion o ejecutar INSERT, para no arriesgar el
registro de parametros sensibles en el log lento del servidor.

No hubo una transaccion de escritura que revertir. La consulta posterior de
solo lectura confirmo los mismos conteos: 0, 0, 1, 1, 0, 0. Las tres cuentas
siguen ausentes. No se modifico la configuracion de registros de MySQL.
Antes de reintentar debe resolverse como impedir el registro de los valores
sensibles durante esta operacion. No se ejecutaron pruebas funcionales.

## Alta confirmada con claves desechables autorizadas

El operador autorizo generar claves exclusivamente para pruebas locales y
acepto expresamente que MySQL pudiera registrarlas. Esta autorizacion sustituye
el bloqueo por slow_query_log descrito en el intento anterior. No se cambio
slow_query_log ni otra configuracion del servidor.

Se comprobo que la direccion JDBC es de loopback, que el hostname de MySQL
corresponde al equipo local y que la base es reservas_proyecto2, identificada
por el operador como entorno de desarrollo. Se genero una clave desechable
independiente por cuenta mediante SecureRandom, sin contrasenas personales.
Las claves se enlazaron con parametros JDBC y se ofrecieron en una ventana
local con campos ocultos; no se incluyeron en archivos, reportes o documentacion.

La transaccion inserto exactamente tres usuarios y dos fichas, sin sobrescribir
registros. Se validaron roles, fichas, nombres, telefonos vacios y conteos antes
de commit y se verificaron nuevamente mediante lectura despues de confirmarlo:

| Cuenta | Rol | Ficha funcionario |
|---|---|---|
| A6-ADMIN | ADMIN | No |
| A6-FUN-A | FUNCIONARIO | Si: Validacion A6 A |
| A6-FUN-B | FUNCIONARIO | Si: Validacion A6 B |

Conteos finales: usuario **3**, funcionario **2**, categoria_recurso **1**,
recurso **1**, reserva **0**, reserva_recurso **0**.

No se modificaron categorias, recursos, reservas ni otras tablas. No hubo
errores ni rollback de las inserciones: COMMIT quedo confirmado. El auxiliar
cerro la conexion despues de verificar; la ventana solo permite consultar las
claves temporales y no ejecuta mas operaciones. No se realizaron pruebas de
login, CRUD o reservas, ni commit o push de Git. La fase funcional queda pendiente
de una autorizacion separada para los registros concretos que vaya a modificar.

## Plan de prueba funcional real: pendiente de autorizacion

Precomprobacion de solo lectura del 2026-11-05: cuentas y roles correctos,
conteos 3, 2, 1, 1, 0, 0. El recurso existente es RES-001, Laboratorio de
Computacion, categoria CAT-001 / Laboratorios. No se consultaron claves.
Se relevaron los reportes A1-A5 y el codigo de creacion/cancelacion antes de
preparar este plan. No se realizaron escrituras en esta fase.

### Unica creacion solicitada

Se realizara desde la pantalla de reservas de A6-FUN-A, mediante ServiceProxy
y Backend, no mediante INSERT SQL directo:

| Campo | Valor propuesto |
|---|---|
| id | RES-000001, previsto segun los datos actuales; lo asigna Backend |
| funcionario_id | A6-FUN-A |
| actividad | A6 - Validacion integral TCP y alertas |
| fecha | 2026-11-06 |
| hora_inicio | 10:00:00 |
| hora_fin | 11:00:00 |
| estado inicial | ACTIVA |
| categoria solicitada | CAT-001 / Laboratorios |
| recurso asignable | RES-001 / Laboratorio de Computacion |

Efecto esperado: una fila nueva en reserva y una fila en reserva_recurso con
reserva_id=RES-000001 y recurso_id=RES-001, guardadas atomicamente por el DAO.
No se modifica la fila del recurso. Antes de crear se volveran a comprobar
conteos, disponibilidad, fecha futura e id previsto. Si cambian las condiciones
o el horario deja de ser futuro, detenerse y revisar el plan antes de escribir.

### Secuencia de verificacion y modificacion

1. Iniciar Backend y Frontend en procesos separados; el classpath Frontend no
   incluira Backend, JDBC ni configuracion MySQL. Usar credenciales locales solo
   en memoria o campos privados, sin escribirlas en logs, reportes o argumentos.
2. Probar login correcto/incorrecto, consultas y permisos. Abrir las nueve
   pantallas: login, cambio de clave, funcionarios, categorias, recursos,
   reservas, calendarizacion, actividades y estadisticas. En cambio de clave
   solo abrir/cerrar el formulario; no guardar modificaciones. En CRUD de
   catalogos y funcionarios, revisar formularios/consultas sin guardar ni borrar.
3. Mantener dos Frontends autenticados A y B, con ASYNC asociado, antes de crear
   la unica reserva indicada. Comprobar persistencia, una alerta en B y ninguna
   alerta propia en A. Ver detalle y descartar localmente en B; verificar que
   la reserva sigue ACTIVA y que su asociacion de recurso permanece.
4. Consultar calendarizacion, actividades y estadisticas para el 2026-11-06 y
   su semana (2026-11-02 a 2026-11-08), comparando con la unica reserva real.
   Cerrar y reabrir el Frontend A; verificar la misma reserva persistida, sin
   volver a crearla.
5. Intentar cancelar esa reserva desde B: debe rechazarse sin cambios en BD.
   Si una autorizacion incorrecta permite cancelarla, detenerse y documentar
   el fallo; no crear otra reserva ni restaurar el estado automaticamente.
6. Tras confirmar el rechazo anterior, cancelar desde A. Unica modificacion
   autorizable: estado de RES-000001, ACTIVA -> CANCELADA. Conservar la fila
   de reserva_recurso y todos los demas campos. Verificar listados/estadisticas
   posteriores segun su filtro de reservas activas.
7. Verificar desconexion/cierre sin solicitudes de escritura pendientes. No
   repetir creaciones para probar desconexion ni ejecutar limpiezas.

Conteos esperados al terminar: usuario 3, funcionario 2, categoria_recurso 1,
recurso 1, reserva 1 (CANCELADA), reserva_recurso 1. Se conservara esa reserva
como evidencia: no se solicita ni se ejecutara su borrado.

Si falla una prueba, registrar el punto y los cambios realmente confirmados,
sin repetir escrituras a ciegas. La creacion confirmada no se revierte al cerrar
Frontend. No alterar usuarios, roles, claves, categorias o recursos, ni ejecutar
scripts de inicializacion o *IT. Este plan no autoriza escrituras adicionales.

### Entrega posterior

Sigue comprobado que el JAR local de Backend contiene reservas.properties y que
data.xml esta versionado. Despues de la prueba funcional se debera corregir y
verificar el empaquetado del JAR, mantener configuracion JDBC fuera del artefacto
distribuido y excluir XML legado, configuraciones locales, .idea y target del
entregable. No se han aplicado esos cambios ni se presenta la entrega como lista.

El reporte final diferenciara pruebas reales aprobadas/fallidas de las simuladas
y pendientes. El CRUD con escritura de catalogos/funcionarios, el cambio efectivo
de clave y la IA real no quedan cubiertos por esta autorizacion limitada.
