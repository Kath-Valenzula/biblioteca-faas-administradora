# Diagrama de arquitectura

```mermaid
flowchart LR
    A[Cliente REST\nPostman o cURL] --> B[BFF Spring Boot\nValidación y orquestación]
    B --> C[Función Java Usuarios\nCRUD Usuarios]
    B --> D[Función Java Préstamos\nCRUD y devolución]
    B --> E[Servicio Libros\nDisponibilidad y estado]
    C --> F[(Oracle\nUSUARIOS)]
    D --> F2[(Oracle\nPRESTAMOS)]
    D --> F3[(Oracle\nLIBROS)]
    E --> F3
    B -. logs .-> G[Actuator y logs]
    C -. logs .-> G
    D -. logs .-> G
    E -. logs .-> G
```

El flujo implementado respeta el diagrama académico solicitado:

- El cliente solo consume el BFF.
- El BFF expone la API REST y valida entradas.
- Usuarios y préstamos se resuelven mediante funciones Java compatibles con Azure Functions.
- Libros se maneja con un servicio dedicado.
- Oracle persiste usuarios, libros y préstamos.
- El monitoreo se limita a logs y endpoints básicos de salud y métricas.
