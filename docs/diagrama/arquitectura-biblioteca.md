# Diagrama de arquitectura

```mermaid
flowchart LR
    A[Cliente REST\nPostman o cURL] --> B[BFF Spring Boot\nValidacion y orquestacion]
    B --> C[Funcion Java Usuarios\nCRUD Usuarios]
    B --> D[Funcion Java Prestamos\nCRUD y devolucion]
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

El flujo implementado respeta el diagrama academico solicitado:

- El cliente solo consume el BFF.
- El BFF expone la API REST y valida entradas.
- Usuarios y prestamos se resuelven mediante funciones Java compatibles con Azure Functions.
- Libros se maneja con un servicio dedicado.
- Oracle persiste usuarios, libros y prestamos.
- El monitoreo se limita a logs y endpoints basicos de salud/metricas.
