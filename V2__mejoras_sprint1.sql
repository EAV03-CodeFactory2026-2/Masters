CREATE OR REPLACE FUNCTION fn_set_fecha_actualizacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

ALTER TABLE usuario ADD COLUMN fecha_actualizacion TIMESTAMP WITH TIME ZONE;
UPDATE usuario SET fecha_actualizacion = fecha_registro;

-- Columna de auditoría para la tabla usuario
ALTER TABLE usuario
    ALTER COLUMN fecha_actualizacion SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN fecha_actualizacion SET NOT NULL,
    ALTER COLUMN telefono TYPE VARCHAR(15);

CREATE TRIGGER trg_usuario_fecha_actualizacion
BEFORE UPDATE ON usuario
FOR EACH ROW
EXECUTE FUNCTION fn_set_fecha_actualizacion();

-- Correo Único Case-Insensitive
CREATE UNIQUE INDEX idx_usuario_correo_ci ON usuario (LOWER(correo));
ALTER TABLE usuario DROP CONSTRAINT usuario_correo_key;

-- Restricciones de formato para la tabla usuario
ALTER TABLE usuario
    ADD CONSTRAINT chk_usuario_correo_formato
        CHECK (correo = BTRIM(correo) AND correo ~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$'),
    
    ADD CONSTRAINT chk_usuario_nombre
        CHECK (LENGTH(nombre) BETWEEN 2 AND 100
        AND nombre ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+( [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$'),
    
    ADD CONSTRAINT chk_usuario_apellido
        CHECK (LENGTH(apellido) BETWEEN 1 AND 100
        AND apellido ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+( [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$'),
    
    ADD CONSTRAINT chk_usuario_telefono
        CHECK (telefono ~ '^[0-9]{7,15}$');

-- Moneda
ALTER TABLE moneda
    ADD CONSTRAINT chk_moneda_codigo_iso CHECK (moneda_codigo_iso ~ '^[A-Z]{3}$');

-- Negocio
ALTER TABLE negocio DROP CONSTRAINT fk_negocio_propietario;

ALTER TABLE negocio
    ADD CONSTRAINT fk_negocio_propietario
    FOREIGN KEY (propietario_id)
    REFERENCES perfil_propietario (usuario_id)
    ON DELETE RESTRICT;

ALTER TABLE negocio
    DROP CONSTRAINT chk_longitud_negocio_identificacion_fiscal,
    ADD CONSTRAINT chk_negocio_identificacion_fiscal_formato
        CHECK (identificacion_fiscal ~ '^[A-Za-z0-9]{9,20}$'),
    ADD CONSTRAINT chk_negocio_direccion_no_vacia
        CHECK (LENGTH(BTRIM(direccion_negocio)) > 0);

CREATE INDEX idx_negocio_propietario_id ON negocio (propietario_id);

-- Servicio
-- Unicidad por negocio Case-Insensitive
DROP INDEX idx_servicio_negocio_nombre_ci;
CREATE UNIQUE INDEX idx_servicio_negocio_nombre_ci
    ON servicio (negocio_id, LOWER(BTRIM(nombre)));

ALTER TABLE servicio
    ADD CONSTRAINT chk_servicio_nombre_no_vacio
        CHECK (LENGTH(BTRIM(nombre)) > 0),
    ADD CONSTRAINT chk_servicio_descripcion_no_vacia
        CHECK (LENGTH(BTRIM(descripcion)) > 0);

-- No permitir eliminar un negocio si tiene servicios asociados
ALTER TABLE servicio
    DROP CONSTRAINT fk_servicio_negocio,
    ADD CONSTRAINT fk_servicio_negocio
        FOREIGN KEY (negocio_id)
        REFERENCES negocio(id_negocio) ON DELETE RESTRICT;

-- Índice para filtrar usuario por rol
CREATE INDEX idx_asignacion_rol_rol_estado ON asignacion_rol (rol_id, estado_id);

-- Estados iniciales automáticos
CREATE OR REPLACE FUNCTION fn_negocio_estado_inicial()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado_id IS NULL THEN
        SELECT id_estado INTO NEW.estado_id
        FROM estado_negocio WHERE nombre_estado = 'Activo';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_negocio_estado_inicial
BEFORE INSERT ON negocio
FOR EACH ROW
EXECUTE FUNCTION fn_negocio_estado_inicial();


CREATE OR REPLACE FUNCTION fn_servicio_estado_inicial()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado_id IS NULL THEN
        SELECT id_estado INTO NEW.estado_id
        FROM estado_servicio WHERE nombre_estado = 'No Asignado';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_servicio_estado_inicial
BEFORE INSERT ON servicio
FOR EACH ROW
EXECUTE FUNCTION fn_servicio_estado_inicial();