# Diagrama de arquitectura

![Diagrama de arquitectura híbrida](./arquitectura-biblioteca.png)

```mermaid
flowchart LR
    %% === ENTORNO LOCAL (DOCKER) ===
    subgraph Local["💻 Entorno Local (Docker)"]
        Cliente["Cliente REST / Postman / GraphQL Playground<br/>- Pruebas REST y GraphQL<br/>- Consumo HTTP"]
        BFF["BFF Spring Boot<br/>- Valida datos<br/>- Orquesta REST, GraphQL y Service Bus<br/>- Expone /api/*"]
        Libros["Servicio de Libros<br/>- CRUD libros (REST)<br/>- Consultas libros (GraphQL)<br/>- Disponibilidad"]
    end

    %% === ENTORNO CLOUD / SERVERLESS ===
    subgraph Cloud["☁️ Entorno Cloud / Serverless"]
        Usuarios["Azure Function Usuarios<br/><i>biblio-usuarios-kath2026</i><br/>- CRUD usuarios (REST)<br/>- Consultas usuarios (GraphQL)"]
        Prestamos["Azure Function Préstamos<br/><i>biblio-prestamos-kath2026</i><br/>- CRUD préstamos (REST)<br/>- Devoluciones<br/>- Consumer Service Bus"]
        ServiceBus["Azure Service Bus<br/><i>servicebus-katherine2026</i><br/>- Cola: prestamo-notificaciones<br/>- Mensajería asíncrona"]
        OracleDB[("Oracle Autonomous Database<br/>- Tablas USUARIOS<br/>- Tablas PRESTAMOS<br/>- Tablas LIBROS<br/>- Persistencia única")]
        Logs["Logs / Health<br/>- Trazabilidad<br/>- Registro de errores<br/>- Seguimiento operativo"]
    end

    %% === FLUJOS REST (línea continua) ===
    Cliente -->|"HTTP"| BFF
    BFF -->|"HTTPS / REST"| Prestamos
    BFF -->|"HTTP / REST"| Libros

    %% === FLUJOS GRAPHQL (línea gruesa discontinua) ===
    BFF ==>|"HTTPS / GraphQL"| Usuarios
    BFF ==>|"HTTP / GraphQL"| Libros

    %% === FLUJO SERVICE BUS (línea punteada) ===
    BFF -.->|"AMQP / Service Bus"| ServiceBus
    ServiceBus -.->|"Trigger Queue"| Prestamos

    %% === PERSISTENCIA ===
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
    style BFF fill:#fff3e0,stroke:#ff8800,stroke-width:2px,color:#000
    style Libros fill:#ffffcc,stroke:#cccc00,stroke-width:1px,color:#000
    style Usuarios fill:#e6f7ff,stroke:#0099cc,stroke-width:1px,color:#000
    style Prestamos fill:#e6f7ff,stroke:#0099cc,stroke-width:1px,color:#000
    style ServiceBus fill:#e6ffe6,stroke:#009900,stroke-width:1px,color:#000
    style OracleDB fill:#cce5ff,stroke:#0066cc,stroke-width:1px,color:#000
    style Logs fill:#f0e6ff,stroke:#6600cc,stroke-width:1px,color:#000
```

## Leyenda de flechas

| Estilo | Protocolo | Ejemplo |
|--------|-----------|---------|
| `──▶` continua | REST (HTTP/HTTPS) | BFF → Préstamos, BFF → Libros |
| `══▶` gruesa | GraphQL (HTTP/HTTPS) | BFF → Usuarios, BFF → Libros |
| `- - ▶` punteada | Service Bus (AMQP) | BFF → Service Bus → Préstamos |

## Descripción

La arquitectura del proyecto es híbrida y soporta tres protocolos de comunicación:

- **REST**: El BFF proxea operaciones CRUD de préstamos y libros mediante HTTP/HTTPS estándar.
- **GraphQL**: Las consultas de usuarios y libros se resuelven mediante endpoints GraphQL dedicados.
- **Service Bus**: Las notificaciones de préstamos se envían de forma asíncrona a través de Azure Service Bus. La función de préstamos consume los mensajes de la cola.
- Usuarios y préstamos se resuelven mediante Azure Functions en Java.
- El servicio de libros corre en Docker local y consulta la misma Oracle Autonomous Database.
- Todos los servicios persisten en una única base Oracle en la nube (OCI Santiago).
- La observabilidad se apoya en logs de trazabilidad y endpoints de salud (actuator).
