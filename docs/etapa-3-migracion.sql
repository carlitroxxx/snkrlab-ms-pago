-- Migración manual de una base v1.1.0, ANTES del primer arranque de etapa 3.
-- Ejecutar una sola vez. Si Hibernate ya añadió columna/índice, inspeccionar antes.
-- No elimina ni recrea tablas ni modifica filas históricas.
ALTER TABLE pagos
    ADD COLUMN intento_id CHAR(36) NULL,
    ADD CONSTRAINT uk_pagos_intento_id UNIQUE (intento_id);

-- Comprobaciones posteriores (también sirven si Hibernate aplicó ddl-auto:update):
SHOW CREATE TABLE pagos;
SELECT COUNT(*) AS historicos FROM pagos WHERE intento_id IS NULL;
SELECT intento_id, COUNT(*) AS cantidad FROM pagos
WHERE intento_id IS NOT NULL GROUP BY intento_id HAVING COUNT(*) > 1;
