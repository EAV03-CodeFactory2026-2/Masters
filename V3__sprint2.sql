-- Normalización de texto para que sea case-insensitive y sin acentos
CREATE OR REPLACE FUNCTION fn_texto_ci(texto TEXT)
RETURNS TEXT
LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$
    SELECT LOWER(
        TRANSLATE(
            BTRIM(texto),
            'áàâäéèêëíìîïóòôöúùûüñçÁÀÂÄÉÈÊËÍÌÎÏÓÒÔÖÚÙÛÜÑÇ',
            'aaaaeeeeiiiioooouuuuncaaaaeeeeiiiioooouuuunc'
        )
    )
$$;

-- Tipo documento
CREATE TABLE tipo_documento (
    id_tipo_documento SERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(50) NOT NULL UNIQUE,

    CONSTRAINT chk_tipo_documento_codigo CHECK (codigo ~ '^[A-Z]{2,10}$')
);

-- Perfil Propietario
CREATE TABLE perfil_propietario (
    usuario_id INTEGER PRIMARY KEY,
    tipo_documento_id INTEGER NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,

    CONSTRAINT fk_perfil_propietario_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario(id_usuario)
        ON DELETE CASCADE,

    CONSTRAINT fk_perfil_propietario_tipo_documento
        FOREIGN KEY (tipo_documento_id)
        REFERENCES tipo_documento(id_tipo_documento)
        ON DELETE RESTRICT,

    CONSTRAINT chk_perfil_propietario_numero_documento
        CHECK (numero_documento = BTRIM(numero_documento) AND LENGTH(numero_documento) > 0)
);

CREATE UNIQUE INDEX idx_perfil_propietario_documento_ci
ON perfil_propietario (tipo_documento_id, LOWER(numero_documento));

-- Perfil Cliente
CREATE TABLE perfil_cliente (
    usuario_id INTEGER PRIMARY KEY,

    CONSTRAINT fk_perfil_cliente_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario(id_usuario)
        ON DELETE CASCADE
);

-- Perfil Proveedor
CREATE TABLE perfil_proveedor (
    usuario_id INTEGER PRIMARY KEY,
    tipo_documento_id INTEGER NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    especialidad VARCHAR(100) NOT NULL,
    descripcion_profesional VARCHAR(500),

    CONSTRAINT fk_perfil_proveedor_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario(id_usuario)
        ON DELETE CASCADE,

    CONSTRAINT fk_perfil_proveedor_tipo_documento
        FOREIGN KEY (tipo_documento_id)
        REFERENCES tipo_documento(id_tipo_documento)
        ON DELETE RESTRICT,

    CONSTRAINT chk_perfil_proveedor_numero_documento
        CHECK (numero_documento = BTRIM(numero_documento) AND LENGTH(numero_documento) > 0),

    CONSTRAINT chk_perfil_proveedor_especialidad
        CHECK (especialidad = BTRIM(especialidad) AND LENGTH(especialidad) BETWEEN 2 AND 100),

    CONSTRAINT chk_perfil_proveedor_descripcion
        CHECK (descripcion_profesional IS NULL OR LENGTH(BTRIM(descripcion_profesional)) > 0)
);

CREATE UNIQUE INDEX idx_perfil_proveedor_documento_ci
ON perfil_proveedor (tipo_documento_id, LOWER(numero_documento));

-- Día Semana
CREATE TABLE dia_semana (
    id_dia_semana SMALLINT PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(20) NOT NULL UNIQUE,

    CONSTRAINT chk_dia_semana_id CHECK (id_dia_semana BETWEEN 1 AND 7)
);

-- Horario Proveedor
CREATE TABLE horario_proveedor (
    id_horario SERIAL PRIMARY KEY,
    proveedor_id INTEGER NOT NULL,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_horario_proveedor_proveedor UNIQUE (proveedor_id),

    CONSTRAINT fk_horario_proveedor_proveedor
        FOREIGN KEY (proveedor_id)
        REFERENCES perfil_proveedor(usuario_id)
        ON DELETE CASCADE
);

CREATE TRIGGER trg_horario_proveedor_fecha_actualizacion
BEFORE UPDATE ON horario_proveedor
FOR EACH ROW
EXECUTE FUNCTION fn_set_fecha_actualizacion();

-- Horario Bloque
CREATE TABLE horario_bloque (
    id_bloque SERIAL PRIMARY KEY,
    horario_id INTEGER NOT NULL,
    dia_semana_id SMALLINT NOT NULL,
    orden SMALLINT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,

    CONSTRAINT fk_horario_bloque_horario
        FOREIGN KEY (horario_id)
        REFERENCES horario_proveedor(id_horario)
        ON DELETE CASCADE,

    CONSTRAINT fk_horario_bloque_dia_semana
        FOREIGN KEY (dia_semana_id)
        REFERENCES dia_semana(id_dia_semana)
        ON DELETE RESTRICT,

    -- Máximo 4 bloques por día (orden 1..4 único por horario y día)
    CONSTRAINT chk_horario_bloque_orden CHECK (orden BETWEEN 1 AND 4),

    CONSTRAINT uk_horario_bloque_orden
        UNIQUE (horario_id, dia_semana_id, orden)
        DEFERRABLE INITIALLY DEFERRED,

    CONSTRAINT chk_horario_bloque_fin_mayor_inicio CHECK (hora_fin > hora_inicio),

    -- Intervalos de 15 minutos y sin segundos
    CONSTRAINT chk_horario_bloque_intervalo_15
        CHECK (EXTRACT(MINUTE FROM hora_inicio)::INTEGER % 15 = 0
            AND EXTRACT(SECOND FROM hora_inicio) = 0
            AND EXTRACT(MINUTE FROM hora_fin)::INTEGER % 15 = 0
            AND EXTRACT(SECOND FROM hora_fin) = 0),

    -- No superposición de bloques por horario y día
    CONSTRAINT ex_horario_bloque_sin_superposicion
        EXCLUDE USING gist (
            (int8range(
                (horario_id::BIGINT * 7 + dia_semana_id - 1) * 86400
                    + EXTRACT(EPOCH FROM hora_inicio)::BIGINT,
                (horario_id::BIGINT * 7 + dia_semana_id - 1) * 86400
                    + EXTRACT(EPOCH FROM hora_fin)::BIGINT,
                '[)')) WITH &&
    )
    DEFERRABLE INITIALLY DEFERRED
);

-- Servicio Proveedor
CREATE TABLE servicio_proveedor (
    servicio_id INTEGER NOT NULL,
    proveedor_id INTEGER NOT NULL,
    fecha_vinculacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (servicio_id, proveedor_id),

    CONSTRAINT fk_servicio_proveedor_servicio
        FOREIGN KEY (servicio_id)
        REFERENCES servicio(id_servicio)
        ON DELETE CASCADE,

    CONSTRAINT fk_servicio_proveedor_proveedor
        FOREIGN KEY (proveedor_id)
        REFERENCES perfil_proveedor(usuario_id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_servicio_proveedor_proveedor_id
ON servicio_proveedor (proveedor_id);

CREATE OR REPLACE FUNCTION fn_sincronizar_estado_servicio()
RETURNS TRIGGER AS $$
DECLARE
    v_servicio_id INTEGER;
    v_estado VARCHAR(20);
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_servicio_id := OLD.servicio_id;
    ELSE
        v_servicio_id := NEW.servicio_id;
    END IF;

    -- Serializa cambios concurrentes sobre el mismo servicio.
    -- NO KEY UPDATE no choca con el KEY SHARE de las FK (evita deadlocks).
    PERFORM 1 FROM servicio WHERE id_servicio = v_servicio_id FOR NO KEY UPDATE;

    IF EXISTS (SELECT 1 FROM servicio_proveedor WHERE servicio_id = v_servicio_id) THEN
        v_estado := 'Asignado';
    ELSE
        v_estado := 'No Asignado';
    END IF;

    UPDATE servicio
        SET estado_id = (SELECT id_estado FROM estado_servicio WHERE nombre_estado = v_estado)
    WHERE id_servicio = v_servicio_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_servicio_proveedor_sync_estado
AFTER INSERT OR DELETE ON servicio_proveedor
FOR EACH ROW
EXECUTE FUNCTION fn_sincronizar_estado_servicio();

-- Catálogos de recurso
CREATE TABLE estado_recurso (
    id_estado SERIAL PRIMARY KEY,
    nombre_estado VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE tipo_recurso (
    id_tipo_recurso SERIAL PRIMARY KEY,
    nombre_tipo VARCHAR(60) NOT NULL UNIQUE
);

-- Recurso
CREATE TABLE recurso (
    id_recurso SERIAL PRIMARY KEY,
    negocio_id INTEGER NOT NULL,
    tipo_recurso_id INTEGER NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    cantidad SMALLINT NOT NULL,
    descripcion VARCHAR(500),
    fecha_creacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    estado_id INTEGER NOT NULL,

    CONSTRAINT fk_recurso_negocio
        FOREIGN KEY (negocio_id)
        REFERENCES negocio(id_negocio)
        ON DELETE RESTRICT,

    CONSTRAINT fk_recurso_tipo_recurso
        FOREIGN KEY (tipo_recurso_id)
        REFERENCES tipo_recurso(id_tipo_recurso)
        ON DELETE RESTRICT,

    CONSTRAINT fk_estado_recurso
        FOREIGN KEY (estado_id)
        REFERENCES estado_recurso(id_estado)
        ON DELETE RESTRICT,

    CONSTRAINT chk_recurso_nombre_no_vacio CHECK (LENGTH(BTRIM(nombre)) > 0),
    
    CONSTRAINT chk_recurso_cantidad CHECK (cantidad BETWEEN 1 AND 999),
    
    CONSTRAINT chk_recurso_descripcion
        CHECK (descripcion IS NULL OR LENGTH(BTRIM(descripcion)) > 0)
);

-- Unicidad de nombre de recurso por negocio
CREATE UNIQUE INDEX idx_recurso_negocio_nombre_ci
ON recurso (negocio_id, fn_texto_ci(nombre));

CREATE TRIGGER trg_recurso_fecha_actualizacion
BEFORE UPDATE ON recurso
FOR EACH ROW
EXECUTE FUNCTION fn_set_fecha_actualizacion();

CREATE OR REPLACE FUNCTION fn_recurso_estado_inicial()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado_id IS NULL THEN
        SELECT id_estado INTO NEW.estado_id
        FROM estado_recurso WHERE nombre_estado = 'Activo';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_recurso_estado_inicial
BEFORE INSERT ON recurso
FOR EACH ROW
EXECUTE FUNCTION fn_recurso_estado_inicial();

INSERT INTO tipo_documento (codigo, nombre) VALUES
    ('CC', 'Cédula de Ciudadanía'),
    ('CE', 'Cédula de Extranjería'),
    ('PA', 'Pasaporte'),
    ('PPT', 'Permiso por Protección Temporal');

INSERT INTO dia_semana (id_dia_semana, codigo, nombre) VALUES
    (1, 'LUNES', 'Lunes'),
    (2, 'MARTES', 'Martes'),
    (3, 'MIERCOLES', 'Miércoles'),
    (4, 'JUEVES', 'Jueves'),
    (5, 'VIERNES', 'Viernes'),
    (6, 'SABADO', 'Sábado'),
    (7, 'DOMINGO', 'Domingo');

INSERT INTO estado_recurso (nombre_estado) VALUES
    ('Activo'),
    ('Inactivo');

INSERT INTO tipo_recurso (nombre_tipo) VALUES
    ('Equipo'),
    ('Espacio');