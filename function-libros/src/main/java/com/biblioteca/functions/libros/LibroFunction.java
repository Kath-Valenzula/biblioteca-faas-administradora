package com.biblioteca.functions.libros;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LibroFunction {

    @FunctionName("LibrosCrear")
    public HttpResponseMessage crear(
            @HttpTrigger(name = "request", methods = {HttpMethod.POST}, route = "libros",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        context.getLogger().info("[REST] Funcion LibrosCrear invocada - POST /api/libros");
        try {
            LibroRequest payload = JsonSupport.mapper().readValue(
                    request.getBody().orElse("{}"), LibroRequest.class);

            List<String> errores = validarCrear(payload);
            if (!errores.isEmpty()) {
                return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                        "La solicitud contiene errores de validacion", Map.of("errores", errores));
            }

            try (Connection conn = DatabaseConfig.getConnection()) {
                if (existeIsbn(conn, payload.getIsbn().trim())) {
                    return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                            "Ya existe un libro con el ISBN indicado", null);
                }

                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO LIBROS (TITULO, AUTOR, ISBN, ESTADO, DESCRIPCION) VALUES (?, ?, ?, 'DISPONIBLE', ?)",
                        new String[]{"ID"})) {
                    stmt.setString(1, payload.getTitulo().trim());
                    stmt.setString(2, payload.getAutor().trim());
                    stmt.setString(3, payload.getIsbn().trim());
                    stmt.setString(4, payload.getDescripcion() == null || payload.getDescripcion().isBlank()
                            ? null : payload.getDescripcion().trim());
                    stmt.executeUpdate();

                    Long id = obtenerIdGenerado(stmt);
                    Map<String, Object> libro = obtenerLibroPorId(conn, id);
                    return JsonSupport.response(request, HttpStatus.CREATED, true,
                            "Libro creado correctamente", libro);
                }
            }
        } catch (SQLException ex) {
            context.getLogger().severe("Error SQL creando libro: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno registrando el libro", null);
        } catch (Exception ex) {
            context.getLogger().severe("Error creando libro: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                    "No fue posible interpretar la solicitud", null);
        }
    }

    @FunctionName("LibrosListar")
    public HttpResponseMessage listar(
            @HttpTrigger(name = "request", methods = {HttpMethod.GET}, route = "libros",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        context.getLogger().info("[REST] Funcion LibrosListar invocada - GET /api/libros");
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT ID, TITULO, AUTOR, ISBN, ESTADO, DESCRIPCION FROM LIBROS ORDER BY ID");
             ResultSet rs = stmt.executeQuery()) {
            List<Map<String, Object>> libros = new ArrayList<>();
            while (rs.next()) {
                libros.add(mapLibro(rs));
            }
            return JsonSupport.response(request, HttpStatus.OK, true,
                    "Listado de libros obtenido correctamente", libros);
        } catch (SQLException ex) {
            context.getLogger().severe("Error listando libros: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno obteniendo libros", null);
        }
    }

    @FunctionName("LibrosObtener")
    public HttpResponseMessage obtener(
            @HttpTrigger(name = "request", methods = {HttpMethod.GET}, route = "libros/{id}",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context,
            @BindingName("id") final Long id
    ) {
        context.getLogger().info("[REST] Funcion LibrosObtener invocada - GET /api/libros/" + id);
        try (Connection conn = DatabaseConfig.getConnection()) {
            Map<String, Object> libro = obtenerLibroPorId(conn, id);
            if (libro == null) {
                return JsonSupport.response(request, HttpStatus.NOT_FOUND, false,
                        "No existe un libro con el id indicado", null);
            }
            return JsonSupport.response(request, HttpStatus.OK, true,
                    "Libro obtenido correctamente", libro);
        } catch (SQLException ex) {
            context.getLogger().severe("Error obteniendo libro: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno obteniendo el libro", null);
        }
    }

    @FunctionName("LibrosActualizarEstado")
    public HttpResponseMessage actualizarEstado(
            @HttpTrigger(name = "request", methods = {HttpMethod.PUT}, route = "libros/{id}/estado",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context,
            @BindingName("id") final Long id
    ) {
        context.getLogger().info("[REST] Funcion LibrosActualizarEstado invocada - PUT /api/libros/" + id + "/estado");
        try {
            LibroRequest payload = JsonSupport.mapper().readValue(
                    request.getBody().orElse("{}"), LibroRequest.class);

            if (payload.getEstado() == null || payload.getEstado().isBlank()) {
                return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                        "El estado es obligatorio", null);
            }
            String estado = payload.getEstado().trim().toUpperCase();
            if (!estado.equals("DISPONIBLE") && !estado.equals("PRESTADO")) {
                return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                        "El estado debe ser DISPONIBLE o PRESTADO", null);
            }

            try (Connection conn = DatabaseConfig.getConnection()) {
                if (obtenerLibroPorId(conn, id) == null) {
                    return JsonSupport.response(request, HttpStatus.NOT_FOUND, false,
                            "No existe un libro con el id indicado", null);
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE LIBROS SET ESTADO = ? WHERE ID = ?")) {
                    stmt.setString(1, estado);
                    stmt.setLong(2, id);
                    stmt.executeUpdate();
                }
                return JsonSupport.response(request, HttpStatus.OK, true,
                        "Estado del libro actualizado correctamente", obtenerLibroPorId(conn, id));
            }
        } catch (SQLException ex) {
            context.getLogger().severe("Error actualizando estado: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno actualizando el estado del libro", null);
        } catch (Exception ex) {
            context.getLogger().severe("Error interpretando solicitud: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                    "No fue posible interpretar la solicitud", null);
        }
    }

    @FunctionName("LibrosDisponibilidad")
    public HttpResponseMessage disponibilidad(
            @HttpTrigger(name = "request", methods = {HttpMethod.GET}, route = "libros/{id}/disponibilidad",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context,
            @BindingName("id") final Long id
    ) {
        context.getLogger().info("[REST] Funcion LibrosDisponibilidad invocada - GET /api/libros/" + id + "/disponibilidad");
        try (Connection conn = DatabaseConfig.getConnection()) {
            Map<String, Object> libro = obtenerLibroPorId(conn, id);
            if (libro == null) {
                return JsonSupport.response(request, HttpStatus.NOT_FOUND, false,
                        "No existe un libro con el id indicado", null);
            }
            boolean disponible = "DISPONIBLE".equals(libro.get("estado"));
            return JsonSupport.response(request, HttpStatus.OK, true,
                    "Consulta de disponibilidad realizada", Map.of("id", id, "disponible", disponible));
        } catch (SQLException ex) {
            context.getLogger().severe("Error consultando disponibilidad: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno consultando disponibilidad", null);
        }
    }

    private List<String> validarCrear(LibroRequest payload) {
        List<String> errores = new ArrayList<>();
        if (payload.getTitulo() == null || payload.getTitulo().isBlank()) {
            errores.add("El titulo es obligatorio");
        }
        if (payload.getAutor() == null || payload.getAutor().isBlank()) {
            errores.add("El autor es obligatorio");
        }
        if (payload.getIsbn() == null || payload.getIsbn().isBlank()) {
            errores.add("El ISBN es obligatorio");
        }
        return errores;
    }

    private boolean existeIsbn(Connection conn, String isbn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(1) AS TOTAL FROM LIBROS WHERE ISBN = ?")) {
            stmt.setString(1, isbn);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getLong("TOTAL") > 0;
            }
        }
    }

    private Long obtenerIdGenerado(PreparedStatement stmt) throws SQLException {
        try (ResultSet keys = stmt.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("No fue posible recuperar el id generado para libro");
    }

    private Map<String, Object> obtenerLibroPorId(Connection conn, Long id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT ID, TITULO, AUTOR, ISBN, ESTADO, DESCRIPCION FROM LIBROS WHERE ID = ?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapLibro(rs);
            }
        }
    }

    private Map<String, Object> mapLibro(ResultSet rs) throws SQLException {
        Map<String, Object> libro = new LinkedHashMap<>();
        libro.put("id", rs.getLong("ID"));
        libro.put("titulo", rs.getString("TITULO"));
        libro.put("autor", rs.getString("AUTOR"));
        libro.put("isbn", rs.getString("ISBN"));
        libro.put("estado", rs.getString("ESTADO"));
        libro.put("descripcion", rs.getString("DESCRIPCION"));
        libro.put("disponible", "DISPONIBLE".equals(rs.getString("ESTADO")));
        return libro;
    }
}
