# Protocolo compartido A1

Este incremento prepara mensajes; no implementa ServiceProxy, Worker persistente,
SocketListener ni el establecimiento de SYNC/ASYNC. IService y las entidades JAXB
permanecen intactos. Frontend y Backend dependen de Protocol, nunca entre si.

## Contrato de operaciones

Los codigos son explicitos y estables; no usar ordinales de enum.
Los nombres de argumentos y resultados siguientes son records anidados en
`Argumentos` y `Resultado`. La tabla refleja las 23 firmas de IService.

| Codigo | Constante | Argumentos | Resultado |
|---:|---|---|---|
| 1 | `FIND_ALL_CATEGORIAS` | Sin argumentos (null) | Categorias |
| 2 | `LOGIN` | Login | Usuario |
| 3 | `CHANGE_PASSWORD` | CambioClave | Confirmacion |
| 4 | `CREATE_FUNCIONARIO` | Funcionario | Confirmacion |
| 5 | `UPDATE_FUNCIONARIO` | Funcionario | Confirmacion |
| 6 | `DELETE_FUNCIONARIO` | Funcionario | Confirmacion |
| 7 | `SEARCH_FUNCIONARIO` | Funcionario | Funcionarios |
| 8 | `CREATE_CATEGORIA` | Categoria | Confirmacion |
| 9 | `UPDATE_CATEGORIA` | Categoria | Confirmacion |
| 10 | `DELETE_CATEGORIA` | Categoria | Confirmacion |
| 11 | `SEARCH_CATEGORIA` | Categoria | Categorias |
| 12 | `CREATE_RECURSO` | Recurso | Confirmacion |
| 13 | `UPDATE_RECURSO` | Recurso | Confirmacion |
| 14 | `DELETE_RECURSO` | Recurso | Confirmacion |
| 15 | `SEARCH_RECURSOS` | BuscarRecursos | Recursos |
| 16 | `FIND_RESERVAS` | Funcionario | Reservas |
| 17 | `CREAR_RESERVA` | CrearReserva | Reserva |
| 18 | `CANCELAR_RESERVA` | Reserva | Confirmacion |
| 19 | `FIND_RESERVAS_ACTIVAS_POR_FECHA` | Fecha | Reservas |
| 20 | `FIND_RESERVAS_ACTIVAS_POR_SEMANA` | Periodo | Reservas |
| 21 | `ESTADISTICAS_RECURSOS_POR_CATEGORIA` | Periodo | EstadisticasCategorias |
| 22 | `ESTADISTICAS_ACTIVIDADES_POR_SEMANA` | Periodo | EstadisticasSemanas |
| 23 | `EXTRAER_RESERVA` | ExtraerReserva | Extraccion |

Login lleva id y clave; CambioClave lleva id, claveActual y claveNueva. Sus
toString omiten credenciales. Ningun tipo de resultado admite estos argumentos.
Los CRUD reciben el DTO correspondiente. BuscarRecursos incluye categoria y
descripcion. CrearReserva incluye funcionario, actividad, fecha, horaInicio,
horaFin y categoriasSolicitadas. Fecha, Periodo y ExtraerReserva llevan,
respectivamente, una fecha, desde/hasta y frase.

Solicitud valida la correspondencia entre codigo y tipo de argumentos tanto al
construirse como al deserializarse. Se conserva Solicitud(int) para la consulta
sin argumentos de categorias. Codigos desconocidos ahora se rechazan al construir
o deserializar el mensaje; el futuro Worker debe traducir errores de formato.

Respuesta exige exactamente un resultado o error. exito(operacion, resultado)
comprueba el tipo esperado; Confirmacion representa el exito de operaciones void.
fallo(codigo, mensaje) transporta un CodigoError y texto controlado, nunca Throwable.
El futuro Worker debe seleccionar mensajes publicos: no pasar trazas, SQL,
credenciales ni mensajes internos sin revisar. El futuro Proxy debe validar el
tipo recibido contra la operacion pendiente mediante validarResultado.

## DTO y conversiones

- UsuarioDto: id, rol y ficha FuncionarioDto opcional. Conserva el subtipo real
  devuelto por login, necesario para el cast de la pantalla de reservas.
- FuncionarioDto: id, rol, nombre y telefono; ninguna clave.
- CategoriaDto existente: id y descripcion.
- RecursoDto: id, categoria completa y descripcion.
- ReservaDto: id, funcionario completo, actividad, LocalDate, dos LocalTime,
  recursos completos (cada uno con categoria) y estado.
- ReservaExtraccionDto: actividad, fecha y horas como texto, categorias como texto.
  Se preservan campos null provenientes de una extraccion incompleta.
- CantidadCategoria y CantidadSemana: clave y cantidad long para estadisticas.

ConvertidorDto convierte en ambas direcciones, sin acceder a servicios o BD.
Las entidades reconstruidas tienen clave vacia. Los campos y las relaciones se
conservan por identificador y contenido; no se promete identidad Java (==) entre
objetos reconstruidos en mensajes distintos. Los DTO son snapshots, no objetos
vinculados a una sesion ORM.

En el transporte se usan arreglos tipados con copias defensivas. En el dominio
se reconstruyen listas mutables y TreeMap para estadisticas. No se serializan
entidades JAXB, implementaciones arbitrarias de List/Map ni clases de Backend.
LocalDate y LocalTime usan la serializacion del JDK, incluida java.time.Ser.

El DAO actual de funcionarios fija la clave inicial al identificador y update
solo cambia nombre/telefono; por eso sus argumentos no necesitan credenciales.
Solo login y changePassword las necesitan con el IService actual.

## Compatibilidad y filtros

Se conservan serialVersionUID = 1, Solicitud(int),
Respuesta(CategoriaDto[], String), categorias() y error(). En la consulta antigua,
los campos nuevos quedan null. exito con Resultado.Categorias tambien utiliza
el campo antiguo categorias. Worker y ClienteCategorias no fueron modificados:
sus filtros restrictivos originales siguen admitiendo este intercambio.
No se promete que clientes antiguos entiendan las nuevas operaciones o errores
con CodigoError; esas rutas pertenecen al futuro cliente ampliado.

FiltrosProtocolo proporciona filtros separados para solicitudes, respuestas e
identificacion. Permite solo clases DTO concretas, records del contrato cerrado,
enums necesarios y tipos de fecha/hora concretos; rechaza otras clases.
Limites: profundidad 24, 100000 referencias, arreglos de hasta 10000 elementos
y 4 MiB observados por el filtro. No es una proteccion completa de transporte:
en A2 se deben definir limites de mensajes y tiempos, incluido el tratamiento
de cadenas, y considerar que contadores del filtro son acumulativos por stream
en conexiones persistentes. No ampliar a comodines de paquetes ni desactivar filtros.

## Identificacion futura (sin conexiones implementadas)

Nota posterior: A2 concreta este flujo en [Backend/README-A2.md](../Backend/README-A2.md).
SesionCreada ahora acusa la sesion tecnica al abrir SYNC; solo LOGIN autentica
y solo entonces se permite asociar ASYNC. La secuencia siguiente conserva la
propuesta historica de A1.

1. El cliente abre SYNC y envia Identificacion(SYNC, null).
2. Se autentica mediante LOGIN; Backend genera un identificador opaco e
   impredecible, guarda la asociacion con el usuario y emite SesionCreada.
3. El segundo socket envia Identificacion(ASYNC, sesionId). Backend valida
   existencia, pertenencia y vigencia antes de asociarlo al mismo usuario.
4. La sesion debe invalidarse al cerrar; un id declarado no autentica por si solo.

El orden exacto de los acuses y los errores de apertura se implementara en A2.
SYNC se plantea con una solicitud pendiente por conexion, respuestas en orden;
si se permiten solicitudes simultaneas se necesitara correlacion adicional.
ASYNC no debe compartir ObjectOutputStream con las respuestas SYNC.
La actualizacion Swing mediante invokeLater corresponde al futuro SocketListener.

## Verificacion y limites pendientes

- ProtocoloTest: round-trip con filtros de todos los DTO y las 23 parejas de
  argumentos/resultados; codigos unicos; conversiones y relaciones; ausencia de
  claves; copias defensivas; errores; identificacion; rechazo de clases ajenas
  y compatibilidad con los filtros originales.
- Maven: Protocol clean install; Backend clean test; Frontend clean test.
  Protocol agrega exclusivamente JUnit en scope test y Surefire para ejecutarlo.
- La comprobacion TCP real requiere la configuracion JDBC local de Backend.
  No ejecutar las pruebas *IT para esta comprobacion: limpian tablas de prueba.

Las operaciones void confirman exito, no trasladan mutaciones del argumento
Java original (por ejemplo, ids asignados por create); el futuro Proxy debe
respetar el flujo de recarga del MVC o definir explicitamente ese comportamiento.
Las consultas actuales de Service pueden convertir errores de BD en listas
vacias: el protocolo no puede distinguirlas de una consulta sin filas sin
cambiar esa logica, que queda fuera de A1.

## Cierre de verificacion A1 (2026-11-05)

- Protocol: clean install correcto, 7 pruebas sin fallos.
- Backend: clean test correcto, 21 pruebas unitarias sin fallos; no se ejecutaron
  las pruebas de integracion que limpian tablas.
- Frontend: clean test correcto, incluida instrumentacion de formularios;
  no hay pruebas unitarias propias en ese modulo.
- JDBC conectado a reservas_proyecto2. Solo se ejecutaron SELECT.
- Servidor y ClienteCategorias ejecutados como procesos Java independientes
  (PID 8068 y 30148, puerto local temporal 56400). Cliente finalizo con codigo 0.
- findAllCategorias devolvio una categoria: CAT-001, Laboratorios; el contenido
  coincide exactamente con la consulta JDBC directa. Ambos stderr quedaron vacios.
- Conteos antes y despues identicos: usuario 0, funcionario 0, categoria_recurso 1,
  recurso 1, reserva 0, reserva_recurso 0.
- reservas.properties esta ignorado por Git y no esta versionado. No se muestran
  ni se incorporan credenciales en este reporte.

## Preparacion posterior de usuarios (no ejecutada)

Con usuario vacio no puede probarse el login. Esto no impide la consulta de
categorias del prototipo actual, que todavia no autentica conexiones.

No ejecutar completo 002_datos_iniciales.sql sobre esta base: tambien inserta
categorias y recursos y utiliza claves de demostracion. Preparar una operacion
separada, revisable, limitada a usuario y funcionario:

1. Confirmar la base seleccionada y revisar ids existentes sin consultar claves.
2. Elegir un administrador de la aplicacion y los funcionarios necesarios.
   Usar claves temporales unicas suministradas localmente, no guardadas en Git
   ni copiadas en reportes; no reutilizar la credencial de conexion de MySQL.
3. Dentro de una transaccion, insertar el administrador en usuario con rol ADMIN.
   No necesita fila en funcionario.
4. Para cada funcionario, insertar primero usuario con rol FUNCIONARIO y luego
   funcionario con el mismo id, nombre y telefono.
5. Verificar ids, roles y relaciones sin mostrar claves; confirmar solo si todo
   coincide con lo previsto, y hacer rollback ante cualquier error.

El Service actual compara claves directamente: no introducir hashes en la tabla
sin adaptar antes la autenticacion. El almacenamiento seguro con hash requiere
otro cambio autorizado, fuera de A1. El alta normal de FuncionarioDao asigna
la clave inicial igual al id; esas cuentas deben cambiarla antes del uso habitual.
Este documento no ejecuta ni autoriza las inserciones.
