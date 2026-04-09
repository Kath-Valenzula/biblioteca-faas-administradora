# Sistema de Biblioteca

Repositorio de la Actividad Sumativa 2 para la implementacion de un sistema de biblioteca con arquitectura hibrida, componentes serverless y arquitectura orientada a eventos (EDA). El proyecto expone una API backend-only compuesta por un BFF en Spring Boot, cuatro Azure Functions en Java, un servicio de libros, endpoints GraphQL, integracion asincrona mediante Azure Service Bus y scripts de base de datos Oracle.

## Alcance

- API REST para usuarios, prestamos y libros.
- API GraphQL para consultas de usuarios, prestamos y libros.
- Backend for Frontend desarrollado con Spring Boot con proxy GraphQL.
- Cuatro funciones serverless en Java desplegadas en Azure:
  - `function-usuarios`: CRUD de usuarios + GraphQL.
  - `function-prestamos`: CRUD de prestamos + GraphQL.
  - `function-libros`: CRUD de libros + GraphQL.
  - `function-notificaciones`: consumidor de eventos de Service Bus.
- Servicio de libros separado del BFF (Spring Boot).
- Arquitectura orientada a eventos (EDA) con Azure Service Bus como message broker.
- Productor de eventos en el BFF para notificaciones de prestamos.
- Consumidor de eventos en Azure Function dedicada con trigger de Service Bus.
- Scripts SQL para creacion y carga inicial de datos en Oracle.
- Sin frontend incluido en el repositorio.

## Componentes

- [bff-springboot](bff-springboot): punto de entrada para el cliente. Valida payloads, expone la API REST principal, proxy GraphQL hacia funciones y servicio de libros, y orquesta llamadas downstream.
- [function-usuarios](function-usuarios): Azure Function en Java para CRUD de usuarios y consultas GraphQL.
- [function-prestamos](function-prestamos): Azure Function en Java para CRUD de prestamos, devoluciones y consultas GraphQL.
- [function-libros](function-libros): Azure Function en Java para CRUD de libros, disponibilidad y consultas GraphQL.
- [function-notificaciones](function-notificaciones): Azure Function en Java dedicada al consumo de eventos de notificacion via Service Bus.
- [servicio-libros](servicio-libros): microservicio Spring Boot para gestion de libros y disponibilidad (GraphQL incluido).
- [database/oracle](database/oracle): scripts `schema.sql` y `data.sql`.
- [diagrama semana 5.png](diagrama%20semana%205.png): diagrama de arquitectura del proyecto.

## Arquitectura

- El cliente consume unicamente el BFF.
- El BFF enruta llamadas REST a usuarios, prestamos y libros.
- El BFF expone un proxy GraphQL unificado bajo `/api/graphql/{servicio}`.
- Las funciones de usuarios, prestamos y libros se consumen por URL configurable; en el flujo validado apuntan a Azure.
- La funcion de notificaciones es exclusivamente consumidora de eventos de Service Bus (sin endpoints HTTP).
- El servicio de libros corre como servicio HTTP independiente dentro de Docker.
- Usuarios, prestamos y libros persisten en una unica Oracle Autonomous Database configurada por variables de entorno.
- El archivo [docker-compose.yml](docker-compose.yml) levanta el BFF y el servicio de libros para validacion local.

Flujo EDA (Arquitectura Orientada a Eventos):

- El BFF actua como productor: al invocar `POST /api/prestamos/notificar`, serializa el payload a JSON y lo publica en la cola `prestamo-notificaciones` de Azure Service Bus.
- La Azure Function `NotificacionConsumer` en `function-notificaciones` actua como consumidor dedicado: escucha la cola mediante `@ServiceBusQueueTrigger`, deserializa el mensaje y simula el envio de una notificacion.
- La comunicacion entre productor y consumidor es completamente asincrona y desacoplada.

Azure Functions desplegadas:

| Function App | Modulo | Funciones |
|---|---|---|
| `biblio-usuarios-kath2026-v2` | function-usuarios | UsuariosCrear, UsuariosListar, UsuariosObtener, UsuariosActualizar, UsuariosEliminar, UsuariosGraphQL |
| `biblio-prestamos-kath2026-v2` | function-prestamos | PrestamosCrear, PrestamosListar, PrestamosObtener, PrestamosActualizar, PrestamosDevolver, PrestamosEliminar, PrestamosGraphQL |
| `biblio-libros-kath2026` | function-libros | LibrosCrear, LibrosListar, LibrosObtener, LibrosActualizarEstado, LibrosDisponibilidad, LibrosGraphQL |
| `biblio-notificaciones-kath2026` | function-notificaciones | NotificacionConsumer |

## Modo de validacion actual

- `docker compose` levanta `bff-springboot` y `servicio-libros`.
- `servicio-libros` se conecta a Oracle Cloud usando `ORACLE_JDBC_URL`, `ORACLE_APP_USER` y `ORACLE_APP_PASSWORD`.
- `USUARIOS_FUNCTION_BASE_URL` y `PRESTAMOS_FUNCTION_BASE_URL` apuntan normalmente a Azure Functions.
- El BFF queda disponible normalmente en `http://localhost:8088`.

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
      PrestamoFunction.java              # CRUD REST prestamos
      PrestamoGraphQLFunction.java       # GraphQL prestamos
  function-libros/
    src/main/java/com/biblioteca/functions/libros/
      LibroFunction.java                 # CRUD REST libros
      LibroGraphQLFunction.java          # GraphQL libros
  function-notificaciones/
    src/main/java/com/biblioteca/functions/notificaciones/
      NotificacionConsumerFunction.java  # consumidor EDA dedicado (@ServiceBusQueueTrigger)
  servicio-libros/
  database/
    oracle/
      schema.sql
      data.sql
  diagrama semana 5.png
  docs/
    diagrama/
      arquitectura-biblioteca.png
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
- Azure Service Bus (SDK `azure-messaging-servicebus`)
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

Configuracion de Azure Service Bus:

```env
SERVICEBUS_CONNECTION_STRING=Endpoint=sb://<tu-namespace>.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=<tu-clave>
SERVICEBUS_QUEUE_NAME=prestamo-notificaciones
```

Para las Azure Functions, la conexion de Service Bus se configura en `local.settings.json` bajo las claves `ServiceBusConnection` y `SERVICEBUS_QUEUE_NAME`. Usa [function-prestamos/local.settings.sample.json](function-prestamos/local.settings.sample.json) como referencia.

Notas operativas:

- En Docker, el BFF usa `http://servicio-libros:8083/api` para comunicarse con `servicio-libros`.
- `servicio-libros` toma su conexion Oracle directamente desde el `.env`.
- El puerto publicado del BFF depende de `BFF_PORT`. En este repositorio se valida normalmente con `8088`.
- Los timeouts del proxy HTTP del BFF se controlan con `BFF_DOWNSTREAM_CONNECT_TIMEOUT` y `BFF_DOWNSTREAM_READ_TIMEOUT`.

## Ejecucion con Docker

Modo recomendado para validar el BFF y el servicio de libros:

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

## Ejecucion manual

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
- `POST /api/prestamos/notificar` — publica un evento de notificacion en Azure Service Bus (EDA)

Libros:

- `GET /api/libros`
- `GET /api/libros/{id}`
- `GET /api/libros/{id}/disponibilidad`
- `POST /api/libros`
- `PUT /api/libros/{id}/estado`

GraphQL (via BFF proxy):

- `POST /api/graphql/usuarios` — proxy a function-usuarios `/graphql`
- `POST /api/graphql/prestamos` — proxy a function-prestamos `/graphql`
- `POST /api/graphql/libros` — proxy a servicio-libros `/graphql`

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
- Al registrar un prestamo, el libro pasa a estado `PRESTADO`.
- Al registrar una devolucion, el libro vuelve a estado `DISPONIBLE`.
- Solo se actualizan prestamos en estado `ACTIVO`.
- Solo se eliminan prestamos en estado `DEVUELTO`.
- No se elimina un usuario si tiene prestamos asociados.

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
- Function notificaciones (Azure): `https://biblio-notificaciones-kath2026.azurewebsites.net` (solo trigger Service Bus)

## Base de datos

Scripts incluidos:

- [database/oracle/schema.sql](database/oracle/schema.sql)
- [database/oracle/data.sql](database/oracle/data.sql)

El esquema crea las tablas `USUARIOS`, `LIBROS` y `PRESTAMOS`, junto con sus restricciones e indices basicos.
Los scripts pueden ejecutarse sobre Oracle Autonomous Database o sobre una instancia Oracle usada para desarrollo manual.

## Documentacion adicional

- Video
- Imagen del diagrama: [docs/diagrama/arquitectura-biblioteca.png](docs/diagrama/arquitectura-biblioteca.png)

Proyecto individual desarrollado para la asignatura Desarrollo Cloud Native II.