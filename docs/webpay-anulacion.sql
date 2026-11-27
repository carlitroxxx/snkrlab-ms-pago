-- Aplicación manual previa al despliegue; no ejecutado.
-- NULL en registros históricos significa que NO hay evidencia de anulación.
ALTER TABLE webpay_intentos ADD COLUMN anulacion_recibida BIT NULL;
