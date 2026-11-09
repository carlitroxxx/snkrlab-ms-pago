# snkrlab-ms-pago

Base del microservicio de pagos de SNKRLAB para EV3. Esta etapa prepara Spring Boot, MySQL y validación JWT; todavía no procesa pagos.

## Endpoints

No hay controladores ni endpoints de negocio implementados. Los paquetes iniciales contienen únicamente documentación de paquete.

## Integraciones

Persistencia preparada para MySQL 8, base snkrlab_payments_db. Puerto HTTP local: **8087**. La URL predeterminada de MySQL utiliza localhost:3311 como puerto propuesto para una base dedicada; esa instancia todavía debe prepararse. DB_URL permite utilizar otra instancia.

No está incluido todavía en el Docker Compose principal. No hay integración con Carrito, Órdenes, RabbitMQ, Kafka, React ni despliegue AWS.

## Organización

- com.snkrlab.pago: clase principal.
- config: SecurityConfig reutiliza el modelo de Carrito.
- controller, service, dto, model y repository: paquetes preparados mediante package-info.java, sin lógica, entidades ni repositorios.
- src/main/resources/application.yaml: configuración externa.
- src/test/java: prueba contextLoads siguiendo la estructura existente.

## Dependencias

Java 21 y Spring Boot 4.1.1, igual que los servicios actuales. Dependencias administradas por Spring Boot: webmvc, security, security-oauth2-resource-server, data-jpa y mysql-connector-j; webmvc-test para pruebas. No se agregan OpenFeign, AMQP, Kafka ni Lombok en esta base porque todavía no se utilizan.

## Desarrollo y ejecución

Requisitos: JDK 21, MySQL 8 disponible, base snkrlab_payments_db creada, usuario con permisos sobre esa base y acceso a los repositorios Maven. El Wrapper se copia del servicio Órdenes y descarga su distribución Maven cuando sea necesario.

Configurar las siguientes variables en la configuración de ejecución de IntelliJ o en el entorno del proceso. No guardar secretos en archivos versionados.

| Variable | Uso |
|---|---|
| DB_URL | Opcional. URL JDBC; predeterminada en application.yaml. |
| DB_USERNAME | Obligatoria. Usuario MySQL. |
| DB_PASSWORD | Obligatoria. Contraseña MySQL. |
| AZURE_TENANT_ID | Obligatoria. Tenant de Entra utilizado por SNKRLAB. |
| AZURE_CLIENT_ID | Obligatoria. Identificador de la API, usado para validar aud. |
| COGNITO_ISSUER | Obligatoria. Emisor del User Pool, sin barra final; no es el dominio de Hosted UI. |
| COGNITO_CLIENT_ID | Obligatoria. App client de Cognito utilizado por SNKRLAB. |

Desde esta carpeta, comandos para ejecutar manualmente en PowerShell:

```powershell
.\mvnw.cmd -DskipTests package
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

En Linux/macOS, utilizar ./mvnw en lugar de .\mvnw.cmd; el script debe tener permiso de ejecución. En IntelliJ se puede importar pom.xml, seleccionar JDK 21 y ejecutar SnkrlabMsPagoApplication con las variables indicadas.

La compilación con -DskipTests no verifica el arranque. La prueba contextLoads y spring-boot:run requieren MySQL y todas las variables obligatorias. contextLoads comprueba la carga del contexto, no demuestra por sí sola la validación real de JWT.

El Dockerfile sigue las dos etapas existentes: Maven con Java 21 para construir y JRE 21 para ejecutar. Omite pruebas durante la construcción, igual que Órdenes. Para ejecutar la imagen habrá que suministrar las variables y una DB_URL accesible desde el contenedor; localhost dentro de él no apunta a MySQL en el equipo anfitrión.

## Seguridad y configuración

SecurityConfig mantiene el comportamiento de Carrito:

- Sesiones STATELESS y autenticación Bearer JWT.
- Entra: emisores v1 y v2 del tenant permitido, claves públicas JWKS, vigencia, audiencia de la API y scope write-read.
- Cognito: emisor permitido, claves JWKS, vigencia, token_use=access y client_id esperado.
- Entra convierte roles Cliente y Administrador a autoridades; Cognito reconoce Cliente únicamente si pertenece a cognito:groups.
- Todas las solicitudes requieren ROLE_Cliente. Un usuario solo Administrador o sin roles no queda autorizado. Igual que Carrito, alguien con ambos roles sí satisface hasRole("Cliente").
- CSRF deshabilitado para este servicio Bearer sin sesión; CORS conserva la configuración de Carrito.

La inspección inicial de iss solo selecciona un decoder permitido; la aceptación del token depende posteriormente de su verificación criptográfica y validadores. El servicio necesita acceso a las claves públicas de ambos proveedores al validar tokens.

No se copian contraseñas ni identificadores de entorno al repositorio. Los identificadores deben corresponder a los usados actualmente por SNKRLAB; esta etapa no requiere modificar AWS.

## Comprobación inicial

1. Compilar y ejecutar contextLoads con MySQL y las variables preparadas.
2. Arrancar y verificar en los logs que el servicio escucha en 8087 y conecta con MySQL.
3. Una solicitud sin token debe ser rechazada con 401. Un token inválido también debe ser rechazado.
4. Probar tokens reales de Cliente de Entra y Cognito y de Administrador sin Cliente. Todavía no existe una ruta de negocio para obtener una respuesta funcional exitosa.

No usar el estado de una ruta inexistente como única prueba de autorización positiva: el tratamiento de /error puede afectar esa respuesta. Las pruebas completas de acceso se realizarán al incorporar el primer endpoint.

## Estado de esta etapa

Estructura inicial. No hay pagos, simulación, antifraude, alertas ni comunicación entre servicios implementados. Compilación y pruebas pendientes de ejecución por el desarrollador.
