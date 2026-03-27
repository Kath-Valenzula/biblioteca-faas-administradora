# Sistema de Biblioteca - Semana 3

Proyecto academico individual para la Semana 3 de Cloud II. La solucion implementa una arquitectura backend only con un BFF en Spring Boot, dos funciones serverless en Java, un servicio dedicado para libros, persistencia en Oracle y ejecucion local con Docker.

## Objetivo del proyecto

Construir un sistema de biblioteca con arquitectura serverless y una capa BFF que permita:

- gestionar usuarios
- gestionar prestamos
- consultar y administrar libros
- validar reglas basicas del negocio
- dejar una base solida para despliegue posterior en Azure, sin depender todavia de Azure para operar localmente

## Arquitectura elegida

Se respeta la estructura academica solicitada:

1. Cliente REST
   - Postman o cURL
   - consume solo el BFF

2. BFF Spring Boot
   - expone endpoints REST JSON
   - valida payloads de entrada
   - orquesta usuarios, prestamos y libros
   - centraliza respuestas y codigos HTTP hacia el cliente

3. Capa logica
   - function-usuarios: Azure Function Java con CRUD de usuarios
   - function-prestamos: Azure Function Java con CRUD de prestamos y devolucion
   - servicio-libros: microservicio Spring Boot para libros, disponibilidad y estado

4. Persistencia
   - Oracle
   - tablas USUARIOS, LIBROS y PRESTAMOS

5. Monitoreo discreto
   - logs de aplicacion
   - endpoints basicos de health y metrics por Actuator en los servicios Spring Boot

Diagrama dentro del proyecto:

- [docs/diagrama/arquitectura-biblioteca.md](docs/diagrama/arquitectura-biblioteca.md)

## Componentes del sistema

### 1. BFF

Ruta: [bff-springboot](bff-springboot)

Responsabilidades:

- ser el punto de entrada unico
- validar datos de entrada
- reenviar solicitudes a funciones y servicio de libros
- devolver respuestas JSON consistentes

### 2. Funcion de usuarios

Ruta: [function-usuarios](function-usuarios)

Responsabilidades:

- crear usuario
- listar usuarios
- obtener usuario por id
- actualizar usuario
- eliminar usuario si no tiene prestamos registrados

### 3. Funcion de prestamos

Ruta: [function-prestamos](function-prestamos)

Responsabilidades:

- registrar prestamo
- listar prestamos
- obtener prestamo por id
- actualizar prestamo activo
- registrar devolucion
- eliminar prestamo solo si ya fue devuelto

Nota de implementacion:

- el registro y la devolucion del prestamo actualizan el estado del libro dentro de la misma transaccion JDBC sobre Oracle para conservar consistencia entre PRESTAMOS y LIBROS
- el servicio de libros sigue existiendo como componente dedicado para las operaciones REST de libros y disponibilidad desde el BFF

### 4. Servicio de libros

Ruta: [servicio-libros](servicio-libros)

Responsabilidades:

- registrar libros
- listar libros
- obtener libro por id
- consultar disponibilidad
- actualizar estado del libro

### 5. Base de datos Oracle

Ruta: [database/oracle](database/oracle)

Archivos:

- [database/oracle/schema.sql](database/oracle/schema.sql)
- [database/oracle/data.sql](database/oracle/data.sql)

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

## Variables de entorno

Archivo base:

- [.env.example](.env.example)

Variables principales:

- ORACLE_DB_HOST
- ORACLE_DB_PORT
- ORACLE_DB_SERVICE
- ORACLE_USERNAME
- ORACLE_PASSWORD
- ORACLE_APP_USER
- ORACLE_APP_PASSWORD
- ORACLE_JDBC_URL
- BFF_PORT
- LIBROS_PORT
- USUARIOS_FUNCTION_PORT
- PRESTAMOS_FUNCTION_PORT
- USUARIOS_FUNCTION_BASE_URL
- PRESTAMOS_FUNCTION_BASE_URL
- LIBROS_SERVICE_BASE_URL
- LOG_LEVEL_ROOT
- LOG_LEVEL_APP

## Endpoints principales del BFF

### Usuarios

- GET /api/usuarios
- GET /api/usuarios/{id}
- POST /api/usuarios
- PUT /api/usuarios/{id}
- DELETE /api/usuarios/{id}

Ejemplo POST:

```json
{
  "nombre": "Maria Lopez",
  "correo": "maria.lopez@correo.com",
  "telefono": "999555111"
}
```

### Prestamos

- GET /api/prestamos
- GET /api/prestamos/{id}
- POST /api/prestamos
- PUT /api/prestamos/{id}
- POST /api/prestamos/{id}/devolucion
- DELETE /api/prestamos/{id}

Ejemplo POST:

```json
{
  "usuarioId": 1,
  "libroId": 1,
  "fechaPrestamo": "2026-03-27",
  "fechaDevolucionEstimada": "2026-04-03",
  "observacion": "Prestamo inicial"
}
```

Ejemplo devolucion:

```json
{
  "fechaDevolucionReal": "2026-04-01",
  "observacion": "Devuelto en buen estado"
}
```

### Libros

- GET /api/libros
- GET /api/libros/{id}
- GET /api/libros/{id}/disponibilidad
- POST /api/libros
- PUT /api/libros/{id}/estado

Ejemplo POST:

```json
{
  "titulo": "Java Concurrency in Practice",
  "autor": "Brian Goetz",
  "isbn": "9780321349606",
  "descripcion": "Libro de concurrencia en Java"
}
```

Ejemplo cambio de estado:

```json
{
  "estado": "PRESTADO"
}
```

## Reglas de negocio implementadas

- no se puede registrar un prestamo para un usuario inexistente
- no se puede registrar un prestamo para un libro inexistente
- no se puede prestar un libro no disponible
- al registrar un prestamo, el libro pasa a PRESTADO
- al registrar una devolucion, el libro pasa a DISPONIBLE
- solo se puede actualizar un prestamo ACTIVO
- solo se puede eliminar un prestamo DEVUELTO
- no se elimina un usuario si tiene prestamos registrados

## Ejecucion local con Docker

### Requisitos

- Docker Desktop
- Docker Compose

### Pasos

1. Crear un archivo .env a partir de [.env.example](.env.example).
2. Desde la raiz del proyecto ejecutar:

```powershell
docker compose up --build
```

3. Cuando todos los contenedores esten levantados, consumir el BFF en:

```text
http://localhost:8080
```

### Servicios expuestos por defecto

- BFF: http://localhost:8080
- Libros: http://localhost:8083
- Funcion usuarios: http://localhost:7071/api
- Funcion prestamos: http://localhost:7072/api
- Oracle: localhost:1521

## Ejecucion local sin Docker para Java

### Requisitos

- JDK 17
- Maven
- Azure Functions Core Tools v4
- Oracle local o contenedor Oracle en ejecucion

### Orden recomendado

1. Levantar Oracle con Docker o usar una instancia Oracle disponible.
2. Ejecutar los scripts [database/oracle/schema.sql](database/oracle/schema.sql) y [database/oracle/data.sql](database/oracle/data.sql).
3. Copiar [.env.example](.env.example) o exportar variables equivalentes.
4. Iniciar el servicio de libros:

```powershell
cd servicio-libros
mvn spring-boot:run
```

5. Iniciar la funcion de usuarios:

```powershell
cd function-usuarios
mvn azure-functions:run
```

6. Iniciar la funcion de prestamos:

```powershell
cd function-prestamos
mvn azure-functions:run
```

7. Iniciar el BFF:

```powershell
cd bff-springboot
mvn spring-boot:run
```

## Build del monorepo

Desde la raiz:

```powershell
mvn clean package
```

## Relacion con el diagrama academico

La implementacion respeta el flujo:

- cliente REST -> BFF
- BFF -> funciones serverless y servicio de libros
- funciones y servicio -> Oracle
- monitoreo limitado a logs y metricas basicas

No se agregaron frontend, JWT, Redis, colas, eventos ni otros componentes no solicitados.

## Git y organizacion

- se deja estructura limpia por modulos
- se incluye [.gitignore](.gitignore)
- no se realiza push automaticamente
- la configuracion del remoto puede dejarse preparada localmente

## Siguientes pasos sugeridos para Azure

Todavia no implementados:

1. publicar las funciones Java en Azure Functions
2. desplegar el BFF y el servicio de libros en App Service o Container Apps
3. mover secretos a Azure Key Vault
4. parametrizar ambientes dev, test y prod
5. agregar pipeline CI/CD
