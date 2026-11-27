-- Revisión/aplicación MANUAL antes del despliegue. Este archivo no se autoejecuta.
-- Sin borrados ni backfill de pagos históricos. NULL proveedor se interpreta SIMULADO.
ALTER TABLE pagos ADD COLUMN proveedor VARCHAR(16) NULL;
CREATE TABLE pago_compra_guard (
  orden_id BIGINT NOT NULL PRIMARY KEY,
  proveedor VARCHAR(16) NOT NULL
);
CREATE TABLE webpay_intentos (
  orden_id BIGINT NOT NULL PRIMARY KEY,
  checkout_id BINARY(16) NOT NULL UNIQUE,
  intento_id BINARY(16) NOT NULL UNIQUE,
  usuario_id VARCHAR(255) NOT NULL,
  importe INT NOT NULL,
  buy_order VARCHAR(26) NOT NULL UNIQUE,
  session_id VARCHAR(61) NOT NULL,
  token VARCHAR(128) NULL UNIQUE,
  url VARCHAR(2048) NULL,
  estado VARCHAR(24) NOT NULL,
  pago_id BIGINT NULL,
  commit_iniciado BIT NOT NULL
);
-- resultado sigue siendo VARCHAR(16); ANULADO cabe sin cambiar la columna.
