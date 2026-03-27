package com.biblioteca.functions.prestamos;

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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PrestamoFunction {

    @FunctionName("PrestamosCrear")
    public HttpResponseMessage crear(
            @HttpTrigger(name = "request", methods = {HttpMethod.POST}, route = "prestamos", authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        try {
            PrestamoCrearRequest payload = JsonSupport.mapper().readValue(request.getBody().orElse("{}"), PrestamoCrearRequest.class);
            List<String> errores = validarCreacion(payload);
            if (!errores.isEmpty()) {
                return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                        "La solicitud contiene errores de validacion", Map.of("errores", errores));
            }

            try (Connection connection = DatabaseConfig.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    if (!usuarioExiste(connection, payload.getUsuarioId())) {
                        connection.rollback();
                        return JsonSupport.response(request, HttpStatus.NOT_FOUND, false,
                                "No se puede registrar un prestamo para un usuario inexistente", null);
                    }

                    Map<String, Object> libro = obtenerLibro(connection, payload.getLibroId());
                    if (libro == null) {
                        connection.rollback();
                        return JsonSupport.response(request, HttpStatus.NOT_FOUND, false,
                                "No se puede registrar un prestamo para un libro inexistente", null);
                    }
                    if (!"DISPONIBLE".equals(libro.get("estado"))) {
                        connection.rollback();
                        return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                                "No se puede prestar un libro que ya no esta disponible", null);
                    }

                    Long prestamoId;
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO PRESTAMOS (USUARIO_ID, LIBRO_ID, FECHA_PRESTAMO, FECHA_DEVOLUCION_ESTIMADA, ESTADO, OBSERVACION, FECHA_REGISTRO) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                        statement.setLong(1, payload.getUsuarioId());
                        statement.setLong(2, payload.getLibroId());
                        statement.setDate(3, Date.valueOf(payload.getFechaPrestamo()));
                        statement.setDate(4, Date.valueOf(payload.getFechaDevolucionEstimada()));
                        statement.setString(5, "ACTIVO");
                        statement.setString(6, valorOpcional(payload.getObservacion()));
                        statement.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
                        statement.executeUpdate();
                        prestamoId = obtenerUltimoId(connection, "PRESTAMOS");
                    }

                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE LIBROS SET ESTADO = 'PRESTADO' WHERE ID = ?")) {
                        statement.setLong(1, payload.getLibroId());
                        statement.executeUpdate();
                    }

                    connection.commit();
                    return JsonSupport.response(request, HttpStatus.CREATED, true,
                            "Prestamo registrado correctamente", obtenerPrestamo(connection, prestamoId));
                } catch (Exception ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException ex) {
            context.getLogger().severe("Error SQL creando prestamo: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno registrando el prestamo", null);
        } catch (Exception ex) {
            context.getLogger().severe("Error creando prestamo: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                    "No fue posible interpretar la solicitud", null);
        }
    }

    @FunctionName("PrestamosListar")
    public HttpResponseMessage listar(
            @HttpTrigger(name = "request", methods = {HttpMethod.GET}, route = "prestamos", authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT P.ID, P.USUARIO_ID, U.NOMBRE AS USUARIO_NOMBRE, P.LIBRO_ID, L.TITULO AS LIBRO_TITULO, P.FECHA_PRESTAMO, P.FECHA_DEVOLUCION_ESTIMADA, P.FECHA_DEVOLUCION_REAL, P.ESTADO, P.OBSERVACION FROM PRESTAMOS P JOIN USUARIOS U ON U.ID = P.USUARIO_ID JOIN LIBROS L ON L.ID = P.LIBRO_ID ORDER BY P.ID"
             );
             ResultSet resultSet = statement.executeQuery()) {
            List<Map<String, Object>> prestamos = new ArrayList<>();
            while (resultSet.next()) {
                prestamos.add(mapPrestamo(resultSet));
            }
            return JsonSupport.response(request, HttpStatus.OK, true,
                    "Listado de prestamos obtenido correctamente", prestamos);
        } catch (SQLException ex) {
            context.getLogger().severe("Error listando prestamos: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno obteniendo prestamos", null);
        }
    }

    @FunctionName("PrestamosObtener")
    public HttpResponseMessage obtener(
            @HttpTrigger(name = "request", methods = {HttpMethod.GET}, route = "prestamos/{id}", authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context,
            @BindingName("id") final Long id
    ) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            Map<String, Object> prestamo = obtenerPrestamo(connection, id);
            if (prestamo == null) {
                return JsonSupport.response(request, HttpStatus.NOT_FOUND, false,
                        "No existe un prestamo con el id indicado", null);
            }
            return JsonSupport.response(request, HttpStatus.OK, true,
                    "Prestamo obtenido correctamente", prestamo);
        } catch (SQLException ex) {
            context.getLogger().severe("Error obteniendo prestamo: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno obteniendo el prestamo", null);
        }
    }

    @FunctionName("PrestamosActualizar")
    public HttpResponseMessage actualizar(
            @HttpTrigger(name = "request", methods = {HttpMethod.PUT}, route = "prestamos/{id}", authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context,
            @BindingName("id") final Long id
    ) {
        try {
            PrestamoActualizarRequest payload = JsonSupport.mapper().readValue(request.getBody().orElse("{}"), PrestamoActualizarRequest.class);
            List<String> errores = validarActualizacion(payload);
            if (!errores.isEmpty()) {
                return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                        "La solicitud contiene errores de validacion", Map.of("errores", errores));
            }

            try (Connection connection = DatabaseConfig.getConnection()) {
                Map<String, Object> prestamo = obtenerPrestamo(connection, id);
                if (prestamo == null) {
                    return JsonSupport.response(request, HttpStatus.NOT_FOUND, false,
                            "No existe un prestamo con el id indicado", null);
                }
                if (!"ACTIVO".equals(prestamo.get("estado"))) {
                    return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                            "Solo se pueden actualizar prestamos en estado ACTIVO", null);
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE PRESTAMOS SET FECHA_DEVOLUCION_ESTIMADA = ?, OBSERVACION = ? WHERE ID = ?")) {
                    statement.setDate(1, Date.valueOf(payload.getFechaDevolucionEstimada()));
                    statement.setString(2, valorOpcional(payload.getObservacion()));
                    statement.setLong(3, id);
                    statement.executeUpdate();
                }

                return JsonSupport.response(request, HttpStatus.OK, true,
                        "Prestamo actualizado correctamente", obtenerPrestamo(connection, id));
            }
        } catch (SQLException ex) {
            context.getLogger().severe("Error actualizando prestamo: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno actualizando el prestamo", null);
        } catch (Exception ex) {
            context.getLogger().severe("Error actualizando prestamo: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                    "No fue posible interpretar la solicitud", null);
        }
    }

    @FunctionName("PrestamosDevolver")
    public HttpResponseMessage devolver(
            @HttpTrigger(name = "request", methods = {HttpMethod.POST}, route = "prestamos/{id}/devolucion", authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context,
            @BindingName("id") final Long id
    ) {
        try {
            PrestamoDevolucionRequest payload = JsonSupport.mapper().readValue(request.getBody().orElse("{}"), PrestamoDevolucionRequest.class);
            List<String> errores = validarDevolucion(payload);
            if (!errores.isEmpty()) {
                return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                        "La solicitud contiene errores de validacion", Map.of("errores", errores));
            }

            try (Connection connection = DatabaseConfig.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    Map<String, Object> prestamo = obtenerPrestamo(connection, id);
                    if (prestamo == null) {
                        connection.rollback();
                        return JsonSupport.response(request, HttpStatus.NOT_FOUND, false,
                                "No existe un prestamo con el id indicado", null);
                    }
                    if (!"ACTIVO".equals(prestamo.get("estado"))) {
                        connection.rollback();
                        return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                                "El prestamo ya fue devuelto o no se encuentra activo", null);
                    }

                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE PRESTAMOS SET FECHA_DEVOLUCION_REAL = ?, ESTADO = 'DEVUELTO', OBSERVACION = ? WHERE ID = ?")) {
                        statement.setDate(1, Date.valueOf(payload.getFechaDevolucionReal()));
                        statement.setString(2, valorOpcional(payload.getObservacion()));
                        statement.setLong(3, id);
                        statement.executeUpdate();
                    }

                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE LIBROS SET ESTADO = 'DISPONIBLE' WHERE ID = ?")) {
                        statement.setLong(1, ((Number) prestamo.get("libroId")).longValue());
                        statement.executeUpdate();
                    }

                    connection.commit();
                    return JsonSupport.response(request, HttpStatus.OK, true,
                            "Devolucion registrada correctamente", obtenerPrestamo(connection, id));
                } catch (Exception ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException ex) {
            context.getLogger().severe("Error registrando devolucion: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno registrando la devolucion", null);
        } catch (Exception ex) {
            context.getLogger().severe("Error registrando devolucion: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                    "No fue posible interpretar la solicitud", null);
        }
    }

    @FunctionName("PrestamosEliminar")
    public HttpResponseMessage eliminar(
            @HttpTrigger(name = "request", methods = {HttpMethod.DELETE}, route = "prestamos/{id}", authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context,
            @BindingName("id") final Long id
    ) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            Map<String, Object> prestamo = obtenerPrestamo(connection, id);
            if (prestamo == null) {
                return JsonSupport.response(request, HttpStatus.NOT_FOUND, false,
                        "No existe un prestamo con el id indicado", null);
            }
            if (!"DEVUELTO".equals(prestamo.get("estado"))) {
                return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                        "Solo se pueden eliminar prestamos ya devueltos", null);
            }

            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM PRESTAMOS WHERE ID = ?")) {
                statement.setLong(1, id);
                statement.executeUpdate();
            }

            return JsonSupport.response(request, HttpStatus.OK, true,
                    "Prestamo eliminado correctamente", Map.of("id", id));
        } catch (SQLException ex) {
            context.getLogger().severe("Error eliminando prestamo: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno eliminando el prestamo", null);
        }
    }

    private List<String> validarCreacion(PrestamoCrearRequest payload) {
        List<String> errores = new ArrayList<>();
        if (payload.getUsuarioId() == null) {
            errores.add("El id del usuario es obligatorio");
        }
        if (payload.getLibroId() == null) {
            errores.add("El id del libro es obligatorio");
        }
        if (payload.getFechaPrestamo() == null) {
            errores.add("La fecha del prestamo es obligatoria");
        }
        if (payload.getFechaDevolucionEstimada() == null) {
            errores.add("La fecha estimada de devolucion es obligatoria");
        }
        if (payload.getFechaPrestamo() != null && payload.getFechaDevolucionEstimada() != null
                && payload.getFechaDevolucionEstimada().isBefore(payload.getFechaPrestamo())) {
            errores.add("La fecha estimada de devolucion no puede ser anterior a la fecha del prestamo");
        }
        return errores;
    }

    private List<String> validarActualizacion(PrestamoActualizarRequest payload) {
        List<String> errores = new ArrayList<>();
        if (payload.getFechaDevolucionEstimada() == null) {
            errores.add("La fecha estimada de devolucion es obligatoria");
        }
        return errores;
    }

    private List<String> validarDevolucion(PrestamoDevolucionRequest payload) {
        List<String> errores = new ArrayList<>();
        if (payload.getFechaDevolucionReal() == null) {
            errores.add("La fecha de devolucion es obligatoria");
        }
        return errores;
    }

    private boolean usuarioExiste(Connection connection, Long usuarioId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(1) AS TOTAL FROM USUARIOS WHERE ID = ?")) {
            statement.setLong(1, usuarioId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong("TOTAL") > 0;
            }
        }
    }

    private Map<String, Object> obtenerLibro(Connection connection, Long libroId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT ID, TITULO, ESTADO FROM LIBROS WHERE ID = ?")) {
            statement.setLong(1, libroId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                Map<String, Object> libro = new LinkedHashMap<>();
                libro.put("id", resultSet.getLong("ID"));
                libro.put("titulo", resultSet.getString("TITULO"));
                libro.put("estado", resultSet.getString("ESTADO"));
                return libro;
            }
        }
    }

    private Long obtenerUltimoId(Connection connection, String tableName) throws SQLException {
        try (Statement fallback = connection.createStatement();
             ResultSet resultSet = fallback.executeQuery("SELECT MAX(ID) AS ID FROM " + tableName)) {
            if (resultSet.next()) {
                return resultSet.getLong("ID");
            }
        }
        throw new SQLException("No fue posible recuperar el id generado");
    }

    private Map<String, Object> obtenerPrestamo(Connection connection, Long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT P.ID, P.USUARIO_ID, U.NOMBRE AS USUARIO_NOMBRE, P.LIBRO_ID, L.TITULO AS LIBRO_TITULO, P.FECHA_PRESTAMO, P.FECHA_DEVOLUCION_ESTIMADA, P.FECHA_DEVOLUCION_REAL, P.ESTADO, P.OBSERVACION FROM PRESTAMOS P JOIN USUARIOS U ON U.ID = P.USUARIO_ID JOIN LIBROS L ON L.ID = P.LIBRO_ID WHERE P.ID = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapPrestamo(resultSet);
            }
        }
    }

    private Map<String, Object> mapPrestamo(ResultSet resultSet) throws SQLException {
        Map<String, Object> prestamo = new LinkedHashMap<>();
        prestamo.put("id", resultSet.getLong("ID"));
        prestamo.put("usuarioId", resultSet.getLong("USUARIO_ID"));
        prestamo.put("usuarioNombre", resultSet.getString("USUARIO_NOMBRE"));
        prestamo.put("libroId", resultSet.getLong("LIBRO_ID"));
        prestamo.put("libroTitulo", resultSet.getString("LIBRO_TITULO"));
        prestamo.put("fechaPrestamo", toDateString(resultSet.getDate("FECHA_PRESTAMO")));
        prestamo.put("fechaDevolucionEstimada", toDateString(resultSet.getDate("FECHA_DEVOLUCION_ESTIMADA")));
        prestamo.put("fechaDevolucionReal", toDateString(resultSet.getDate("FECHA_DEVOLUCION_REAL")));
        prestamo.put("estado", resultSet.getString("ESTADO"));
        prestamo.put("observacion", resultSet.getString("OBSERVACION"));
        return prestamo;
    }

    private String toDateString(Date date) {
        LocalDate localDate = date == null ? null : date.toLocalDate();
        return localDate == null ? null : localDate.toString();
    }

    private String valorOpcional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
