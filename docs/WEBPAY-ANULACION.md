# Conciliación de anulación
Retorno exclusivo TBK_TOKEN + TBK_ORDEN_COMPRA + TBK_ID_SESION correlacionado:
se persiste anulacion_recibida bajo bloqueo solo si no comenzó commit ni existe pago.
Se consulta Transbank; INITIALIZED correlacionado permite ANULADO solo con esa
evidencia y sin commit. Otros INITIALIZED siguen INCIERTO. Retorno mixto no aporta
evidencia. Una reserva de anulación bloquea commits posteriores incluso si falla status;
sincronización autenticada puede recuperar el resultado. Si commit ganó la carrera,
INITIALIZED permanece incierto. No se usa response_code para inferir anulación.
Esto es evidencia del flujo de retorno, no una firma criptográfica de Transbank.

Orden local 29: INITIALIZED, sin pago ni commit NO prueba que hubo anulación.
No marcarla manualmente ni inventar los parámetros perdidos. Después de aplicar
la migración y desplegar (fuera de esta tarea), si se conserva el retorno original
verificable, reprocesarlo por el endpoint normal con los tres valores originales
en un entorno controlado, sin registrar tokens. Alternativamente retomar el mismo
formulario únicamente si todavía es válido y efectuar la anulación real.
Sin evidencia recuperable, mantener bloqueo y consultar estado/soporte Transbank
hasta contar con resultado definitivo. No crear otro intento para eludir el bloqueo.
Finalmente sincronizar el checkout por Carrito. No se ejecutó SQL ni se alteró orden 29.
