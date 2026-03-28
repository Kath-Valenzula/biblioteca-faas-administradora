# Diagrama de arquitectura

![Diagrama de arquitectura híbrida](./arquitectura-biblioteca.png)

```mermaid
flowchart LR
    %% === ENTORNO LOCAL (DOCKER) ===
    subgraph Local["💻 Entorno Local (Docker)"]
        Cliente["Cliente REST / Swagger / Postman<br/>- Pruebas API<br/>- Consumo HTTP"]
        BFF["BFF Spring Boot<br/>- Valida datos<br/>- Orquesta funciones JSON<br/>- Expone /api/*"]
        Libros["Servicio de Libros<br/>- CRUD libros<br/>- Disponibilidad<br/>- Respuesta JSON"]
    end

    %% === ENTORNO CLOUD / SERVERLESS ===
    subgraph Cloud["☁️ Entorno Cloud / Serverless"]
        Usuarios["Azure Function Usuarios<br/><i>biblio-usuarios-kath2026</i><br/>- CRUD usuarios<br/>- Respuesta JSON"]
        Prestamos["Azure Function Préstamos<br/><i>biblio-prestamos-kath2026</i><br/>- CRUD préstamos<br/>- Devoluciones<br/>- Respuesta JSON"]
        OracleDB[("Oracle Autonomous Database<br/>- Tablas USUARIOS<br/>- Tablas PRESTAMOS<br/>- Tablas LIBROS<br/>- Persistencia única")]
        Logs["Logs / Health<br/>- Trazabilidad<br/>- Registro de errores<br/>- Seguimiento operativo"]
    end

    %% === FLUJOS PRINCIPALES ===
    Cliente -->|"HTTP/REST"| BFF
    BFF -->|"HTTPS/REST"| Usuarios
    BFF -->|"HTTPS/REST"| Prestamos
    BFF -->|"HTTP/REST"| Libros

    Usuarios -->|"JDBC/TLS"| OracleDB
    Prestamos -->|"JDBC/TLS"| OracleDB
    Libros -->|"JDBC/TLS"| OracleDB

    %% === OBSERVABILIDAD ===
    BFF -.-> Logs
    Usuarios -.-> Logs
    Prestamos -.-> Logs
    Libros -.-> Logs

    %% === ESTILOS ===
    style Cliente fill:#fdf2f2,stroke:#cc0000,stroke-width:1px,color:#000
    style BFF fill:#fff3e0,stroke:#ff8800,stroke-width:1px,color:#000
    style Libros fill:#ffffcc,stroke:#cccc00,stroke-width:1px,color:#000
    style Usuarios fill:#e6f7ff,stroke:#0099cc,stroke-width:1px,color:#000
    style Prestamos fill:#e6f7ff,stroke:#0099cc,stroke-width:1px,color:#000
    style OracleDB fill:#cce5ff,stroke:#0066cc,stroke-width:1px,color:#000
    style Logs fill:#f0e6ff,stroke:#6600cc,stroke-width:1px,color:#000
```

La arquitectura actual del proyecto es híbrida:

- El cliente consume únicamente el BFF local.
- El BFF expone la API REST, valida entradas y orquesta funciones en Azure.
- Usuarios y préstamos se resuelven mediante Azure Functions en Java.
- El servicio de libros corre en Docker local y consulta la misma Oracle Autonomous Database.
- Usuarios, préstamos y libros persisten en una única base Oracle en la nube.
- La observabilidad se apoya en logs y endpoints básicos de salud.
