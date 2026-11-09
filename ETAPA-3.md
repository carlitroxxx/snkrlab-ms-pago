# Etapa 3: idempotencia local

El cliente genera un UUID por intento nuevo y conserva ese UUID al reintentar.
POST /api/v1/pagos mantiene JWT y rol Cliente, perfil local y respuesta 201,
incluso al devolver un intento persistido (se conserva el contrato HTTP existente).

## Contrato Postman

POST http://localhost:8087/api/v1/pagos
Authorization: Bearer <JWT válido Cliente>
Content-Type: application/json

    {"intentoId":"c4602727-9f0d-41d4-b3d9-0e1820e147dd","ordenId":900001,"importe":15000,"referenciaPago":"SIM_APROBADA_01"}

La respuesta anterior incorpora intentoId. Conservará id, intentoId, fecha y resultado al repetir.
1. Enviar el cuerpo y repetirlo con el mismo JWT: 201, mismo cuerpo y una sola fila.
2. Mantener UUID y cambiar ordenId, importe o referenciaPago: 409.
3. Reutilizar UUID con JWT de otro Cliente: 409, sin divulgar datos del propietario.
4. Usar un UUID nuevo y SIM_RECHAZADA_01: 201 RECHAZADO; repetir conserva el resultado.
5. Omitir intentoId o enviar null/UUID inválido: 400.
6. Enviar el mismo cuerpo simultáneamente desde dos pestañas: mismo resultado, una fila.
   Para una comprobación más fuerte, usar dos instancias contra la misma base y puertos distintos.
   Ante timeout/deadlock: 409 controlado; reintentar el MISMO UUID.

## Persistencia y concurrencia

Pago.intentoId usa UUID Java y CHAR(36) MySQL, con UNIQUE uk_pagos_intento_id.
Se compara propietario JWT, orden, importe, referencia y moneda CLP.
La primera inserción con saveAndFlush reserva la clave ANTES de ejecutar el simulador.
Usa RECHAZADO como valor provisional solo dentro de la transacción no confirmada;
se reemplaza por el resultado y se confirma todo junto. No es un nuevo estado de negocio.
Un error revierte toda la transacción. Una colisión UNIQUE se captura después del rollback,
y se consulta al ganador en otra transacción REQUIRES_NEW, evitando rollback-only y snapshots antiguos.
Esto depende del índice UNIQUE real y del motor transaccional InnoDB, no de bloqueos en memoria.
Ante un fallo que revierte la transacción se puede volver a evaluar en un reintento;
la garantía corresponde a pagos persistidos y a este simulador sin efectos externos.

## Migración compatible

Conservar la base existente. Antes del primer arranque, ejecutar manualmente
[docs/etapa-3-migracion.sql](docs/etapa-3-migracion.sql) una sola vez sobre la base v1.1.0.
No ejecutarlo si columna o índice ya existen: inspeccionar SHOW CREATE TABLE pagos primero.
Los registros anteriores conservan todos sus datos con intento_id NULL; MySQL admite varios
NULL en un índice UNIQUE. Nuevas solicitudes requieren UUID desde el servicio.
Se mantiene ddl-auto:update; también puede crear columna e índice al arrancar.
RECREATE_QUIETLY evita que Hibernate elimine previamente restricciones UNIQUE.
Comprobar siempre SHOW CREATE TABLE pagos antes de probar: debe existir el índice UNIQUE.
La migración no fue ejecutada por el asistente.

## Maven (desde snkrlab-ms-pago)

    ./mvnw.cmd "-Dtest=PagoServiceTest,PagoIdempotenciaTest,SimuladorPagoServiceTest" test

Arranque con las variables MySQL/Azure/Cognito ya usadas en v1.1.0:

    ./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"

Suite completa, con MySQL y variables configuradas para contextLoads:

    ./mvnw.cmd test

PagoServiceTest cubre creación de ambos resultados y validaciones.
PagoIdempotenciaTest cubre repetición, conflictos, propietario, UUID obligatorio,
colisión UNIQUE, timeout y dos hilos concurrentes con repositorio simulado.
Estas pruebas no sustituyen la comprobación de transacciones/índice en MySQL.
No se ejecutaron Maven, pruebas, terminal ni Git durante la implementación.
No hay nuevas dependencias, cambios de versión ni integraciones de etapas posteriores.
