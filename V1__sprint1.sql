CREATE TABLE rol (
  id_rol SERIAL PRIMARY KEY,
  nombre_rol VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE moneda (
  moneda_codigo_iso VARCHAR(3) PRIMARY KEY,
  nombre_moneda VARCHAR(50) NOT NULL
);

CREATE TABLE modalidad (
  id_modalidad SERIAL PRIMARY KEY,
  nombre_modalidad VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE estado_usuario (
  id_estado SERIAL PRIMARY KEY,
  nombre_estado VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE usuario (
  id_usuario SERIAL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  apellido VARCHAR(100) NOT NULL,
  correo VARCHAR(150) NOT NULL UNIQUE,
  telefono VARCHAR(20) NOT NULL,
  contrasena_hash VARCHAR(255) NOT NULL,
  fecha_registro TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  estado_id INTEGER NOT NULL,

  CONSTRAINT fk_estado_usuario
    FOREIGN KEY (estado_id)
    REFERENCES estado_usuario(id_estado)
    ON DELETE RESTRICT
);

CREATE TABLE estado_asignacion (
  id_estado SERIAL PRIMARY KEY,
  nombre_estado VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE asignacion_rol (
  usuario_id INTEGER NOT NULL,
  rol_id INTEGER NOT NULL,
  fecha_asignacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  estado_id INTEGER NOT NULL,

  PRIMARY KEY (usuario_id, rol_id),

  CONSTRAINT fk_asignacion_usuario
    FOREIGN KEY (usuario_id)
    REFERENCES usuario(id_usuario)
    ON DELETE CASCADE,

  CONSTRAINT fk_asignacion_rol
    FOREIGN KEY (rol_id)
    REFERENCES rol(id_rol)
    ON DELETE RESTRICT,
  
  CONSTRAINT fk_estado_asignacion
    FOREIGN KEY (estado_id)
    REFERENCES estado_asignacion(id_estado)
    ON DELETE RESTRICT
);


CREATE TABLE estado_negocio (
  id_estado SERIAL PRIMARY KEY,
  nombre_estado VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE negocio (
  id_negocio SERIAL PRIMARY KEY,
  propietario_id INTEGER NOT NULL,
  nombre_negocio VARCHAR(250) NOT NULL,
  direccion_negocio VARCHAR(250) NOT NULL,
  telefono VARCHAR(20),
  identificacion_fiscal VARCHAR(20) NOT NULL,
  moneda_codigo_iso VARCHAR(3) NOT NULL,
  estado_id INTEGER NOT NULL,

  CONSTRAINT fk_negocio_propietario
    FOREIGN KEY (propietario_id)
    REFERENCES usuario(id_usuario)
    ON DELETE RESTRICT,
  
  CONSTRAINT fk_negocio_moneda
    FOREIGN KEY (moneda_codigo_iso)
    REFERENCES moneda(moneda_codigo_iso)
    ON DELETE RESTRICT,
  
  CONSTRAINT fk_estado_negocio
    FOREIGN KEY (estado_id)
    REFERENCES estado_negocio(id_estado)
    ON DELETE RESTRICT,
  
  CONSTRAINT chk_longitud_nombre_negocio CHECK (LENGTH(TRIM(nombre_negocio)) >= 3),

  CONSTRAINT chk_longitud_negocio_identificacion_fiscal CHECK (LENGTH(TRIM(identificacion_fiscal)) >= 9)
);

CREATE UNIQUE INDEX idx_negocio_identificacion_fiscal_ci
ON negocio (LOWER(identificacion_fiscal));

CREATE TABLE estado_servicio (
  id_estado SERIAL PRIMARY KEY,
  nombre_estado VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE servicio (
  id_servicio SERIAL PRIMARY KEY,
  negocio_id INTEGER NOT NULL,
  modalidad_id INTEGER NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  descripcion VARCHAR(500) NOT NULL,
  duracion_minutos INTEGER NOT NULL,
  precio NUMERIC(12, 2) NOT NULL,
  estado_id INTEGER NOT NULL,

  CONSTRAINT fk_servicio_negocio
    FOREIGN KEY (negocio_id)
    REFERENCES negocio(id_negocio)
    ON DELETE CASCADE,
  
  CONSTRAINT fk_servicio_modalidad
    FOREIGN KEY (modalidad_id)
    REFERENCES modalidad(id_modalidad)
    ON DELETE RESTRICT,

  CONSTRAINT fk_estado_servicio
    FOREIGN KEY (estado_id)
    REFERENCES estado_servicio(id_estado)
    ON DELETE RESTRICT,
  
  CONSTRAINT chk_servicio_duracion CHECK (duracion_minutos > 0),

  CONSTRAINT chk_servicio_precio CHECK (precio >= 0)
);

CREATE UNIQUE INDEX idx_servicio_negocio_nombre_ci
ON servicio (negocio_id, LOWER(nombre));


INSERT INTO rol (nombre_rol) VALUES
  ('Cliente'),
  ('Proveedor'),
  ('Propietario');

INSERT INTO moneda (moneda_codigo_iso, nombre_moneda) VALUES
  ('COP', 'Peso Colombiano'),
  ('USD', 'Dólar Estadounidense'),
  ('EUR', 'Euro'),
  ('MXN', 'Peso Mexicano'),
  ('ARS', 'Peso Argentino'),
  ('PEN', 'Sol Peruano'),
  ('CLP', 'Peso Chileno');

INSERT INTO modalidad (nombre_modalidad) VALUES
  ('Presencial'),
  ('Virtual');

INSERT INTO estado_usuario (nombre_estado) VALUES
  ('Activo'),
  ('Inactivo');

INSERT INTO estado_asignacion (nombre_estado) VALUES
  ('Activo'),
  ('Inactivo');

INSERT INTO estado_negocio (nombre_estado) VALUES
  ('Activo'),
  ('Inactivo');

INSERT INTO estado_servicio (nombre_estado) VALUES
  ('Asignado'),
  ('No Asignado');