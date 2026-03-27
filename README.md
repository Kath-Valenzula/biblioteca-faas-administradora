# Sistema de Biblioteca

Backend académico para gestionar usuarios, libros y préstamos de una biblioteca. La solución está dividida en un BFF en Spring Boot, dos funciones Java, un servicio de libros y una base Oracle, todo preparado para ejecución local con Docker.

## Objetivo del proyecto

El objetivo es resolver el caso de biblioteca pedido en la actividad con una arquitectura simple, modular y fácil de explicar en una demo:

- alta, consulta, actualización y baja de usuarios
- registro y devolución de préstamos
- consulta y administración de libros
- validación de disponibilidad antes de prestar
- ejecución local reproducible con Docker

## Arquitectura implementada

La arquitectura se mantiene backend only y respeta el diseño solicitado:

1. Cliente REST
2. BFF en Spring Boot
3. Función serverless de usuarios
4. Función serverless de préstamos
5. Servicio de libros y disponibilidad
6. Base de datos Oracle
7. Orquestación local con Docker Compose

No se agregaron componentes fuera del alcance pedido: no hay frontend, JWT, Redis, colas, mensajería ni circuit breakers.

## Componentes del sistema

### BFF Spring Boot

Ubicación: [bff-springboot](bff-springboot)

Responsabilidades:

- exponer la API principal al cliente
- validar payloads de entrada
- enrutar llamadas hacia usuarios, préstamos y libros
- unificar respuestas HTTP y JSON

### Función de Usuarios

Ubicación: [function-usuarios](function-usuarios)

Responsabilidades:

- crear usuarios
- listar usuarios
- obtener usuario por id
- actualizar usuarios
- eliminar usuarios sin préstamos asociados

### Función de Préstamos

Ubicación: [function-prestamos](function-prestamos)

Responsabilidades:

- registrar préstamos
- listar préstamos
- obtener préstamo por id
- actualizar préstamos activos
- registrar devoluciones
- impedir operaciones inválidas según estado y disponibilidad

### Servicio de Libros

Ubicación: [servicio-libros](servicio-libros)

Responsabilidades:

- registrar libros
- listar libros
- consultar libro por id
- consultar disponibilidad
- cambiar estado del libro

### Base de datos Oracle

Ubicación: [database/oracle](database/oracle)

Archivos relevantes:

- [database/oracle/schema.sql](database/oracle/schema.sql)
- [database/oracle/data.sql](database/oracle/data.sql)

### Documentación de arquitectura

Ubicación: [docs/diagrama/arquitectura-biblioteca.md](docs/diagrama/arquitectura-biblioteca.md)

## Estructura del repositorio

```text
biblioteca-faas-semana3/
  bff-springboot/
  function-prestamos/
  function-usuarios/
  servicio-libros/
  database/
    oracle/
      schema.sql
      data.sql
  docs/
    diagrama/
      arquitectura-biblioteca.md
  .env.example
  .gitignore
  docker-compose.yml
  pom.xml
  README.md
```

## Tecnologías utilizadas

- Java 17
- Spring Boot 3.3.5
- Azure Functions Java
- Oracle Database Free
- JDBC
- Spring Data JPA
- Docker
- Docker Compose
- Maven

## Requisitos previos

Para ejecución con Docker:

- Docker Desktop levantado
- Docker Compose disponible

Para ejecución manual sin Docker:

- JDK 17
- Maven
- Azure Functions Core Tools v4
- una instancia Oracle accesible

## Variables de entorno necesarias

Tomar como base [.env.example](.env.example) y copiarlo a un archivo `.env` local antes de ejecutar con Docker o sin Docker.

Variables utilizadas:

- `ORACLE_DB_HOST`
- `ORACLE_DB_PORT`
- `ORACLE_DB_SERVICE`
- `ORACLE_USERNAME`
- `ORACLE_PASSWORD`
- `ORACLE_APP_USER`
- `ORACLE_APP_PASSWORD`
- `ORACLE_JDBC_URL`
- `BFF_PORT`
- `LIBROS_PORT`
- `USUARIOS_FUNCTION_PORT`
- `PRESTAMOS_FUNCTION_PORT`
- `USUARIOS_FUNCTION_BASE_URL`
- `PRESTAMOS_FUNCTION_BASE_URL`
- `LIBROS_SERVICE_BASE_URL`
- `LOG_LEVEL_ROOT`
- `LOG_LEVEL_APP`

Valor usado en esta máquina durante la validación:

- `BFF_PORT=8088`

Se usó 8088 porque durante la validación local el 8080 ya estaba ocupado por otro servicio.

## Cómo ejecutar en local

Ejecución manual sugerida:

1. Levantar Oracle o usar el contenedor Oracle del proyecto.
2. Ejecutar [database/oracle/schema.sql](database/oracle/schema.sql).
3. Ejecutar [database/oracle/data.sql](database/oracle/data.sql).
4. Crear `.env` desde [.env.example](.env.example).
5. Levantar el servicio de libros.
6. Levantar la función de usuarios.
7. Levantar la función de préstamos.
8. Levantar el BFF.

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

## Cómo ejecutar con Docker

1. Copiar [.env.example](.env.example) como `.env`.
2. Ajustar `BFF_PORT` si el puerto elegido ya está ocupado.
3. Desde la raíz, ejecutar:

```powershell
docker compose up --build
```

Para bajar el entorno:

```powershell
docker compose down
```

Si se necesita reconstruir Oracle desde cero:

```powershell
docker compose down -v
docker compose up --build
```

## Orden recomendado de levantamiento

1. Oracle
2. Servicio de libros
3. Función de usuarios
4. Función de préstamos
5. BFF

Ese orden ya está modelado en [docker-compose.yml](docker-compose.yml) mediante `depends_on` y health checks.

## Puertos usados

- BFF en contenedor: `8080`
- BFF publicado localmente en esta máquina: `8088`
- Servicio de libros: `8083`
- Función de usuarios: `7071`
- Función de préstamos: `7072`
- Oracle: `1521`

El puerto externo del BFF depende de `BFF_PORT`. En esta entrega se documenta y se validó con `8088`.

## Endpoints principales

### Usuarios

- `GET /api/usuarios`
- `GET /api/usuarios/{id}`
- `POST /api/usuarios`
- `PUT /api/usuarios/{id}`
- `DELETE /api/usuarios/{id}`

### Préstamos

- `GET /api/prestamos`
- `GET /api/prestamos/{id}`
- `POST /api/prestamos`
- `PUT /api/prestamos/{id}`
- `POST /api/prestamos/{id}/devolucion`
- `DELETE /api/prestamos/{id}`

### Libros

- `GET /api/libros`
- `GET /api/libros/{id}`
- `GET /api/libros/{id}/disponibilidad`
- `POST /api/libros`
- `PUT /api/libros/{id}/estado`

## Ejemplos de uso con JSON

### Crear usuario

```json
{
  "nombre": "Maria Lopez",
  "correo": "maria.lopez@correo.com",
  "telefono": "999555111"
}
```

### Crear préstamo

```json
{
  "usuarioId": 1,
  "libroId": 2,
  "fechaPrestamo": "2026-03-27",
  "fechaDevolucionEstimada": "2026-04-03",
  "observacion": "Prestamo inicial"
}
```

### Registrar devolución

```json
{
  "fechaDevolucionReal": "2026-04-01",
  "observacion": "Devuelto en buen estado"
}
```

### Crear libro

```json
{
  "titulo": "Java Concurrency in Practice",
  "autor": "Brian Goetz",
  "isbn": "9780321349606",
  "descripcion": "Libro de concurrencia en Java"
}
```

### Cambiar estado de un libro

```json
{
  "estado": "PRESTADO"
}
```

## Reglas de negocio importantes

- no se registra un préstamo para un usuario inexistente
- no se registra un préstamo para un libro inexistente
- no se presta un libro que ya no está disponible
- al registrar un préstamo, el libro pasa a `PRESTADO`
- al registrar una devolución, el libro vuelve a `DISPONIBLE`
- solo se actualizan préstamos en estado `ACTIVO`
- solo se eliminan préstamos en estado `DEVUELTO`
- no se elimina un usuario si tiene préstamos registrados

## Cómo verificar que el sistema está funcionando

Con Docker arriba, validar en este orden:

1. comprobar que Oracle esté healthy en `docker compose ps`
2. abrir el health del BFF
3. abrir el health del servicio de libros
4. consultar usuarios, libros y préstamos a través del BFF
5. probar un alta de usuario y un préstamo válido

Comandos mínimos de verificación:

```powershell
curl http://localhost:8088/actuator/health
```

```powershell
curl http://localhost:8088/api/usuarios
```

```powershell
curl http://localhost:8088/api/libros
```

```powershell
curl http://localhost:8088/api/prestamos
```

## Links de prueba local

Asumiendo `BFF_PORT=8088`:

- BFF: http://localhost:8088
- Health del BFF: http://localhost:8088/actuator/health
- Usuarios: http://localhost:8088/api/usuarios
- Préstamos: http://localhost:8088/api/prestamos
- Libros: http://localhost:8088/api/libros
- Disponibilidad de libro 2: http://localhost:8088/api/libros/2/disponibilidad
- Health del servicio de libros: http://localhost:8083/actuator/health
- Servicio de libros directo: http://localhost:8083/api/libros
- Función de usuarios directa: http://localhost:7071/api/usuarios
- Función de préstamos directa: http://localhost:7072/api/prestamos

## Notas útiles para la demo

- el puerto externo del BFF depende de `BFF_PORT`
- si `8080` está ocupado, usar `8088` u otro puerto libre
- si Oracle ya tenía datos de una corrida anterior, puede ser necesario ejecutar `docker compose down -v`
- el archivo `.env` es local y no forma parte de la entrega final
- conviene grabar la demo con Docker ya levantado para no perder tiempo esperando Oracle

## Posibles siguientes pasos hacia Azure

Sin cambiar la arquitectura actual, los siguientes pasos razonables serían:

1. publicar las funciones Java en Azure Functions
2. desplegar el BFF y el servicio de libros en App Service o Container Apps
3. mover credenciales a Azure Key Vault
4. separar configuración por ambiente
5. incorporar CI/CD

## Autoría individual

Proyecto individual desarrollado para la asignatura Desarrollo Cloud Native II.
