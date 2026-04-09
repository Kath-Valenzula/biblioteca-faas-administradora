package com.biblioteca.functions.libros;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LibroGraphQLFunction {

    private static final String SDL = """
            type Libro {
                id: ID!
                titulo: String!
                autor: String!
                isbn: String!
                estado: String!
                descripcion: String
                disponible: Boolean!
            }

            type Query {
                libros: [Libro!]!
                libro(id: ID!): Libro
            }
            """;

    private static final GraphQL GRAPHQL = buildGraphQL();

    private static GraphQL buildGraphQL() {
        TypeDefinitionRegistry registry = new SchemaParser().parse(SDL);
        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder
                        .dataFetcher("libros", env -> fetchAllLibros())
                        .dataFetcher("libro", env -> {
                            String id = env.getArgument("id");
                            return fetchLibroById(Long.parseLong(id));
                        })
                )
                .build();
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(registry, wiring);
        return GraphQL.newGraphQL(schema).build();
    }

    @FunctionName("LibrosGraphQL")
    public HttpResponseMessage graphql(
            @HttpTrigger(name = "request", methods = {HttpMethod.POST}, route = "graphql",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        context.getLogger().info("[GraphQL] Consulta GraphQL de libros recibida");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JsonSupport.mapper().readValue(
                    request.getBody().orElse("{}"), Map.class);

            String query = (String) body.get("query");
            if (query == null || query.isBlank()) {
                return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                        "El campo 'query' es obligatorio en la peticion GraphQL", null);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) body.getOrDefault("variables",
                    Collections.emptyMap());

            ExecutionInput input = ExecutionInput.newExecutionInput()
                    .query(query)
                    .variables(variables == null ? Collections.emptyMap() : variables)
                    .build();

            ExecutionResult result = GRAPHQL.execute(input);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("data", result.getData());
            if (!result.getErrors().isEmpty()) {
                response.put("errors", result.getErrors().stream()
                        .map(graphql.GraphQLError::toSpecification).toList());
            }

            context.getLogger().info("[GraphQL] Consulta ejecutada correctamente");
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(JsonSupport.mapper().writeValueAsString(response))
                    .build();

        } catch (Exception ex) {
            context.getLogger().severe("[GraphQL] Error procesando consulta: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error procesando consulta GraphQL", null);
        }
    }

    private static List<Map<String, Object>> fetchAllLibros() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT ID, TITULO, AUTOR, ISBN, ESTADO, DESCRIPCION FROM LIBROS ORDER BY ID");
             ResultSet rs = stmt.executeQuery()) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    private static Map<String, Object> fetchLibroById(Long id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT ID, TITULO, AUTOR, ISBN, ESTADO, DESCRIPCION FROM LIBROS WHERE ID = ?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(rs.getLong("ID")));
        m.put("titulo", rs.getString("TITULO"));
        m.put("autor", rs.getString("AUTOR"));
        m.put("isbn", rs.getString("ISBN"));
        m.put("estado", rs.getString("ESTADO"));
        m.put("descripcion", rs.getString("DESCRIPCION"));
        m.put("disponible", "DISPONIBLE".equals(rs.getString("ESTADO")));
        return m;
    }
}
