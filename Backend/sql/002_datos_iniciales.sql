-- Sistema de Reservas: datos iniciales de prueba.
-- Ejecutar DESPUES de 001_schema.sql, sobre reservas_proyecto2.
-- No contiene credenciales de servidor; las claves son de prueba.
--
-- ORDEN OBLIGATORIO padre -> hijo. La integridad referencial exige que la
-- fila referenciada por una llave foranea exista ANTES de insertar la fila
-- que la referencia; al contrario MySQL rechaza con el error 1452.
--   usuario -> funcionario -> categoria_recurso -> recurso
--
-- FORMATO DE IDENTIFICADORES. El Backend calcula el siguiente id leyendo
-- los existentes, asi que el formato no es decorativo:
--   categoria_recurso.id  ->  CAT- + 6 digitos   (CAT-000001)
--   reserva.id            ->  RES- + 6 digitos   (RES-000001)
--   usuario.id, funcionario.id, recurso.id  ->  libres, los escribe el usuario
-- Un id como 'LAB' o 'CAT-1' rompe la generacion del siguiente identificador.

USE reservas_proyecto2;

-- 1. USUARIOS. La columna rol solo admite 'ADMIN' o 'FUNCIONARIO' (CHECK).
--    El administrador no necesita ficha de funcionario.
START TRANSACTION;  
INSERT INTO usuario (id, clave, rol) VALUES ('admin', 'admin', 'ADMIN');

--    Cada funcionario necesita primero su usuario, porque funcionario.id es
--    a la vez llave primaria y llave foranea hacia usuario.id.
INSERT INTO usuario (id, clave, rol) VALUES ('111', '111', 'FUNCIONARIO');
INSERT INTO usuario (id, clave, rol) VALUES ('222', '222', 'FUNCIONARIO');

-- 2. FUNCIONARIOS (hijos de usuario).
INSERT INTO funcionario (id, nombre, telefono) VALUES ('111', 'Ana Jimenez', '8888-1111');
INSERT INTO funcionario (id, nombre, telefono) VALUES ('222', 'Bryan Mora', '8888-2222');

-- 3. CATEGORIAS DE RECURSO. Numeracion consecutiva desde CAT-000001.
INSERT INTO categoria_recurso (id, descripcion) VALUES ('CAT-000001', 'Laptop');
INSERT INTO categoria_recurso (id, descripcion) VALUES ('CAT-000002', 'Proyector');
INSERT INTO categoria_recurso (id, descripcion) VALUES ('CAT-000003', 'Sala');

-- 4. RECURSOS (hijos de categoria_recurso). Dos por categoria, para que la
--    busqueda de disponibilidad tenga una alternativa cuando una este ocupada.
INSERT INTO recurso (id, categoria_id, descripcion) VALUES ('LAP-01', 'CAT-000001', 'Laptop Dell Latitude');
INSERT INTO recurso (id, categoria_id, descripcion) VALUES ('LAP-02', 'CAT-000001', 'Laptop HP ProBook');
INSERT INTO recurso (id, categoria_id, descripcion) VALUES ('PRO-01', 'CAT-000002', 'Proyector Epson');
INSERT INTO recurso (id, categoria_id, descripcion) VALUES ('PRO-02', 'CAT-000002', 'Proyector BenQ');
INSERT INTO recurso (id, categoria_id, descripcion) VALUES ('SAL-01', 'CAT-000003', 'Sala de reuniones A');
INSERT INTO recurso (id, categoria_id, descripcion) VALUES ('SAL-02', 'CAT-000003', 'Sala de reuniones B');
COMMIT;  
-- 5. Comprobacion.
SELECT 'usuario' AS tabla, COUNT(*) AS filas FROM usuario
UNION ALL SELECT 'funcionario', COUNT(*) FROM funcionario
UNION ALL SELECT 'categoria_recurso', COUNT(*) FROM categoria_recurso
UNION ALL SELECT 'recurso', COUNT(*) FROM recurso
UNION ALL SELECT 'reserva', COUNT(*) FROM reserva
UNION ALL SELECT 'reserva_recurso', COUNT(*) FROM reserva_recurso;
-- Esperado: 3, 2, 3, 6, 0, 0

-- Para volver a empezar desde cero, en este orden (hijo -> padre, el inverso
-- de la insercion, porque las llaves foraneas son ON DELETE RESTRICT):
--   DELETE FROM reserva_recurso;
--   DELETE FROM reserva;
--   DELETE FROM recurso;
--   DELETE FROM categoria_recurso;
--   DELETE FROM funcionario;
--   DELETE FROM usuario;
