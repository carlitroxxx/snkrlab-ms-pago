-- Revisar y ejecutar manualmente sobre v1.2.0 con la aplicación detenida.
-- No elimina registros. Las tablas deben usar InnoDB.
-- Conversión conservadora: admite también instalaciones donde Hibernate creó ENUM.
ALTER TABLE pagos MODIFY COLUMN resultado VARCHAR(16) NOT NULL;
CREATE TABLE IF NOT EXISTS referencias_antifraude (
 referencia VARCHAR(32) NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
INSERT INTO referencias_antifraude (referencia) VALUES ('SIM_APROBADA_01'), ('SIM_RECHAZADA_01')
ON DUPLICATE KEY UPDATE referencia = VALUES(referencia);
CREATE TABLE IF NOT EXISTS alertas_fraude (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 intento_id CHAR(36) NOT NULL,
 referencia_pago VARCHAR(32) NOT NULL,
 usuario_id VARCHAR(255) NOT NULL,
 momento DATETIME(6) NOT NULL,
 cantidad_intentos BIGINT NOT NULL,
 ventana_segundos INT NOT NULL,
 max_intentos INT NOT NULL,
 motivo VARCHAR(255) NOT NULL,
 CONSTRAINT uk_alertas_intento UNIQUE (intento_id)
) ENGINE=InnoDB;
-- Índice de ventana: creación idempotente sin procedimientos permanentes.
SET @indice_existe = (SELECT COUNT(*) FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'pagos' AND index_name = 'idx_pagos_referencia_fecha');
SET @ddl_indice = IF(@indice_existe = 0,
 'CREATE INDEX idx_pagos_referencia_fecha ON pagos (referencia_pago, fecha)', 'SELECT 1');
PREPARE crear_indice FROM @ddl_indice;
EXECUTE crear_indice;
DEALLOCATE PREPARE crear_indice;
