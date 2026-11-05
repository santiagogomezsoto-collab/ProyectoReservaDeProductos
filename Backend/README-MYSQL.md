# Backend: puesta en marcha con MySQL

Persistencia del Proyecto II. El Backend es el **único** proceso que se conecta
a MySQL; ni el Frontend ni Protocol tienen dependencias de base de datos.

## 1. Requisitos

| | Versión usada |
|---|---|
| MySQL Server | 8.4.11 |
| Driver JDBC (Connector/J) | 8.4.0 (lo baja Maven) |
| JDK | 25 de desarrollo, compilando para Java 17 |

## 2. Crear la base y las tablas

En MySQL Workbench, en este orden:

```sql
CREATE DATABASE reservas_proyecto2
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
```

Después ejecutar, en orden:

1. `sql/001_schema.sql` — las 6 tablas con sus llaves y restricciones
2. `sql/002_datos_iniciales.sql` — datos de prueba (admin, categorías, recursos)

El segundo script termina con un `SELECT` de comprobación. Debe mostrar:
`usuario 3`, `funcionario 2`, `categoria_recurso 3`, `recurso 6`, `reserva 0`,
`reserva_recurso 0`.

**Usuarios de prueba:** `admin` / `admin` (rol ADMIN), `111` / `111` y
`222` / `222` (rol FUNCIONARIO).

## 3. Configurar la conexión

Copiar la plantilla y escribir la clave propia:

```
src/main/resources/reservas.properties.example  ->  src/main/resources/reservas.properties
```

```properties
database_driver=com.mysql.cj.jdbc.Driver
database_server=localhost
database_port=3306
database_name=reservas_proyecto2
database_user=root
database_password=LA_CLAVE_DE_CADA_UNO
```

La ruta importa: `Database` busca el archivo en el **classpath**, y Maven solo
publica allí lo que esté bajo `src/main/resources`.

`reservas.properties` está excluido por `.gitignore` y **no debe subirse**: lleva
la clave de MySQL. Lo que se versiona es la plantilla `.example`.

## 4. Compilar

Desde el panel Maven de IntelliJ (en Windows `mvn` puede no estar en el PATH),
en este orden, porque Backend depende de Protocol:

1. `Protocol` → `install`
2. `Backend` → `install`

## 5. Pruebas

| Comando | Qué corre | Necesita MySQL |
|---|---|---|
| `mvn test` | Surefire: 19 pruebas unitarias | No |
| `mvn verify` | Surefire + Failsafe: 19 + 26 pruebas | **Sí, y debe ser la base `_test`** |

Unitarias (sin MySQL): `ReglasReservaTest` (15) y `DatabaseConfiguracionTest` (4).
Integración (con MySQL): `ServiceIT` (12), `ServiceRecursosIT` (6),
`ServiceCalendarizacionIT` (5) y `ReservaIT` (3).

Las reglas que no consultan datos (categorías vacías, hora de fin anterior a la
de inicio, fecha pasada, solape de horarios) están en `ReglasReserva` y se
prueban sin base de datos. Todo lo que necesita consultar datos se prueba con
las clases `*IT`, que dejan la base vacía antes de cada prueba (`PruebaBase`)
para que cada una empiece desde un estado conocido.

### Base de datos de pruebas

Las pruebas de integración **insertan y borran datos**. Para no tocar la base
real, crear una base aparte y apuntar las pruebas a ella:

```sql
CREATE DATABASE reservas_proyecto2_test
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
```

Ejecutar `001_schema.sql` sobre ella, copiar
`reservas-test.properties.example` como `reservas-test.properties` con
`database_name=reservas_proyecto2_test`, y correr:

```
mvn verify -Dreservas.db.properties=/reservas-test.properties
```

**No hace falta recordarlo:** las pruebas de integración comprueban el nombre de
la base antes de borrar nada, y si no termina en `_test` se detienen con un
mensaje que explica qué falta. O sea que `mvn verify` sin esa propiedad falla a
propósito, en vez de borrar los datos de desarrollo.

## 6. Arquitectura de la capa de datos

```
Service  (reglas de negocio, no sabe SQL)
   |
   +-- UsuarioDao          usuario
   +-- FuncionarioDao      funcionario  (+ usuario, en transacción)
   +-- CategoriaRecursoDao categoria_recurso
   +-- RecursoDao          recurso      (+ su categoría, con alias en el SELECT)
   +-- ReservaDao          reserva + reserva_recurso  (en transacción)
                |
           Database  (Singleton: una conexión, métodos synchronized)
                |
             MySQL
```

### Decisiones y su motivo

- **`Database` es un Singleton con una sola conexión**, como el ejemplo de
  clase. Sus métodos son `synchronized` porque este Backend atiende a varios
  Frontend con un pool de hilos, y una `Connection` de JDBC no es segura entre
  hilos.
- **Los errores de SQL se propagan** con su causa en las operaciones que
  modifican datos. Las consultas de `IService` no declaran `throws`, así que
  ahí un fallo se informa por consola y se devuelve una colección vacía.
- **`Database` expone transacciones** (`beginTransaction` / `commit` /
  `rollback`), que el ejemplo de clase no necesitaba. Las usan dos operaciones:
  crear o borrar un funcionario (dos tablas por la herencia
  `usuario`/`funcionario`) y crear una reserva (`reserva` más una fila por
  recurso en `reserva_recurso`).
- **Crear una reserva se sincroniza de punta a punta** sobre
  `reservaDao.lock()`. Comprobar disponibilidad y guardar no pueden separarse:
  si se separan, dos Frontend pueden recibir el mismo recurso libre. El bloqueo
  cubre los hilos de este Backend, que es el único proceso que escribe. Con dos
  Backend haría falta además `SELECT ... FOR UPDATE`.
- **Los identificadores conservan su formato**: `CAT-000001` y `RES-000001`. El
  siguiente se calcula con `max(cast(substring(id, 5) as unsigned)) + 1` dentro
  del mismo bloque sincronizado que la inserción.

## 7. Pendiente

- `Data.java` y `XmlPersister.java` quedaron sin uso (eran la persistencia en
  XML). Se conservan hasta validar MySQL contra la aplicación completa; después
  se pueden eliminar junto con las dependencias de JAXB del POM.
- `Service.resetForTesting(String)` conserva un parámetro que ya no se usa (era
  la ruta del XML). Se mantiene solo por compatibilidad y se puede eliminar
  cuando nadie lo llame.
