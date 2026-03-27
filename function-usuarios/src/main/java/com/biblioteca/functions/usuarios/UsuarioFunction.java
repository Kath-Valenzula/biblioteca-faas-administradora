package com.biblioteca.functions.usuarios;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class UsuarioFunction {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    @FunctionName("UsuariosCrear")
    public HttpResponseMessage crear(
            @HttpTrigger(name = "request", methods = {HttpMethod.POST}, route = "usuarios", authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        try {
            UsuarioRequest payload = JsonSupport.mapper().readValue(request.getBody().orElse("{}"), UsuarioRequest.class);
            List<String> errores = validarPayload(payload, true);
            if (!errores.isEmpty()) {
                return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                        "La solicitud contiene errores de validacion", Map.of("errores", errores));
            }

            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO USUARIOS (NOMBRE, CORREO, TELEFONO, ESTADO, FECHA_REGISTRO) VALUES (?, ?, ?, ?, ?)")) {
                statement.setString(1, payload.getNombre().trim());
                statement.setString(2, payload.getCorreo().trim().toLowerCase());
                statement.setString(3, valorOpcional(payload.getTelefono()));
                statement.setString(4, normalizarEstado(payload.getEstado()));
                statement.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                statement.executeUpdate();

                Long id = obtenerUltimoId(connection, "USUARIOS");
                Map<String, Object> usuario = obtenerUsuarioPorId(connection, id);
                return JsonSupport.response(request, HttpStatus.CREATED, true,
                        "Usuario creado correctamente", usuario);
            }
        } catch (SQLException ex) {
            context.getLogger().severe("Error SQL creando usuario: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno registrando el usuario", null);
        } catch (Exception ex) {
            context.getLogger().severe("Error creando usuario: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                    "No fue posible interpretar la solicitud", null);
        }
    }

    @FunctionName("UsuariosListar")
    public HttpResponseMessage listar(
            @HttpTrigger(name = "request", methods = {HttpMethod.GET}, route = "usuarios", authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context
    ) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT ID, NOMBRE, CORREO, TELEFONO, ESTADO, FECHA_REGISTRO FROM USUARIOS ORDER BY ID"
             );
             ResultSet resultSet = statement.executeQuery()) {
            List<Map<String, Object>> usuarios = new ArrayList<>();
            while (resultSet.next()) {
                usuarios.add(mapUsuario(resultSet));
            }
            return JsonSupport.response(request, HttpStatus.OK, true,
                    "Listado de usuarios obtenido correctamente", usuarios);
        } catch (SQLException ex) {
            context.getLogger().severe("Error listando usuarios: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno obteniendo usuarios", null);
        }
    }

    @FunctionName("UsuariosObtener")
    public HttpResponseMessage obtener(
            @HttpTrigger(name = "request", methods = {HttpMethod.GET}, route = "usuarios/{id}", authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context,
            @BindingName("id") final Long id
    ) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            Map<String, Object> usuario = obtenerUsuarioPorId(connection, id);
            if (usuario == null) {
                return JsonSupport.response(request, HttpStatus.NOT_FOUND, false,
                        "No existe un usuario con el id indicado", null);
            }
            return JsonSupport.response(request, HttpStatus.OK, true,
                    "Usuario obtenido correctamente", usuario);
        } catch (SQLException ex) {
            context.getLogger().severe("Error obteniendo usuario: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno obteniendo el usuario", null);
        }
    }

    @FunctionName("UsuariosActualizar")
    public HttpResponseMessage actualizar(
            @HttpTrigger(name = "request", methods = {HttpMethod.PUT}, route = "usuarios/{id}", authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context,
            @BindingName("id") final Long id
    ) {
        try {
            UsuarioRequest payload = JsonSupport.mapper().readValue(request.getBody().orElse("{}"), UsuarioRequest.class);
            List<String> errores = validarPayload(payload, false);
            if (!errores.isEmpty()) {
                return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                        "La solicitud contiene errores de validacion", Map.of("errores", errores));
            }

            try (Connection connection = DatabaseConfig.getConnection()) {
                if (obtenerUsuarioPorId(connection, id) == null) {
                    return JsonSupport.response(request, HttpStatus.NOT_FOUND, false,
                            "No existe un usuario con el id indicado", null);
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE USUARIOS SET NOMBRE = ?, CORREO = ?, TELEFONO = ?, ESTADO = ? WHERE ID = ?")) {
                    statement.setString(1, payload.getNombre().trim());
                    statement.setString(2, payload.getCorreo().trim().toLowerCase());
                    statement.setString(3, valorOpcional(payload.getTelefono()));
                    statement.setString(4, normalizarEstado(payload.getEstado()));
                    statement.setLong(5, id);
                    statement.executeUpdate();
                }

                return JsonSupport.response(request, HttpStatus.OK, true,
                        "Usuario actualizado correctamente", obtenerUsuarioPorId(connection, id));
            }
        } catch (SQLException ex) {
            context.getLogger().severe("Error actualizando usuario: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno actualizando el usuario", null);
        } catch (Exception ex) {
            context.getLogger().severe("Error interpretando usuario: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                    "No fue posible interpretar la solicitud", null);
        }
    }

    @FunctionName("UsuariosEliminar")
    public HttpResponseMessage eliminar(
            @HttpTrigger(name = "request", methods = {HttpMethod.DELETE}, route = "usuarios/{id}", authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context,
            @BindingName("id") final Long id
    ) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            if (obtenerUsuarioPorId(connection, id) == null) {
                return JsonSupport.response(request, HttpStatus.NOT_FOUND, false,
                        "No existe un usuario con el id indicado", null);
            }
            if (tienePrestamos(connection, id)) {
                return JsonSupport.response(request, HttpStatus.BAD_REQUEST, false,
                        "No se puede eliminar el usuario porque tiene prestamos registrados", null);
            }

            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM USUARIOS WHERE ID = ?")) {
                statement.setLong(1, id);
                statement.executeUpdate();
            }

            return JsonSupport.response(request, HttpStatus.OK, true,
                    "Usuario eliminado correctamente", Map.of("id", id));
        } catch (SQLException ex) {
            context.getLogger().severe("Error eliminando usuario: " + ex.getMessage());
            return JsonSupport.response(request, HttpStatus.INTERNAL_SERVER_ERROR, false,
                    "Error interno eliminando el usuario", null);
        }
    }

    private List<String> validarPayload(UsuarioRequest payload, boolean crear) {
        List<String> errores = new ArrayList<>();
        if (payload.getNombre() == null || payload.getNombre().isBlank()) {
            errores.add("El nombre es obligatorio");
        }
        if (payload.getCorreo() == null || payload.getCorreo().isBlank()) {
            errores.add("El correo es obligatorio");
        } else if (!EMAIL_PATTERN.matcher(payload.getCorreo().trim()).matches()) {
            errores.add("El correo no es valido");
        }
        String estado = payload.getEstado();
        if (estado != null && !estado.isBlank()) {
            String normalizado = estado.trim().toUpperCase();
            if (!normalizado.equals("ACTIVO") && !normalizado.equals("INACTIVO")) {
                errores.add("El estado del usuario debe ser ACTIVO o INACTIVO");
            }
        }
        return errores;
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "ACTIVO";
        }
        return estado.trim().toUpperCase();
    }

    private String valorOpcional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long obtenerUltimoId(Connection connection, String tableName) throws SQLException {
        try (Statement fallback = connection.createStatement();
             ResultSet generatedKeys = fallback.executeQuery("SELECT MAX(ID) AS ID FROM " + tableName)) {
            if (generatedKeys.next()) {
                return generatedKeys.getLong("ID");
            }
        }
        throw new SQLException("No fue posible recuperar el id generado");
    }

    private boolean tienePrestamos(Connection connection, Long usuarioId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(1) AS TOTAL FROM PRESTAMOS WHERE USUARIO_ID = ?")) {
            statement.setLong(1, usuarioId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong("TOTAL") > 0;
            }
        }
    }

    private Map<String, Object> obtenerUsuarioPorId(Connection connection, Long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT ID, NOMBRE, CORREO, TELEFONO, ESTADO, FECHA_REGISTRO FROM USUARIOS WHERE ID = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapUsuario(resultSet);
            }
        }
    }

    private Map<String, Object> mapUsuario(ResultSet resultSet) throws SQLException {
        Map<String, Object> usuario = new LinkedHashMap<>();
        usuario.put("id", resultSet.getLong("ID"));
        usuario.put("nombre", resultSet.getString("NOMBRE"));
        usuario.put("correo", resultSet.getString("CORREO"));
        usuario.put("telefono", resultSet.getString("TELEFONO"));
        usuario.put("estado", resultSet.getString("ESTADO"));
        Date fechaRegistro = resultSet.getDate("FECHA_REGISTRO");
        usuario.put("fechaRegistro", fechaRegistro == null ? null : fechaRegistro.toLocalDate().toString());
        return usuario;
    }
}
