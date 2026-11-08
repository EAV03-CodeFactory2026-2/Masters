CREATE TABLE estado_reserva (
  id_estado SMALLINT PRIMARY KEY,
  nombre_estado VARCHAR(20) NOT NULL UNIQUE
);

INSERT INTO estado_reserva (id_estado, nombre_estado) VALUES
  (1, 'Confirmada'),
  (2, 'Cancelada');

-- Para garantizar recurso y servicio del mismo negocio
ALTER TABLE servicio ADD CONSTRAINT uk_servicio_id_negocio UNIQUE (id_servicio, negocio_id);
ALTER TABLE recurso ADD CONSTRAINT uk_recurso_id_negocio UNIQUE (id_recurso, negocio_id);

CREATE TABLE servicio_recurso (
  servicio_id INTEGER NOT NULL,
  recurso_id INTEGER NOT NULL,
  negocio_id INTEGER NOT NULL,
  cantidad_requerida SMALLINT NOT NULL,
  fecha_creacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  fecha_actualizacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

  PRIMARY KEY (servicio_id, recurso_id),

  CONSTRAINT fk_servicio_recurso_servicio
    FOREIGN KEY (servicio_id, negocio_id)
    REFERENCES servicio(id_servicio, negocio_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_servicio_recurso_recurso
    FOREIGN KEY (recurso_id, negocio_id)
    REFERENCES recurso(id_recurso, negocio_id)
    ON DELETE RESTRICT,

  CONSTRAINT chk_servicio_recurso_cantidad
    CHECK (cantidad_requerida BETWEEN 1 AND 999)
);

CREATE INDEX idx_servicio_recurso_recurso_id ON servicio_recurso (recurso_id);

CREATE TRIGGER trg_servicio_recurso_fecha_actualizacion
BEFORE UPDATE ON servicio_recurso
FOR EACH ROW
EXECUTE FUNCTION fn_set_fecha_actualizacion();

CREATE OR REPLACE FUNCTION fn_servicio_recurso_validar()
RETURNS TRIGGER AS $$
DECLARE
  v_negocio_id INTEGER;
  v_nombre VARCHAR(100);
  v_cantidad SMALLINT;
BEGIN
  SELECT negocio_id INTO v_negocio_id FROM servicio WHERE id_servicio = NEW.servicio_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'El servicio no existe.';
  END IF;
  NEW.negocio_id := v_negocio_id;

  -- Impedir que otra transacción reduzca las unidades del recurso a la vez.
  SELECT nombre, cantidad INTO v_nombre, v_cantidad
  FROM recurso WHERE id_recurso = NEW.recurso_id FOR SHARE;

  IF FOUND AND NEW.cantidad_requerida > v_cantidad THEN
    RAISE EXCEPTION 'El recurso % solo tiene % unidades.', v_nombre, v_cantidad;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_servicio_recurso_validar
BEFORE INSERT OR UPDATE ON servicio_recurso
FOR EACH ROW
EXECUTE FUNCTION fn_servicio_recurso_validar();

CREATE OR REPLACE FUNCTION fn_recurso_validar_cantidad_requerida()
RETURNS TRIGGER AS $$
BEGIN
  IF EXISTS (SELECT 1 FROM servicio_recurso WHERE recurso_id = NEW.id_recurso AND cantidad_requerida > NEW.cantidad) THEN
    RAISE EXCEPTION 'No puedes reducir las unidades del recurso % por debajo de las requeridas por sus servicios.', NEW.nombre;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_recurso_validar_cantidad_requerida
BEFORE UPDATE OF cantidad ON recurso
FOR EACH ROW
WHEN (NEW.cantidad < OLD.cantidad)
EXECUTE FUNCTION fn_recurso_validar_cantidad_requerida();

CREATE TABLE franja_servicio (
  id_franja SERIAL PRIMARY KEY,
  servicio_id INTEGER NOT NULL,
  proveedor_id INTEGER NOT NULL,
  dia_semana_id SMALLINT NOT NULL,
  hora_inicio TIME NOT NULL,
  creada_por INTEGER NOT NULL,
  fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT uk_franja_servicio_slot UNIQUE (servicio_id, proveedor_id, dia_semana_id, hora_inicio),

  CONSTRAINT fk_franja_servicio_asignacion
    FOREIGN KEY (servicio_id, proveedor_id)
    REFERENCES servicio_proveedor(servicio_id, proveedor_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_franja_servicio_dia
    FOREIGN KEY (dia_semana_id)
    REFERENCES dia_semana(id_dia_semana)
    ON DELETE RESTRICT,

  CONSTRAINT fk_franja_servicio_creada_por
    FOREIGN KEY (creada_por)
    REFERENCES usuario(id_usuario)
    ON DELETE RESTRICT,

  CONSTRAINT chk_franja_hora_sin_segundos
    CHECK (EXTRACT(SECOND FROM hora_inicio) = 0),

  CONSTRAINT chk_franja_hora_valida
    CHECK (hora_inicio < TIME '24:00:00')
);

CREATE INDEX idx_franja_servicio_proveedor_dia ON franja_servicio (proveedor_id, dia_semana_id);
CREATE INDEX idx_franja_servicio_creada_por ON franja_servicio (creada_por);

CREATE OR REPLACE FUNCTION fn_franja_servicio_validar()
RETURNS TRIGGER AS $$
DECLARE
  v_duracion INTEGER;
  v_inicio_s INTEGER;
BEGIN
  SELECT duracion_minutos INTO v_duracion FROM servicio WHERE id_servicio = NEW.servicio_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'El servicio no existe.';
  END IF;

  -- Serializa a quienes definen franjas del mismo servicio y proveedor para evitar cruces por concurrencia
  PERFORM 1 FROM servicio_proveedor
  WHERE servicio_id = NEW.servicio_id AND proveedor_id = NEW.proveedor_id
  FOR NO KEY UPDATE;

  v_inicio_s := EXTRACT(EPOCH FROM NEW.hora_inicio)::INTEGER;

  IF NOT EXISTS (
    SELECT 1
    FROM horario_proveedor h
    JOIN horario_bloque b ON b.horario_id = h.id_horario
    WHERE h.proveedor_id = NEW.proveedor_id
      AND b.dia_semana_id = NEW.dia_semana_id
      AND v_inicio_s >= EXTRACT(EPOCH FROM b.hora_inicio)
      AND v_inicio_s + v_duracion * 60 <= EXTRACT(EPOCH FROM b.hora_fin)
  ) THEN
    RAISE EXCEPTION 'La franja debe estar completa dentro de tu horario de atención.';
  END IF;

  IF EXISTS (
    SELECT 1 FROM franja_servicio f
    WHERE f.servicio_id = NEW.servicio_id
      AND f.proveedor_id = NEW.proveedor_id
      AND f.dia_semana_id = NEW.dia_semana_id
      AND f.id_franja <> NEW.id_franja
      AND ABS(EXTRACT(EPOCH FROM (f.hora_inicio - NEW.hora_inicio))) < v_duracion * 60
  ) THEN
    RAISE EXCEPTION 'Esta franja se cruza con otra ya definida.';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_franja_servicio_validar
BEFORE INSERT OR UPDATE ON franja_servicio
FOR EACH ROW
EXECUTE FUNCTION fn_franja_servicio_validar();

CREATE OR REPLACE FUNCTION fn_servicio_validar_duracion_con_franjas()
RETURNS TRIGGER AS $$
BEGIN
  IF EXISTS (SELECT 1 FROM franja_servicio WHERE servicio_id = NEW.id_servicio) THEN
    RAISE EXCEPTION 'No puedes cambiar la duración de un servicio que tiene franjas definidas. Elimínalas primero.';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_servicio_validar_duracion_con_franjas
BEFORE UPDATE OF duracion_minutos ON servicio
FOR EACH ROW
WHEN (NEW.duracion_minutos IS DISTINCT FROM OLD.duracion_minutos)
EXECUTE FUNCTION fn_servicio_validar_duracion_con_franjas();

CREATE SEQUENCE seq_codigo_reserva;

CREATE TABLE reserva (
  id_reserva SERIAL PRIMARY KEY,
  codigo_reserva VARCHAR(20) NOT NULL DEFAULT ('RSV-' || LPAD(nextval('seq_codigo_reserva')::TEXT, 8, '0')),
  cliente_id INTEGER NOT NULL,
  proveedor_id INTEGER NOT NULL,
  servicio_id INTEGER NOT NULL,
  inicio TIMESTAMP NOT NULL,
  fin TIMESTAMP NOT NULL,
  notas VARCHAR(250),
  nombre_servicio VARCHAR(100) NOT NULL,
  duracion_minutos INTEGER NOT NULL,
  precio_servicio NUMERIC(12, 2) NOT NULL,
  moneda_codigo_iso VARCHAR(3) NOT NULL,
  estado_id SMALLINT NOT NULL DEFAULT 1,
  fecha_creacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  fecha_actualizacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

  CONSTRAINT uk_reserva_codigo UNIQUE (codigo_reserva),

  CONSTRAINT uk_reserva_id_estado UNIQUE (id_reserva, estado_id),

  CONSTRAINT fk_reserva_cliente
    FOREIGN KEY (cliente_id)
    REFERENCES perfil_cliente(usuario_id)
    ON DELETE RESTRICT,

  CONSTRAINT fk_reserva_proveedor
    FOREIGN KEY (proveedor_id)
    REFERENCES perfil_proveedor(usuario_id)
    ON DELETE RESTRICT,

  CONSTRAINT fk_reserva_servicio
    FOREIGN KEY (servicio_id)
    REFERENCES servicio(id_servicio)
    ON DELETE RESTRICT,

  CONSTRAINT fk_reserva_moneda
    FOREIGN KEY (moneda_codigo_iso)
    REFERENCES moneda(moneda_codigo_iso)
    ON DELETE RESTRICT,

  CONSTRAINT fk_reserva_estado
    FOREIGN KEY (estado_id)
    REFERENCES estado_reserva(id_estado)
    ON DELETE RESTRICT,

  CONSTRAINT chk_reserva_fin_mayor_inicio
    CHECK (fin > inicio),

  CONSTRAINT chk_reserva_minutos_exactos
    CHECK (inicio = DATE_TRUNC('minute', inicio) AND fin = DATE_TRUNC('minute', fin)),

  CONSTRAINT chk_reserva_rango_fechas
    CHECK (inicio >= TIMESTAMP '2000-01-01' AND fin <= TIMESTAMP '2200-01-01'),

  CONSTRAINT chk_reserva_duracion_positiva
    CHECK (duracion_minutos > 0),

  CONSTRAINT chk_reserva_duracion_coherente
    CHECK (fin - inicio = duracion_minutos * INTERVAL '1 minute'),
  
  CONSTRAINT chk_reserva_precio
    CHECK (precio_servicio >= 0),
  
  CONSTRAINT chk_reserva_nombre_servicio
    CHECK (LENGTH(BTRIM(nombre_servicio)) > 0),

  CONSTRAINT chk_reserva_notas
    CHECK (notas IS NULL OR LENGTH(BTRIM(notas)) > 0),

  CONSTRAINT ex_reserva_proveedor_sin_cruce
    EXCLUDE USING gist (
      (int8range(
        proveedor_id::BIGINT * 10000000000 + EXTRACT(EPOCH FROM inicio)::BIGINT,
        proveedor_id::BIGINT * 10000000000 + EXTRACT(EPOCH FROM fin)::BIGINT,
        '[)')) WITH &&
    ) WHERE (estado_id = 1),

  CONSTRAINT ex_reserva_cliente_sin_cruce
    EXCLUDE USING gist (
      (int8range(
        cliente_id::BIGINT * 10000000000 + EXTRACT(EPOCH FROM inicio)::BIGINT,
        cliente_id::BIGINT * 10000000000 + EXTRACT(EPOCH FROM fin)::BIGINT,
        '[)')) WITH &&
    ) WHERE (estado_id = 1)
);

CREATE INDEX idx_reserva_cliente_inicio ON reserva (cliente_id, inicio DESC);

CREATE INDEX idx_reserva_proveedor_inicio_confirmada ON reserva (proveedor_id, inicio) WHERE estado_id = 1;

CREATE INDEX idx_reserva_servicio_id ON reserva (servicio_id);

CREATE TRIGGER trg_reserva_fecha_actualizacion
BEFORE UPDATE ON reserva
FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_actualizacion();

CREATE OR REPLACE FUNCTION fn_reserva_preparar()
RETURNS TRIGGER AS $$
DECLARE
  v_nombre VARCHAR(100);
  v_duracion INTEGER;
  v_precio NUMERIC(12, 2);
  v_moneda VARCHAR(3);
BEGIN
  SELECT s.nombre, s.duracion_minutos, s.precio, n.moneda_codigo_iso
    INTO v_nombre, v_duracion, v_precio, v_moneda
  FROM servicio s
  JOIN negocio n ON n.id_negocio = s.negocio_id
  WHERE s.id_servicio = NEW.servicio_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'El servicio no existe.';
  END IF;

  NEW.nombre_servicio := v_nombre;
  NEW.duracion_minutos := v_duracion;
  NEW.precio_servicio := v_precio;
  NEW.moneda_codigo_iso := v_moneda;
  NEW.fin := NEW.inicio + MAKE_INTERVAL(mins => v_duracion);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reserva_1_preparar
BEFORE INSERT ON reserva
FOR EACH ROW
EXECUTE FUNCTION fn_reserva_preparar();

CREATE OR REPLACE FUNCTION fn_reserva_validar_insert()
RETURNS TRIGGER AS $$
DECLARE
  v_estado_negocio VARCHAR(20);
  v_inicio_s INTEGER;
  v_dia INTEGER;
BEGIN
  IF NEW.estado_id <> 1 THEN
    RAISE EXCEPTION 'Una reserva nueva debe crearse en estado Confirmada.';
  END IF;

  SELECT en.nombre_estado INTO v_estado_negocio
  FROM servicio s
  JOIN negocio n ON n.id_negocio = s.negocio_id
  JOIN estado_negocio en ON en.id_estado = n.estado_id
  WHERE s.id_servicio = NEW.servicio_id;

  IF v_estado_negocio IS DISTINCT FROM 'Activo' THEN
    RAISE EXCEPTION 'El negocio no está disponible para reservas.';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM servicio_proveedor WHERE servicio_id = NEW.servicio_id AND proveedor_id = NEW.proveedor_id) THEN
    RAISE EXCEPTION 'El proveedor no está asignado a este servicio.';
  END IF;

  v_dia := EXTRACT(ISODOW FROM NEW.inicio)::INTEGER;
  v_inicio_s := EXTRACT(EPOCH FROM NEW.inicio::TIME)::INTEGER;

  IF NOT EXISTS (
    SELECT 1 FROM franja_servicio f
    WHERE f.servicio_id = NEW.servicio_id
      AND f.proveedor_id = NEW.proveedor_id
      AND f.dia_semana_id = v_dia
      AND f.hora_inicio = NEW.inicio::TIME
  ) THEN
    RAISE EXCEPTION 'Este horario no está disponible para reserva.';
  END IF;

  IF v_inicio_s + NEW.duracion_minutos * 60 > 86400 OR NOT EXISTS (
    SELECT 1
    FROM horario_proveedor h
    JOIN horario_bloque b ON b.horario_id = h.id_horario
    WHERE h.proveedor_id = NEW.proveedor_id
      AND b.dia_semana_id = v_dia
      AND v_inicio_s >= EXTRACT(EPOCH FROM b.hora_inicio)
      AND v_inicio_s + NEW.duracion_minutos * 60 <= EXTRACT(EPOCH FROM b.hora_fin)
  ) THEN
    RAISE EXCEPTION 'La franja no corresponde al horario de atención del proveedor.';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reserva_2_validar_insert
BEFORE INSERT ON reserva
FOR EACH ROW
EXECUTE FUNCTION fn_reserva_validar_insert();

CREATE OR REPLACE FUNCTION fn_reserva_validar_update()
RETURNS TRIGGER AS $$
BEGIN
  IF (NEW.codigo_reserva, NEW.cliente_id, NEW.proveedor_id, NEW.servicio_id, NEW.inicio, NEW.fin,
      NEW.notas, NEW.nombre_servicio, NEW.duracion_minutos, NEW.precio_servicio,
      NEW.moneda_codigo_iso, NEW.fecha_creacion)
    IS DISTINCT FROM
    (OLD.codigo_reserva, OLD.cliente_id, OLD.proveedor_id, OLD.servicio_id, OLD.inicio, OLD.fin,
      OLD.notas, OLD.nombre_servicio, OLD.duracion_minutos, OLD.precio_servicio,
      OLD.moneda_codigo_iso, OLD.fecha_creacion) THEN
    RAISE EXCEPTION 'Los datos de una reserva no se pueden modificar; solo cancelarla.';
  END IF;

  IF OLD.estado_id = 2 AND NEW.estado_id <> OLD.estado_id THEN
    RAISE EXCEPTION 'Una reserva cancelada no se puede reactivar.';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reserva_3_validar_update
BEFORE UPDATE ON reserva
FOR EACH ROW
EXECUTE FUNCTION fn_reserva_validar_update();

CREATE TABLE cancelacion_reserva (
  reserva_id INTEGER PRIMARY KEY,
  estado_id SMALLINT NOT NULL DEFAULT 2,
  cancelada_por INTEGER NOT NULL,
  fecha_cancelacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  motivo VARCHAR(250),

  CONSTRAINT chk_cancelacion_estado
    CHECK (estado_id = 2),

  CONSTRAINT chk_cancelacion_motivo
    CHECK (motivo IS NULL OR LENGTH(BTRIM(motivo)) > 0),

  CONSTRAINT fk_cancelacion_reserva
    FOREIGN KEY (reserva_id, estado_id)
    REFERENCES reserva(id_reserva, estado_id)
    ON DELETE RESTRICT,
  
  CONSTRAINT fk_cancelacion_usuario
    FOREIGN KEY (cancelada_por)
    REFERENCES usuario(id_usuario)
    ON DELETE RESTRICT
);

CREATE INDEX idx_cancelacion_reserva_cancelada_por ON cancelacion_reserva (cancelada_por);

CREATE OR REPLACE FUNCTION fn_cancelacion_reserva_aplicar()
RETURNS TRIGGER AS $$
DECLARE
  v_estado SMALLINT;
BEGIN
  NEW.estado_id := 2;

  UPDATE reserva SET estado_id = 2 WHERE id_reserva = NEW.reserva_id AND estado_id = 1;
  IF FOUND THEN
    RETURN NEW;
  END IF;

  SELECT estado_id INTO v_estado FROM reserva WHERE id_reserva = NEW.reserva_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'La reserva no existe.';
  END IF;

  RAISE EXCEPTION 'Esta reserva ya fue cancelada.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cancelacion_reserva_aplicar
BEFORE INSERT ON cancelacion_reserva
FOR EACH ROW
EXECUTE FUNCTION fn_cancelacion_reserva_aplicar();

CREATE OR REPLACE FUNCTION fn_reserva_exigir_cancelacion()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.estado_id = 2 AND NOT EXISTS (SELECT 1 FROM cancelacion_reserva WHERE reserva_id = NEW.id_reserva) THEN
    RAISE EXCEPTION 'Toda reserva cancelada debe tener su registro de cancelación.';
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_reserva_exigir_cancelacion
AFTER INSERT OR UPDATE OF estado_id ON reserva
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_reserva_exigir_cancelacion();

CREATE TABLE reserva_recurso (
  reserva_id INTEGER NOT NULL,
  recurso_id INTEGER NOT NULL,
  cantidad SMALLINT NOT NULL,

  PRIMARY KEY (reserva_id, recurso_id),

  CONSTRAINT fk_reserva_recurso_reserva
    FOREIGN KEY (reserva_id)
    REFERENCES reserva(id_reserva)
    ON DELETE RESTRICT,

  CONSTRAINT fk_reserva_recurso_recurso
    FOREIGN KEY (recurso_id)
    REFERENCES recurso(id_recurso)
    ON DELETE RESTRICT,

  CONSTRAINT chk_reserva_recurso_cantidad
    CHECK (cantidad BETWEEN 1 AND 999)
);

CREATE INDEX idx_reserva_recurso_recurso_id ON reserva_recurso (recurso_id);

CREATE OR REPLACE FUNCTION fn_reserva_recurso_validar_capacidad()
RETURNS TRIGGER AS $$
DECLARE
  v_nombre VARCHAR(100);
  v_total SMALLINT;
  v_inicio TIMESTAMP;
  v_fin TIMESTAMP;
  v_ocupado BIGINT;
BEGIN
  SELECT nombre, cantidad INTO v_nombre, v_total
  FROM recurso WHERE id_recurso = NEW.recurso_id FOR NO KEY UPDATE;

  SELECT inicio, fin INTO v_inicio, v_fin FROM reserva WHERE id_reserva = NEW.reserva_id;

  SELECT COALESCE(SUM(rr.cantidad), 0) INTO v_ocupado
  FROM reserva_recurso rr
  JOIN reserva r ON r.id_reserva = rr.reserva_id
  WHERE rr.recurso_id = NEW.recurso_id
    AND r.estado_id = 1
    AND r.id_reserva <> NEW.reserva_id
    AND r.inicio < v_fin
    AND r.fin > v_inicio;

  IF v_ocupado + NEW.cantidad > v_total THEN
    RAISE EXCEPTION 'No hay unidades suficientes del recurso % para esa franja.', v_nombre;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reserva_recurso_validar_capacidad
BEFORE INSERT ON reserva_recurso
FOR EACH ROW
EXECUTE FUNCTION fn_reserva_recurso_validar_capacidad();

CREATE OR REPLACE FUNCTION fn_reserva_copiar_recursos()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO reserva_recurso (reserva_id, recurso_id, cantidad)
  SELECT NEW.id_reserva, sr.recurso_id, sr.cantidad_requerida
  FROM servicio_recurso sr
  WHERE sr.servicio_id = NEW.servicio_id
  ORDER BY sr.recurso_id;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reserva_copiar_recursos
AFTER INSERT ON reserva
FOR EACH ROW
EXECUTE FUNCTION fn_reserva_copiar_recursos();