PROJECT_CONTEXT.md — EIF206 Programación 3, Proyecto II
Documento de contexto para Codex y para el equipo. No ejecutar cambios automáticamente por leer este archivo. Antes de implementar, inspeccionar el repositorio local y consultar el enunciado oficial. Este documento es una guía, no sustituye al PDF del profesor.

1. Objetivo oficial
   Transformar la aplicación Sistema de Reserva de Recursos (Proyecto I) en una aplicación distribuida con dos procesos independientes: Frontend y Backend, capaces de ejecutarse en máquinas distintas. Múltiples Frontends deben conectarse a un único Backend a través de sockets. El Backend es el único que accede a MySQL. El Frontend sigue MVC y utiliza un Proxy local que representa el Service remoto. El Backend debe notificar asíncronamente a los demás Frontends conectados cada vez que se registre una nueva reserva.
   Requisitos funcionales y ponderación
- Login y cambio de clave: 5 %.
- Reservas: 20 %.
- Funcionarios: 8 %.
- Categorías de recursos: 8 %.
- Recursos: 8 %.
- Calendarización: 8 %.
- Actividades: 8 %.
- Estadísticas: 15 %.
- Alertas: 20 %.
  Alertas: columna a la derecha de la pantalla principal, visible sin importar la pestaña activa; reservas creadas por otros usuarios durante la sesión actual. Deben recibirse asíncronamente; se pueden ver en ventana de detalle y descartar con confirmación. Descartar no elimina la reserva del backend.
  Otras reglas: respetar estrictamente arquitectura distribuida y MVC de clase; persistencia MySQL; validar y reportar errores; entregar proyectos fuente comprimidos junto con script SQL; mantener GitHub compartido actualizado; incluir tests JUnit Jupiter, unitarios con Surefire e integración con Failsafe. Entrega oficial: 1 de noviembre de 2026. Mantener el equipo del Proyecto I.
2. Línea base: Proyecto I
   Repositorio local del usuario: C:\ProyectoReservaDeProductos (verificar ruta local vigente). Rama principal protegida: main; commit de referencia: e49885f (fix: cosas variadas). Rama activa creada para la Parte II: feature/proyecto2.
   Estructura observada en ZIP del Proyecto I:
   src/main/java/reservas/
   Application.java
   logic/
   Service.java, Reserva.java, Recurso.java, CategoriaRecurso.java,
   Funcionario.java, Usuario.java
   data/
   Data.java, XmlPersister.java, LocalDateAdapter.java, LocalTimeAdapter.java
   presentation/
   AbstractModel.java, AbstractTableModel.java, Sesion.java
   login/, cambiarclave/, reservas/, funcionarios/, categorias/,
   recursos/, calendarizacion/, actividades/, estadisticas/
   (cada módulo contiene View/Controller/Model, cuando corresponde)
   ia/
   ReservaExtraccion.java, ReservaExtractorService.java
   src/test/java/reservas/logic/
   ServiceTest.java, ServiceRecursosTest.java,
   ServiceCalendarizacionTest.java, ReservaIT.java
   La persistencia original pasa por Service / Data / XmlPersister hacia data.xml con JAXB. No asumir que es seguro borrar XML de inmediato; conservar como respaldo hasta validar MySQL y los tests. Mantener reglas funcionales de la Parte I.
3. MVC existente y diferencia con el enfoque antiguo
   Las entidades de dominio (logic.Reserva, logic.Funcionario, etc.) no son el mismo Model que el presentation.<pantalla>.Model. El Model de presentación representa el estado observable de la pantalla y extiende AbstractModel.
   Flujo antiguo que se quiere evitar al refrescar la interfaz:
   View -> Controller -> Service -> Controller -> modificación DIRECTA de View
   Flujo implementado del MVC del Proyecto I:
   Usuario / View (listener Swing)
   |
   v
   Controller
   |
   v
   Service
   |
   v
   Controller (recibe resultado)
   |
   v
   Model de presentación (set... / firePropertyChange)
   |
   v
   View (PropertyChangeListener / propertyChange)
   |
   v
   Actualiza JTable, JTextField, etc.
   AbstractModel encapsula PropertyChangeSupport; la View se registra con model.addPropertyChangeListener(this). El Controller cambia propiedades del Model; el Model avisa, no dibuja la View por sí mismo. El Service puede responder al Controller; el punto decisivo es que la View se refresca observando el Model.
4. Arquitectura destino — dependencias previstas
   PROCESO FRONTEND (Swing + MVC)
   View -> Controller -> ServiceProxy (API local de servicio)
   View <- Model <--- Controller (resultados)
   |
   mensajes solicitados
   v
   Socket TCP / Protocol
   |
   v
   PROCESO BACKEND (sin interfaz)
   ServerSocket -> atención de clientes / dispatch -> Service
   -> DAO -> Database/JDBC -> MySQL
   |
   avisos asíncronos
   v
   PROCESO(S) FRONTEND
   receptor de notificaciones -> Model de alertas -> View, actualizado en EDT
   Puntos no negociables:
- Ningún DAO, DriverManager, Connection, credencial o dependencia de MySQL en Frontend.
- Frontend no depende del módulo Backend ni lo invoca directamente.
- Protocol puede albergar DTO/entidades compartidas, contratos y definiciones de mensajes según el ejemplo final; la organización exacta requiere diseño.
- Backend implementa de verdad las operaciones de negocio y la persistencia.
- Las operaciones de sockets/BD no deben bloquear el EDT; las modificaciones de Swing se hacen en EDT (SwingUtilities.invokeLater cuando correspondan).
- Diseñar la concurrencia y respuesta a múltiples clientes, además de avisos de reservas a otros clientes; evitar intercalar mensajes concurrentes sobre un mismo flujo de salida sin sincronización/protocolo definido.
5. Ejemplo del profesor: PersonasModulos.zip
   Contiene tres proyectos Maven separados, cada uno con su pom.xml:
   Personas/
   Frontend/ pom.xml; presentation/; Application.java
   Protocol/ pom.xml; logic/Persona.java, Departamento.java, EstadoCivil.java
   Backend/  pom.xml; data/Database.java, PersonaDao.java,
   DepartamentoDao.java, EstadoCivilDao.java; logic/Service.java
   El código de Frontend/presentation/personas/Controller.java todavía importa personas.logic.Service y llama directamente Service.instance(). El Frontend/pom.xml declara dependencia de Backend además de Protocol. Por tanto este ejemplo no es aún un cliente/servidor distribuido completo. Reutilizar estilo de organización, DAO y Database como material didáctico; no copiar esa dependencia Backend -> Frontend ni atribuirle un Proxy/sockets que no se observan.
   La clase Database del ejemplo carga database.properties y proporciona prepareStatement, executeQuery y executeUpdate. No copiar literalmente su manejo de errores (System.exit) ni contraseñas en cadenas/archivos versionados sin examinar consecuencias. Mantener errores visibles y recursos cerrados.
6. Teoría del profesor ya vista
- 07-BasesDatos-01: tablas, filas/columnas, llave primaria y foránea, relaciones 1:N y N:M, SQL, servidor MySQL.
- 07-BasesDatos-03: JDBC, drivers, Connection, Statement, ResultSet; patrón DAO y Database.
- 08-ProcesosHilos: procesos e hilos, Runnable.run(), Thread.start(), ciclo EDT, SwingUtilities.invokeLater.
- 09-Sockets: TCP/IP, IP/puerto, Socket, ServerSocket, InputStream y OutputStream; protocolo de aplicación necesario sobre bytes.
  Temas exigidos y que aún requieren diseño/ejemplo concreto: Proxy, formato de protocolo de peticiones/respuestas, concurrencia multicliente, alertas asíncronas, pruebas de integración distribuidas.
7. Entorno comprobado
- Windows, IntelliJ IDEA, JDK 25.
- MySQL Server 8.0.46, Command Line Client y Workbench operativos.
- Prueba separada Java/Maven PruebaJDBC: Connector/J 8.4.0; conexión JDBC y consulta a una tabla Funcionario realizadas con éxito.
- La credencial de MySQL se entrega a través de variable de entorno MYSQL_PASSWORD, no debe escribirse ni versionarse. Esta prueba usa una base de datos didáctica; NO confundirla con la base final del Proyecto II.
- Confirmar versiones y herramientas Maven adecuadas en el repositorio real antes de modificar POMs; hubo dificultades históricas con resolución de dependencias.
8. Seguridad de Git y flujo de trabajo
   Estado confirmado por el usuario:
   On branch feature/proyecto2
   nothing to commit, working tree clean
   stash@{0}: On main: Cambios locales pendientes antes del Proyecto II
   El stash contiene .idea/vcs.xml, data.xml, src/main/java/reservas/presentation/cambiarclave/Controller.java. No aplicar/eliminar stash sin revisar diferencias. No reemplazar main ni borrar XML ni archivos de configuración sin autorización.
   Antes de cada bloque: git status, git branch --show-current, git pull cuando corresponda y cuando el árbol esté limpio. Trabajar en incrementos acotados. Después de cada incremento: ejecutar comprobaciones, git diff, git add de rutas explícitas, git commit -m "...", git push -u origin feature/proyecto2 si aún no existe el upstream. Solicitar al usuario autorización antes de acciones destructivas o cambios amplios.
9. Forma de colaborar con Codex
- Explicar al estudiante: problema -> flujo lógico -> clases modificadas -> pruebas, breve y práctico.
- Dividir cada sesión en bloques de 5 pasos, deteniéndose ante errores.
- Seguir términos y estilo del profesor sin inventar reglas. Usar comentarios Java cortos, evitar complejidad innecesaria y conservar comportamiento existente.
- Hacer sólo validaciones necesarias para satisfacer el enunciado y proteger consistencia/errores; el Proyecto II sí exige validar y reportar adecuadamente todos los errores.
- Antes de editar: informar archivos a crear/cambiar y objetivo; inspeccionar código real y dependencias.
- Después de editar: mostrar pruebas ejecutadas, fallos pendientes y comandos de commit/push. No declarar una función lista sin comprobarla.
- No inventar información que no figure en documentos o archivos. Cuando falten detalles de protocolo/concurrencia, proponer opciones y pedir definición.
10. Prioridad de implementación
1. Auditar la rama actual y preservar baseline limpio.
2. Confirmar diseño de entidades y relaciones de la BD mediante lectura real de logic, Service, Data, XML y usos en pantallas; elaborar script SQL versionado.
3. Montar Backend, Protocol, Frontend como proyectos/módulos independientes y ajustar dependencias correctamente; conservar MVC Swing.
4. Implementar Database y DAO en Backend y probar SQL con tests. Migrar funcionalidad sin perder reglas del negocio.
5. Implementar Proxy, servidor, peticiones y respuestas reales por sockets; probar dos Frontends a un Backend.
6. Implementar recepción asíncrona de alertas y actualización EDT, detalle/descarte. Cubrir toda la rúbrica y pruebas JUnit Jupiter Surefire/Failsafe.
   Orden flexible: se puede construir una prueba vertical mínima con una entidad y un socket antes de migrar todos los DAO para verificar arquitectura; elegir según prioridad y avances reales. Ningún atajo para una demostración parcial debe confundirse con cumplir el proyecto entero.
11. Primera instrucción sugerida para Codex
    Lee PROJECT_CONTEXT.md, inspecciona el árbol completo, el pom.xml, las entidades, Service, Data, XmlPersister, los modelos/controladores de presentación y los tests. No cambies archivos todavía. Devuélveme un diagnóstico y un plan mínimo de migración con dependencias y riesgos, identificando qué conservar, qué mover a Frontend/Protocol/Backend y las tablas SQL necesarias. Prioriza un primer cambio verificable y pequeño. Señala cualquier discrepancia entre el estado real de la rama y este archivo.