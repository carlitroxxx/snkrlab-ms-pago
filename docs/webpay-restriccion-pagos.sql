-- Migracion para bases de datos con la restriccion antigua.
-- Realizar un respaldo antes de ejecutar.
-- Verificar previamente la restriccion con SHOW CREATE TABLE pagos.
-- Aplicar solo si pagos_chk_1 no admite ANULADO.

ALTER TABLE pagos
    DROP CHECK pagos_chk_1,
    ADD CONSTRAINT pagos_chk_1
    CHECK (resultado IN (
        'APROBADO',
        'RECHAZADO',
        'BLOQUEADO_FRAUDE',
        'ANULADO'
    ));

-- Verificacion posterior.
SHOW CREATE TABLE pagos;
