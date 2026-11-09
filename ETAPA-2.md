# Etapa 2: simulación local de pagos

Endpoint provisional, sin cobros reales ni efectos sobre órdenes, ventas, stock o comprobantes.
Solo se registra con el perfil local, que enlaza el servidor a 127.0.0.1.
No habilitar este perfil en despliegues. No se han incorporado integraciones externas.

## Preparación y Maven (ejecución manual)

Desde snkrlab-ms-pago, con Java 21, configurar DB_USERNAME, DB_PASSWORD y opcionalmente DB_URL
(predeterminado: MySQL localhost:3311/snkrlab_payments_db). La base debe existir.
Configurar AZURE_TENANT_ID, AZURE_CLIENT_ID, COGNITO_ISSUER y COGNITO_CLIENT_ID.
Hibernate utiliza la configuración existente ddl-auto: update.

Pruebas unitarias aisladas, sin MySQL ni JWT remoto:

    ./mvnw.cmd "-Dtest=SimuladorPagoServiceTest,PagoServiceTest" test

Si no hay wrapper, sustituir ./mvnw.cmd por mvn.
El test existente contextLoads requiere la configuración y MySQL disponibles para ejecutar toda la suite con mvn test.

Arranque local:

    ./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"

## Postman

POST http://localhost:8087/api/v1/pagos
Authorization: Bearer <JWT válido con rol Cliente>
Content-Type: application/json

    {"ordenId": 900001, "importe": 15000, "referenciaPago": "SIM_APROBADA_01"}

Usar identificadores ficticios. El cuerpo admite solo esos tres campos.
No enviar usuarioId, moneda, números de tarjeta, CVV ni vencimiento.
La identidad se obtiene del JWT: oid de Azure o cognito: seguido del sub de Cognito.
La moneda es siempre CLP. El importe debe ser entero positivo (máximo Integer.MAX_VALUE).

Respuesta 201 tanto para APROBADO como para RECHAZADO:

    {"id":1,"ordenId":900001,"usuarioId":"<identidad JWT>","importe":15000,"moneda":"CLP","referenciaPago":"SIM_APROBADA_01","resultado":"APROBADO","fecha":"2026-11-09T12:00:00"}

La fecha se genera en UTC y se representa como LocalDateTime.
SIM_APROBADA_01 produce APROBADO; SIM_RECHAZADA_01 produce RECHAZADO.
Una referencia distinta devuelve 400 sin persistencia. Orden/importe nulos o no positivos: 400.
Campos adicionales y decimales: 400. Token inválido: 401; falta de rol Cliente: 403.
Probar ambas referencias y consultar la tabla pagos para comprobar los dos registros.
Repetir una solicitud genera otro intento con un id distinto.
La consulta por usuario está disponible en PagoService, sin endpoint GET en esta etapa.

## Pruebas y límites

SimuladorPagoServiceTest verifica las referencias y entradas no admitidas.
PagoServiceTest verifica los campos enviados al repositorio, ambos resultados, entradas inválidas y consulta por usuario.
Son pruebas unitarias con Mockito; no verifican físicamente MySQL ni el filtro HTTP/JWT.
No se han ejecutado pruebas ni compilación durante esta implementación.

Pendientes: validación de orden, propietario e importe contra Órdenes; antifraude por referencia estable
(más de tres usos en diez segundos); idempotencia e integraciones en sus etapas correspondientes.
Las guías 10 a 15 no estaban disponibles entre los archivos revisados y no se copiaron implementaciones de ellas.
No se cambia la versión Maven existente ni se confirma v1.1.0.
