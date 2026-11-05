-- Sistema de Reservas: esquema inicial para MySQL 8.0.16 o posterior.
-- Ejecutar una sola vez sobre una base vacia seleccionada por el operador.
-- No crea bases, usuarios ni datos iniciales; no contiene credenciales.
-- Los limites VARCHAR son decisiones del esquema, no limites del modelo Java.
-- Los identificadores conservan los valores actuales, incluidos CAT- y RES-.
-- Su generacion concurrente corresponde al Backend; no usar MAX(id) + 1.

CREATE TABLE usuario (
    id VARCHAR(64) NOT NULL,
    clave VARCHAR(255) NOT NULL,
    rol VARCHAR(11) NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMIN', 'FUNCIONARIO'))
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_bin;

-- Herencia: cada funcionario comparte su identificador con un usuario.
-- El Backend debe crear/eliminar ambas filas en una transaccion y comprobar
-- que el usuario tenga rol FUNCIONARIO. Un ADMIN no necesita esta ficha.
CREATE TABLE funcionario (
    id VARCHAR(64) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    telefono VARCHAR(50) NOT NULL,
    CONSTRAINT pk_funcionario PRIMARY KEY (id),
    CONSTRAINT fk_funcionario_usuario FOREIGN KEY (id)
        REFERENCES usuario (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_bin;

CREATE TABLE categoria_recurso (
    id VARCHAR(64) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    CONSTRAINT pk_categoria_recurso PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_bin;

CREATE TABLE recurso (
    id VARCHAR(64) NOT NULL,
    categoria_id VARCHAR(64) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    CONSTRAINT pk_recurso PRIMARY KEY (id),
    INDEX ix_recurso_categoria (categoria_id),
    CONSTRAINT fk_recurso_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria_recurso (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_bin;

CREATE TABLE reserva (
    id VARCHAR(64) NOT NULL,
    funcionario_id VARCHAR(64) NOT NULL,
    actividad TEXT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME(6) NOT NULL,
    hora_fin TIME(6) NOT NULL,
    estado VARCHAR(9) NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT pk_reserva PRIMARY KEY (id),
    INDEX ix_reserva_funcionario_fecha (funcionario_id, fecha, hora_inicio),
    INDEX ix_reserva_estado_fecha (estado, fecha),
    CONSTRAINT fk_reserva_funcionario FOREIGN KEY (funcionario_id)
        REFERENCES funcionario (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_reserva_estado CHECK (estado IN ('ACTIVA', 'CANCELADA')),
    CONSTRAINT ck_reserva_horario CHECK (
        hora_inicio >= '00:00:00' AND hora_inicio < '24:00:00'
        AND hora_fin >= '00:00:00' AND hora_fin < '24:00:00'
        AND hora_fin > hora_inicio
    )
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_bin;

CREATE TABLE reserva_recurso (
    reserva_id VARCHAR(64) NOT NULL,
    recurso_id VARCHAR(64) NOT NULL,
    CONSTRAINT pk_reserva_recurso PRIMARY KEY (reserva_id, recurso_id),
    INDEX ix_reserva_recurso_recurso (recurso_id),
    CONSTRAINT fk_reserva_recurso_reserva FOREIGN KEY (reserva_id)
        REFERENCES reserva (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reserva_recurso_recurso FOREIGN KEY (recurso_id)
        REFERENCES recurso (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_bin;

-- Reglas que requieren Service/DAO y transacciones, no solo PK/FK/CHECK:
-- 1. Crear reservas futuras con al menos un recurso y uno por categoria pedida.
-- 2. Comprobar disponibilidad y guardar reserva/asignaciones atomicamente,
--    con bloqueo de los recursos para evitar solapamientos entre clientes.
--    Solo ACTIVA ocupa recursos; los intervalos contiguos son compatibles.
-- 3. Cancelar solo antes del inicio, cambiando estado y conservando asignaciones.
-- 4. Validar permisos, credenciales y rol del funcionario en el Backend.
-- No se restringe fecha al presente: las reservas deben conservar su historia.
-- Las claves no se generan ni se incluyen aqui; su tratamiento corresponde
-- al servicio de autenticacion. Los filtros sin distinguir mayusculas deben
-- implementarse explicitamente en los DAO (la collation preserva identidad).
-- Actividades, calendarizacion y estadisticas se obtienen de estas tablas.
-- Las alertas de la sesion se gestionan mediante notificaciones, no otra tabla.
