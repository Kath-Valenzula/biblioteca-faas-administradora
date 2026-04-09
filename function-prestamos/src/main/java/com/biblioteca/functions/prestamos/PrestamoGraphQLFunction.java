package com.biblioteca.functions.prestamos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class PrestamoGraphQLFunction {

    private static final String SDL = """
            type Prestamo {
                id: Int
                usuarioId: Int
                usuarioNombre: String
                libroId: Int
                libroTitulo: String
                fechaPrestamo: String
                fechaDevolucionEstimada: String
                fechaDevolucionReal: String
                estado: String
                observacion: String
            }

            type Query {
                prestamos: [Prestamo]
                prestamo(id: Int!): Prestamo
            }
            """;

    private static final GraphQL GRAPHQL_ENGINE = buildGraphQL();

    private static GraphQL buildGraphQL() {
        TypeDefinitionRegistry registry = new SchemaParser().parse(SDL);
        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder
                        .dataFetcher("prestamos", env -> fetchAllPrestamos())
                        .dataFetcher("prestamo", env -> fetchPrestamoById(env.getArgument("id")))
                )
                .build();
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(registry, wiring);
        return GraphQL.newGraphQL(schema).build();
    }

    @FunctionName("PrestamosGraphQL")
    public HttpResponseMessage graphql(
            @HttpTrigger(name = "req",
                    methods = {HttpMethod.POST},
                    route = "graphql",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        try {
            String body = request.getBody().orElse("{}");
            ObjectMapper mapper = JsonSupport.mapper();
            var node = mapper.readTree(body);
            String query = node.has("query") ? node.get("query").asText() : "";
            Map<String, Object> variables = node.has("variables") && !node.get("variables").isNull()
                    ? mapper.convertValue(node.get("variables"), Map.class)
                    : Collections.emptyMap();

            ExecutionInput input = ExecutionInput.newExecutionInput()
                    .query(query)
                    .variables(variables)
                    .build();
            ExecutionResult result = GRAPHQL_ENGINE.execute(input);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("data", result.getData());
            if (!result.getErrors().isEmpty()) {
                response.put("errors", result.getErrors());
            }

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(mapper.writeValueAsString(response))
                    .build();
        } catch (Exception ex) {
            context.getLogger().severe("Error en GraphQL prestamos: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body("{\"errors\":[{\"message\":\"Error interno en GraphQL\"}]}")
                    .build();
        }
    }

    private static List<Map<String, Object>> fetchAllPrestamos() throws Exception {
        String sql = """
                SELECT p.ID, p.USUARIO_ID, u.NOMBRE AS USUARIO_NOMBRE,
                       p.LIBRO_ID, l.TITULO AS LIBRO_TITULO,
                       p.FECHA_PRESTAMO, p.FECHA_DEVOLUCION_ESTIMADA,
                       p.FECHA_DEVOLUCION_REAL, p.ESTADO, p.OBSERVACION
                FROM PRESTAMOS p
                LEFT JOIN USUARIOS u ON p.USUARIO_ID = u.ID
                LEFT JOIN LIBROS l ON p.LIBRO_ID = l.ID
                ORDER BY p.ID DESC
                """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapPrestamo(rs));
            }
        }
        return list;
    }

    private static Map<String, Object> fetchPrestamoById(int id) throws Exception {
        String sql = """
                SELECT p.ID, p.USUARIO_ID, u.NOMBRE AS USUARIO_NOMBRE,
                       p.LIBRO_ID, l.TITULO AS LIBRO_TITULO,
                       p.FECHA_PRESTAMO, p.FECHA_DEVOLUCION_ESTIMADA,
                       p.FECHA_DEVOLUCION_REAL, p.ESTADO, p.OBSERVACION
                FROM PRESTAMOS p
                LEFT JOIN USUARIOS u ON p.USUARIO_ID = u.ID
                LEFT JOIN LIBROS l ON p.LIBRO_ID = l.ID
                WHERE p.ID = ?
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPrestamo(rs);
                }
            }
        }
        return null;
    }

    private static Map<String, Object> mapPrestamo(ResultSet rs) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getInt("ID"));
        m.put("usuarioId", rs.getInt("USUARIO_ID"));
        m.put("usuarioNombre", rs.getString("USUARIO_NOMBRE"));
        m.put("libroId", rs.getInt("LIBRO_ID"));
        m.put("libroTitulo", rs.getString("LIBRO_TITULO"));
        m.put("fechaPrestamo", rs.getDate("FECHA_PRESTAMO") != null ? rs.getDate("FECHA_PRESTAMO").toString() : null);
        m.put("fechaDevolucionEstimada", rs.getDate("FECHA_DEVOLUCION_ESTIMADA") != null ? rs.getDate("FECHA_DEVOLUCION_ESTIMADA").toString() : null);
        m.put("fechaDevolucionReal", rs.getDate("FECHA_DEVOLUCION_REAL") != null ? rs.getDate("FECHA_DEVOLUCION_REAL").toString() : null);
        m.put("estado", rs.getString("ESTADO"));
        m.put("observacion", rs.getString("OBSERVACION"));
        return m;
    }
}
