# Sistema de Biblioteca

Evaluacion Final Transversal - Desarrollo Cloud Native II (DSY2207).

Sistema de biblioteca backend-only con BFF Spring Boot, cuatro Azure Functions en Java, servicio de libros Spring Boot, API REST, API GraphQL, integracion asincrona con Azure Event Grid (EDA principal, S8/S9) y Azure Service Bus (legacy S5), desplegado completamente en Azure y OCI.

## Cumplimiento de requisitos S9

| Requisito | Implementacion |
| --- | --- |
| Microservicios Spring Boot en nube | BFF (`bff-biblioteca-kath2026`) y servicio-libros (`servicio-libros-kath2026`) en Azure Container Apps |
| Funciones serverless Java | 4 Azure Function Apps desplegadas en Azure |
| API REST | BFF expone REST para usuarios, prestamos y libros |
| API GraphQL | `function-usuarios`, `function-prestamos`, `function-libros` y `servicio-libros` exponen GraphQL; BFF lo proxea |
| BFF | `bff-springboot` orquesta y proxea REST + GraphQL |
| Tecnologia de eventos | Azure Event Grid: `Biblioteca.PrestamoCreado`, `Biblioteca.PrestamoDevuelto`, `Biblioteca.UsuarioEliminado` |
| Prestamo resta disponibilidad del libro | Al crear prestamo, libro pasa a `ESTADO=PRESTADO` (1 fila = 1 copia fisica; disponibilidad 1 -> 0) |
| Eliminar usuario elimina prestamos asociados | `DELETE /api/usuarios/{id}` hace cascade delete en una transaccion y publica `Biblioteca.UsuarioEliminado` en Event Grid |
| Despliegue cloud | 100%: BFF, servicio-libros, 4 Azure Functions, Event Grid, Service Bus en Azure; Oracle en OCI |
| Scripts de BD Oracle | `database/oracle/schema.sql` y `database/oracle/data.sql` |
| Docker | `docker-compose.yml` disponible para desarrollo local |

## Ambiente cloud completo

Todos los componentes Java estan desplegados en Azure:

| Componente | Tipo | URL |
| --- | --- | --- |
| `bff-springboot` | Azure Container Apps | `https://bff-biblioteca-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io` |
| `servicio-libros` | Azure Container Apps | `https://servicio-libros-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io` |
| `function-usuarios` | Azure Function App | `https://biblio-usuarios-kath2026-v2.azurewebsites.net/api` |
| `function-prestamos` | Azure Function App | `https://biblio-prestamos-kath2026-v2.azurewebsites.net/api` |
| `function-libros` | Azure Function App | `https://biblio-libros-kath2026.azurewebsites.net/api` |
| `function-notificaciones` | Azure Function App | trigger-only (Service Bus + Event Grid) |

Servicios de infraestructura en Azure:

- **Event Grid Topic**: `biblioteca-eventos-topic` (mecanismo EDA S8)
- **Event Grid Subscription**: `biblioteca-prestamos-notificaciones-sub`
- **Service Bus**: `servicebus-katherine2026` / cola `prestamo-notificaciones` (flujo S5 legacy)
- **Oracle Autonomous Database**: OCI - persistencia unica para USUARIOS, LIBROS, PRESTAMOS

Verificacion rapida del ambiente cloud:

```powershell
curl https://bff-biblioteca-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io/actuator/health
curl https://bff-biblioteca-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io/api/usuarios
curl https://bff-biblioteca-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io/api/prestamos
curl https://bff-biblioteca-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io/api/libros
curl https://servicio-libros-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io/actuator/health
```

## Alcance

- API REST para usuarios, prestamos y libros.
- API GraphQL para consultas de usuarios, prestamos y libros.
- Backend for Frontend desarrollado con Spring Boot con proxy GraphQL.
- Cuatro funciones serverless en Java desplegadas en Azure:
  - `function-usuarios`: CRUD de usuarios + GraphQL.
  - `function-prestamos`: CRUD de prestamos + GraphQL.
  - `function-libros`: CRUD de libros + GraphQL.
  - `function-notificaciones`: consumidor dual de eventos - Azure Service Bus (flujo S5) y Azure Event Grid (flujo S8).
- Servicio de libros separado del BFF (Spring Boot).
- Arquitectura orientada a eventos (EDA) con Azure Event Grid como mecanismo principal (flujo S8): productor en `function-prestamos`, consumidor en `function-notificaciones`.
- Arquitectura orientada a eventos (EDA) con Azure Service Bus como message broker (flujo S5, mantenido).
- Productor de eventos Event Grid en `function-prestamos`: publica `Biblioteca.PrestamoCreado` y `Biblioteca.PrestamoDevuelto` despues de confirmar cada operacion en Oracle.
- Consumidor de eventos Event Grid en Azure Function con `@EventGridTrigger`.
- Productor legado en el BFF para notificaciones via Service Bus (`POST /api/prestamos/notificar`).
- Scripts SQL para creacion y carga inicial de datos en Oracle.
- Sin frontend incluido en el repositorio.

## Componentes

- [bff-springboot](bff-springboot): punto de entrada para el cliente. Valida payloads, expone la API REST principal, proxy GraphQL hacia funciones y servicio de libros, y orquesta llamadas downstream.
- [function-usuarios](function-usuarios): Azure Function en Java para CRUD de usuarios y consultas GraphQL.
- [function-prestamos](function-prestamos): Azure Function en Java para CRUD de prestamos, devoluciones, consultas GraphQL y publicacion de eventos en Azure Event Grid (`Biblioteca.PrestamoCreado`, `Biblioteca.PrestamoDevuelto`) como productor EDA S8.
- [function-libros](function-libros): Azure Function en Java para CRUD de libros, disponibilidad y consultas GraphQL.
- [function-notificaciones](function-notificaciones): Azure Function en Java con consumidor dual: `NotificacionConsumer` via Azure Service Bus (flujo S5) y `PrestamoEventGridConsumer` via Azure Event Grid con `@EventGridTrigger` (flujo S8).
- [servicio-libros](servicio-libros): microservicio Spring Boot para gestion de libros y disponibilidad (GraphQL incluido).
- [database/oracle](database/oracle): scripts `schema.sql` y `data.sql`.
- [docs/diagrama/diagrama-s8.png](docs/diagrama/diagrama-s8.png): diagrama de arquitectura S8 del proyecto.

## Arquitectura

- El cliente consume unicamente el BFF.
- El BFF enruta llamadas REST a usuarios, prestamos y libros.
- El BFF expone un proxy GraphQL unificado bajo `/api/graphql/{servicio}`.
- Las funciones de usuarios, prestamos y libros se consumen por URL configurable; en el flujo validado apuntan a Azure.
- La funcion de notificaciones es exclusivamente consumidora de eventos de Service Bus (sin endpoints HTTP).
- El servicio de libros esta desplegado en Azure Container Apps (`servicio-libros-kath2026`) y se consume mediante su URL publica.
- Usuarios, prestamos y libros persisten en una unica Oracle Autonomous Database configurada por variables de entorno.
- El archivo [docker-compose.yml](docker-compose.yml) permite levantar el BFF y el servicio de libros localmente para desarrollo.

Flujo EDA - S8: Azure Event Grid (mecanismo principal):

- `function-prestamos` actua como productor de eventos: al crear o devolver un prestamo, primero confirma la operacion en Oracle (`commit()`), luego publica el evento en el topic `biblioteca-eventos-topic` de Azure Event Grid.
- Los tipos de evento definidos son `Biblioteca.PrestamoCreado` y `Biblioteca.PrestamoDevuelto`. Cada evento incluye `prestamoId`, `usuarioId`, `libroId`, `estado`, `fechaEvento` y `correlationId`.
- La suscripcion `biblioteca-prestamos-notificaciones-sub` filtra unicamente esos dos tipos de evento y los enruta a `function-notificaciones`.
- La Azure Function `PrestamoEventGridConsumer` en `function-notificaciones` actua como consumidor: escucha mediante `@EventGridTrigger`, deserializa el evento y genera la notificacion correspondiente con trazabilidad por `correlationId`.
- La comunicacion es completamente asincrona, desacoplada y basada en push desde Azure Event Grid.

Flujo EDA - S5: Azure Service Bus (flujo anterior, mantenido):

- El BFF actua como productor: al invocar `POST /api/prestamos/notificar`, serializa el payload a JSON y lo publica en la cola `prestamo-notificaciones` de Azure Service Bus.
- La Azure Function `NotificacionConsumer` en `function-notificaciones` actua como consumidor: escucha la cola mediante `@ServiceBusQueueTrigger`, deserializa el mensaje y genera la notificacion.
- La comunicacion entre productor y consumidor es completamente asincrona y desacoplada.

Azure Functions desplegadas:

| Function App | Modulo | Funciones |
| --- | --- | --- |
| `biblio-usuarios-kath2026-v2` | function-usuarios | UsuariosCrear, UsuariosListar, UsuariosObtener, UsuariosActualizar, UsuariosEliminar, UsuariosGraphQL |
| `biblio-prestamos-kath2026-v2` | function-prestamos | PrestamosCrear, PrestamosListar, PrestamosObtener, PrestamosActualizar, PrestamosDevolver, PrestamosEliminar, PrestamosGraphQL |
| `biblio-libros-kath2026` | function-libros | LibrosCrear, LibrosListar, LibrosObtener, LibrosActualizarEstado, LibrosDisponibilidad, LibrosGraphQL |
| `biblio-notificaciones-kath2026` | function-notificaciones | NotificacionConsumer, PrestamoEventGridConsumer |

## Nota de diseño EDA

El flujo S8 usa Azure Event Grid como mecanismo EDA principal: `function-prestamos` publica los eventos `Biblioteca.PrestamoCreado` y `Biblioteca.PrestamoDevuelto` directamente al topic, y `function-notificaciones` los consume via `@EventGridTrigger`. Azure Service Bus se mantiene como flujo legacy S5 (endpoint `POST /api/prestamos/notificar` en el BFF), conservado por compatibilidad con el entregable anterior. Ambos flujos coexisten sin interferencia.

## Evidencia de despliegue cloud completo

| Componente | Servicio Azure | URL | Estado esperado |
| --- | --- | --- | --- |
| BFF Spring Boot | Azure Container Apps | `https://bff-biblioteca-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io` | `Running` |
| servicio-libros | Azure Container Apps | `https://servicio-libros-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io` | `Running` |
| function-usuarios | Azure Function App | `https://biblio-usuarios-kath2026-v2.azurewebsites.net` | `Running` |
| function-prestamos | Azure Function App | `https://biblio-prestamos-kath2026-v2.azurewebsites.net` | `Running` |
| function-libros | Azure Function App | `https://biblio-libros-kath2026.azurewebsites.net` | `Running` |
| function-notificaciones | Azure Function App | `biblio-notificaciones-kath2026` | trigger-only |
| Event Grid Topic | Azure Event Grid | `biblioteca-eventos-topic` | activo |
| Event Grid Subscription | Azure Event Grid | `biblioteca-prestamos-notificaciones-sub` | activo |
| Service Bus | Azure Service Bus | `servicebus-katherine2026 / prestamo-notificaciones` | activo (S5 legacy) |
| Base de datos | Oracle Autonomous DB (OCI) | configurado via `ORACLE_JDBC_URL` | activo |

## Como validar en 3 minutos

```bash
BFF=https://bff-biblioteca-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io

# 1. Health checks
curl $BFF/actuator/health
curl https://servicio-libros-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io/actuator/health

# 2. Datos en cloud
curl $BFF/api/usuarios
curl $BFF/api/prestamos
curl $BFF/api/libros

# 3. Flujo completo Event Grid S8 (crea prestamo -> publica evento -> function-notificaciones lo consume)
./scripts/demo-eventgrid-flow.sh
# Verificar en Azure Portal -> biblio-notificaciones-kath2026 -> Log stream:
#   [NOTIFICACION SIMULADA] ... PRESTAMO REGISTRADO ...
#   [NOTIFICACION SIMULADA] ... DEVOLUCION REGISTRADA ...
```

## Estructura del repositorio

```text
biblioteca-faas-semana3/
  bff-springboot/
    src/main/java/com/biblioteca/bff/
      controller/
        GraphQLProxyController.java      # proxy GraphQL unificado
        LibrosController.java
        PrestamosController.java         # endpoint /notificar (productor EDA)
        UsuariosController.java
      dto/
        NotificacionPrestamoRequest.java # DTO de notificacion
      service/
        ServiceBusProducerService.java   # productor Azure Service Bus
  function-usuarios/
    src/main/java/com/biblioteca/functions/usuarios/
      UsuarioFunction.java               # CRUD REST usuarios
      UsuarioGraphQLFunction.java        # GraphQL usuarios
  function-prestamos/
    src/main/java/com/biblioteca/functions/prestamos/
      PrestamoFunction.java              # CRUD REST prestamos + publicacion eventos Event Grid
      PrestamoGraphQLFunction.java       # GraphQL prestamos
      PrestamoEventGridPublisher.java    # publicador de eventos Event Grid - S8
  function-libros/
    src/main/java/com/biblioteca/functions/libros/
      LibroFunction.java                 # CRUD REST libros
      LibroGraphQLFunction.java          # GraphQL libros
  function-notificaciones/
    src/main/java/com/biblioteca/functions/notificaciones/
      NotificacionConsumerFunction.java          # consumidor EDA via Service Bus (@ServiceBusQueueTrigger) - S5
      PrestamoEventGridConsumerFunction.java     # consumidor EDA via Event Grid (@EventGridTrigger) - S8
  servicio-libros/
  database/
    oracle/
      schema.sql
      data.sql
  docs/
    diagrama/
      diagrama-s8.mmd
      diagrama-s8.png
  .env.example
  .gitignore
  docker-compose.yml
  pom.xml
  README.md
```

## Tecnologias

- Java 17
- Spring Boot 3.3.5
- Azure Functions Java
- Azure Service Bus (SDK `azure-messaging-servicebus`) - flujo S5
- Azure Event Grid (SDK `azure-messaging-eventgrid:4.28.0`) - flujo S8
- Oracle Database
- JDBC
- Spring Data JPA
- Docker
- Docker Compose
- Maven

## Requisitos

- JDK 17
- Maven
- Docker Desktop
- Azure Functions Core Tools v4 solo si necesitas ejecutar las funciones en local

## Configuracion

Usa [`.env.example`](.env.example) como base para crear tu archivo `.env`.

Variables relevantes:

- `ORACLE_JDBC_URL`
- `ORACLE_APP_USER`
- `ORACLE_APP_PASSWORD`
- `BFF_PORT`
- `LIBROS_PORT`
- `USUARIOS_FUNCTION_BASE_URL`
- `PRESTAMOS_FUNCTION_BASE_URL`
- `LIBROS_SERVICE_BASE_URL`
- `BFF_DOWNSTREAM_CONNECT_TIMEOUT`
- `BFF_DOWNSTREAM_READ_TIMEOUT`
- `LOG_LEVEL_ROOT`
- `LOG_LEVEL_APP`
- `SERVICEBUS_CONNECTION_STRING`
- `SERVICEBUS_QUEUE_NAME`

Variables legacy para compatibilidad local:

- `ORACLE_DB_HOST`
- `ORACLE_DB_PORT`
- `ORACLE_DB_SERVICE`
- `ORACLE_USERNAME`
- `ORACLE_PASSWORD`

Configuracion recomendada para modo hibrido:

```env
ORACLE_JDBC_URL=<oracle-cloud-jdbc-url>
ORACLE_APP_USER=biblioteca
ORACLE_APP_PASSWORD=<oracle-cloud-password>
USUARIOS_FUNCTION_BASE_URL=https://<tu-funcion-usuarios>.azurewebsites.net/api
PRESTAMOS_FUNCTION_BASE_URL=https://<tu-funcion-prestamos>.azurewebsites.net/api
```

Configuracion para modo local manual:

```env
USUARIOS_FUNCTION_BASE_URL=http://localhost:7071/api
PRESTAMOS_FUNCTION_BASE_URL=http://localhost:7072/api
```

Configuracion de Azure Service Bus (flujo S5):

```env
SERVICEBUS_CONNECTION_STRING=Endpoint=sb://<tu-namespace>.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=<tu-clave>
SERVICEBUS_QUEUE_NAME=prestamo-notificaciones
```

Configuracion de Azure Event Grid (flujo S8):

```env
EVENTGRID_TOPIC_ENDPOINT=https://<tu-topic>.eastus2-1.eventgrid.azure.net/api/events
EVENTGRID_TOPIC_KEY=<tu-clave-del-topic>
EVENTGRID_TOPIC_NAME=biblioteca-eventos-topic
```

Estas variables se configuran en los App Settings de `biblio-prestamos-kath2026-v2` en Azure Portal.
El topic `biblioteca-eventos-topic` debe tener una Event Subscription con filtros `Biblioteca.PrestamoCreado` y `Biblioteca.PrestamoDevuelto` apuntando a `biblio-notificaciones-kath2026`.

Para las Azure Functions, la conexion de Service Bus se configura en `local.settings.json` bajo las claves `ServiceBusConnection` y `SERVICEBUS_QUEUE_NAME`. Usa [function-prestamos/local.settings.sample.json](function-prestamos/local.settings.sample.json) como referencia.

Notas operativas:

- En Docker, el BFF usa `http://servicio-libros:8083/api` para comunicarse con `servicio-libros`.
- `servicio-libros` toma su conexion Oracle directamente desde el `.env`.
- El puerto publicado del BFF depende de `BFF_PORT`. En este repositorio se valida normalmente con `8088`.
- Los timeouts del proxy HTTP del BFF se controlan con `BFF_DOWNSTREAM_CONNECT_TIMEOUT` y `BFF_DOWNSTREAM_READ_TIMEOUT`.

## Ejecucion con Docker (desarrollo local)

Para ejecutar el BFF y el servicio de libros localmente:

1. Crea `.env` a partir de [`.env.example`](.env.example).
2. Define `USUARIOS_FUNCTION_BASE_URL` y `PRESTAMOS_FUNCTION_BASE_URL` segun el ambiente.
3. Ejecuta:

```powershell
docker compose up -d --build
```

Servicios levantados por Compose:

- `servicio-libros`
- `bff-springboot`

Compose no levanta `function-usuarios` ni `function-prestamos`. Esas funciones se consumen por URL externa configurada en el `.env`.
Compose tampoco levanta Oracle: la base se consume de forma remota mediante `ORACLE_JDBC_URL`.

Detener el entorno:

```powershell
docker compose down
```

Limpiar contenedores huerfanos antiguos:

```powershell
docker compose down --remove-orphans
```

## Despliegue de Azure Functions

Las Azure Functions se despliegan con zip-deploy via Azure CLI. Pasos para cualquiera de las cuatro funciones:

```bash
# 1. Compilar
cd function-<nombre>
mvn package -DskipTests -q

# 2. Crear ZIP del artefacto
cd target/azure-functions/<app-name>
# Usar jar para que el ZIP mantenga rutas con "/" compatibles con Linux/Flex.
# No usar Compress-Archive en Windows para este despliegue.
jar --create --file ../deploy-fwd.zip .

# 3. Desplegar
az functionapp deployment source config-zip \
  --resource-group rg-local \
  --name <app-name> \
  --src target/azure-functions/deploy-fwd.zip
```

Nombres de Function App por modulo:

| Modulo | Function App |
| --- | --- |
| function-usuarios | `biblio-usuarios-kath2026-v2` |
| function-prestamos | `biblio-prestamos-kath2026-v2` |
| function-libros | `biblio-libros-kath2026` |
| function-notificaciones | `biblio-notificaciones-kath2026` |

## Ejecucion manual (local)

Si necesitas correr todos los componentes fuera de Docker o validar funciones localmente:

1. Configura una instancia Oracle accesible. El flujo recomendado usa Oracle Cloud.
2. Si la base esta vacia, ejecuta [database/oracle/schema.sql](database/oracle/schema.sql).
3. Si la base esta vacia, ejecuta [database/oracle/data.sql](database/oracle/data.sql).
4. Ajusta el `.env` para apuntar a las funciones locales o remotas segun el escenario.
5. Levanta cada componente en una terminal separada.

Comandos:

```powershell
cd servicio-libros
mvn spring-boot:run
```

```powershell
cd function-usuarios
mvn azure-functions:run
```

```powershell
cd function-prestamos
mvn azure-functions:run
```

```powershell
cd bff-springboot
mvn spring-boot:run
```

Los archivos [function-usuarios/local.settings.sample.json](function-usuarios/local.settings.sample.json) y [function-prestamos/local.settings.sample.json](function-prestamos/local.settings.sample.json) sirven como referencia para la configuracion local de Azure Functions.

## Endpoints principales

Usuarios:

- `GET /api/usuarios`
- `GET /api/usuarios/{id}`
- `POST /api/usuarios`
- `PUT /api/usuarios/{id}`
- `DELETE /api/usuarios/{id}`

Prestamos:

- `GET /api/prestamos`
- `GET /api/prestamos/{id}`
- `POST /api/prestamos`
- `PUT /api/prestamos/{id}`
- `POST /api/prestamos/{id}/devolucion`
- `DELETE /api/prestamos/{id}`
- `POST /api/prestamos/notificar` - publica un evento de notificacion en Azure Service Bus (EDA)

Libros:

- `GET /api/libros`
- `GET /api/libros/{id}`
- `GET /api/libros/{id}/disponibilidad`
- `POST /api/libros`
- `PUT /api/libros/{id}/estado`

GraphQL (via BFF proxy):

- `POST /api/graphql/usuarios` - proxy a function-usuarios `/graphql`
- `POST /api/graphql/prestamos` - proxy a function-prestamos `/graphql`
- `POST /api/graphql/libros` - proxy a servicio-libros `/graphql`

Queries GraphQL disponibles:

Usuarios:

```graphql
{ usuarios { id nombre correo telefono } }
{ usuario(id: 1) { id nombre correo telefono } }
```

Prestamos:

```graphql
{ prestamos { id usuarioNombre libroTitulo estado fechaPrestamo } }
{ prestamo(id: 1) { id usuarioNombre libroTitulo estado } }
```

Libros:

```graphql
{ libros { id titulo autor isbn estado disponible } }
{ libro(id: 1) { id titulo autor isbn estado disponible } }
```

## Ejemplos de payload

Crear usuario:

```json
{
  "nombre": "Maria Lopez",
  "correo": "maria.lopez@correo.com",
  "telefono": "999555111"
}
```

Crear prestamo:

```json
{
  "usuarioId": 1,
  "libroId": 2,
  "fechaPrestamo": "2026-03-27",
  "fechaDevolucionEstimada": "2026-04-03",
  "observacion": "Prestamo inicial"
}
```

Registrar devolucion:

```json
{
  "fechaDevolucionReal": "2026-04-01",
  "observacion": "Devuelto en buen estado"
}
```

Crear libro:

```json
{
  "titulo": "Java Concurrency in Practice",
  "autor": "Brian Goetz",
  "isbn": "9780321349606",
  "descripcion": "Libro de concurrencia en Java"
}
```

Actualizar estado de un libro:

```json
{
  "estado": "PRESTADO"
}
```

Enviar notificacion de prestamo (EDA):

```json
{
  "prestamoId": 1,
  "usuarioId": 2,
  "libroTitulo": "Java Concurrency in Practice",
  "tipo": "CONFIRMACION_PRESTAMO"
}
```

## Reglas de negocio

- No se registra un prestamo para un usuario inexistente.
- No se registra un prestamo para un libro inexistente.
- No se presta un libro que no este disponible.
- Al registrar un prestamo, el libro pasa a estado `PRESTADO` (disponibilidad 1 -> 0 por copia fisica).
- Al registrar una devolucion, el libro vuelve a estado `DISPONIBLE` (disponibilidad 0 -> 1).
- Solo se actualizan prestamos en estado `ACTIVO`.
- Solo se eliminan prestamos en estado `DEVUELTO`.
- Al eliminar un usuario, se eliminan automaticamente todos sus prestamos asociados y se restaura la disponibilidad de los libros con prestamos activos. Se publica el evento `Biblioteca.UsuarioEliminado` en Event Grid como notificacion de auditoria.

## Verificacion

Asumiendo `BFF_PORT=8088`, validaciones minimas:

```powershell
curl http://localhost:8088/actuator/health
```

```powershell
curl http://localhost:8088/api/usuarios
```

```powershell
curl http://localhost:8088/api/prestamos
```

```powershell
curl http://localhost:8088/api/libros
```

Verificar el flujo EDA (requiere Azure Service Bus configurado y la funcion consumidora corriendo):

```powershell
curl -X POST http://localhost:8088/api/prestamos/notificar -H "Content-Type: application/json" -d '{"prestamoId":1,"usuarioId":2,"libroTitulo":"Java Concurrency in Practice","tipo":"CONFIRMACION_PRESTAMO"}'
```

Referencias utiles:

- BFF: `http://localhost:8088`
- Health BFF: `http://localhost:8088/actuator/health`
- Servicio de libros: `http://localhost:8083/api/libros`
- Health servicio de libros: `http://localhost:8083/actuator/health`
- GraphQL usuarios (BFF): `POST http://localhost:8088/api/graphql/usuarios`
- GraphQL prestamos (BFF): `POST http://localhost:8088/api/graphql/prestamos`
- GraphQL libros (BFF): `POST http://localhost:8088/api/graphql/libros`
- Function libros (Azure): `https://biblio-libros-kath2026.azurewebsites.net/api/libros`
- Function usuarios (Azure): `https://biblio-usuarios-kath2026-v2.azurewebsites.net/api/usuarios`
- Function prestamos (Azure): `https://biblio-prestamos-kath2026-v2.azurewebsites.net/api/prestamos`
- Function notificaciones (Azure): `https://biblio-notificaciones-kath2026.azurewebsites.net` (NotificacionConsumer via Service Bus - S5; PrestamoEventGridConsumer via Event Grid - S8)

## Base de datos

Scripts incluidos:

- [database/oracle/schema.sql](database/oracle/schema.sql)
- [database/oracle/data.sql](database/oracle/data.sql)

El esquema crea las tablas `USUARIOS`, `LIBROS` y `PRESTAMOS`, junto con sus restricciones e indices basicos.
Los scripts pueden ejecutarse sobre Oracle Autonomous Database o sobre una instancia Oracle usada para desarrollo manual.

## Documentacion adicional

- Video
- Diagrama S8 (Mermaid fuente): [docs/diagrama/diagrama-s8.mmd](docs/diagrama/diagrama-s8.mmd)
- Diagrama S8 (imagen): [docs/diagrama/diagrama-s8.png](docs/diagrama/diagrama-s8.png)

Proyecto individual desarrollado para la asignatura Desarrollo Cloud Native II.
