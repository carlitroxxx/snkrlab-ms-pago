# Webpay Plus — etapa 1 (solo Pago)

SDK oficial 6.2.1, Java target 21, Spring Boot 4.1.1. Exclusivamente integración:
https://transbankdevelopers.com/documentacion/webpay-plus
https://transbankdevelopers.com/documentacion/como_empezar
https://github.com/TransbankDevelopers/transbank-sdk-java

No se aplicaron migraciones ni cambios de infraestructura. Ejecutar manualmente
docs/webpay-migracion.sql tras revisión y respaldo, antes de desplegar esta versión.
ddl-auto sigue en validate. El SQL no es un script de arranque ni debe repetirse.
Los pagos históricos conservan todos sus datos; proveedor NULL significa SIMULADO.
Se añaden proveedor a pagos, webpay_intentos y pago_compra_guard.
No se almacenan PAN, CVV ni datos de tarjeta devueltos por el SDK.

## Configuración
Perfil local:
- WEBPAY_RETURN_URL: http://localhost:8087/api/v1/pagos/webpay/retorno
- WEBPAY_FRONTEND_URL: http://localhost:5173/pago/resultado
- ORDENES_URL: http://localhost:8085
Son valores predeterminados sobrescribibles. Para pruebas externas usar una URL
HTTPS accesible por el navegador. El perfil local conserva PagoController simulado.

AWS (sin perfil local): las tres variables son obligatorias. RETURN_URL debe ser
la URL HTTPS pública del endpoint; FRONTEND_URL la página HTTPS fija de resultado;
ORDENES_URL la dirección interna del servicio de Órdenes (sin /api/v1/ordenes).
La dirección exacta de AWS no se inventó ni se configuró.
Credenciales públicas oficiales de integración fijadas en WebpayGateway; no existe
selector de producción. No se deben reemplazar por credenciales de producción.

## API
Todas las rutas salvo retorno requieren JWT válido, rol Cliente y propiedad.
- POST /api/v1/pagos/webpay/transacciones
  JSON: {"intentoId":"UUID","checkoutId":"UUID","ordenId":123}
  No recibe importe: consulta GET /api/v1/ordenes/{id} con el JWT del usuario.
  Comprueba propietario, checkout, estado PENDIENTE_PAGO, total positivo y CLP.
  Devuelve ordenId, checkoutId, intentoId, pagoId, estado, importe, moneda.
  Solo en PENDIENTE devuelve token/url para el formulario POST token_ws de Transbank.
  Repetir exactamente los mismos identificadores recupera el intento.
- GET /api/v1/pagos/webpay/ordenes/{id}: consulta local, nunca entrega token.
- POST /api/v1/pagos/webpay/ordenes/{id}/sincronizacion: consulta SDK status;
  concilia con persistencia, nunca ejecuta commit ni crea otro cobro.
- GET y POST /api/v1/pagos/webpay/retorno: parámetros token_ws, TBK_TOKEN,
  TBK_ORDEN_COMPRA y TBK_ID_SESION. Retorno normal confirma una sola vez;
  anulación, timeout y retorno mixto consultan estado, no confirman.
  Un retorno conocido redirige 303 a FRONTEND_URL fija, sin parámetros sensibles.
  Un token desconocido devuelve 400 sin consultar Transbank.
- GET /api/v1/pagos/{id}: disponible también fuera de local; mantiene DTO existente.
- POST /api/v1/pagos: simulación existente, únicamente perfil local.

## Persistencia y recuperación
Reserva de proveedor por orden compartida con simulación: INSERT IGNORE + bloqueo
de fila dentro de transacción MySQL. Transbank se invoca fuera de transacciones.
webpay_intentos tiene una fila por orden y checkout/intento/buyOrder/token únicos.
Se verifica orden, sesión e importe de toda respuesta antes de aprobar.
Solo AUTHORIZED + responseCode 0 produce APROBADO. FAILED produce RECHAZADO;
REVERSED/NULLIFIED producen ANULADO. Otras respuestas permanecen INCIERTO.
La aprobación crea Pago y guarda su ID atómicamente, bajo bloqueo de intento.
Las consultas posteriores reutilizan el Pago terminal.
El simulador y sus reglas antifraude siguen intactos; solo se añade reserva del
proveedor y etiquetado. Los registros anteriores se rechazan al iniciar Webpay
si ya existe cualquier pago para esa orden.

Límite deliberado: una transacción Webpay por orden, incluso tras rechazo/anulación.
No se libera automáticamente una reserva. No iniciar otro checkout para eludir
una compra incierta. Un proceso que cae durante create puede dejar CREANDO sin
token; requiere conciliación manual. Nunca se repite create automáticamente.
commitIniciado se persiste antes de commit: si el proceso cae antes de enviarlo,
se consulta status y puede requerir conciliación manual; no se repite commit.
INITIALIZED tras anular/timeout no se traduce a ANULADO sin evidencia del proveedor.
No hay tarea programada de reconciliación en esta etapa.
Los pagos terminales no se actualizan por devoluciones posteriores (fuera de alcance).

## Contrato siguiente etapa
Carrito debe generar/persistir intentoId antes del HTTP, fijar modalidad y conservar
checkoutId/ordenId. Debe recuperar pagoId y resultado desde Pago, contrastar sus
datos y reutilizar su confirmación existente; no confiar en el navegador.
Frontend debe conservar el checkout, enviar formulario POST a url con token_ws y
volver a autenticarse con Cognito si hace falta. La página de resultado aún no existe.
Órdenes conserva GET /api/v1/pagos/{id} y la validación de pago/usuario/monto/moneda.
Pago NO confirma órdenes, descuenta stock ni envía comprobantes.
API Gateway debe permitir únicamente GET/POST de la ruta exacta de retorno sin
authorizer. Todas las demás rutas continúan protegidas. No se modificó AWS.
No registrar cuerpos, tokens, parámetros del retorno ni Authorization en proxies,
access logs o trazas HTTP del SDK.
Pruebas concurrentes reales de MySQL, seguridad HTTP completa y flujo sandbox de
navegador quedan para un entorno aislado tras aplicar la migración.
