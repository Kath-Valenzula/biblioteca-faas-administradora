package com.biblioteca.bff.service;

import com.biblioteca.bff.exception.InvalidRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class RequestValidationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final ObjectMapper objectMapper;

    public RequestValidationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String validateUsuario(String rawBody) {
        JsonNode root = parse(rawBody);
        Map<String, String> errors = new LinkedHashMap<>();
        requireText(root, errors, "nombre", "El nombre es obligatorio");
        requireText(root, errors, "correo", "El correo es obligatorio");
        if (!errors.containsKey("correo") && !EMAIL_PATTERN.matcher(root.get("correo").asText().trim()).matches()) {
            errors.put("correo", "El correo no es valido");
        }
        validate(errors);
        return normalize(root);
    }

    public String validatePrestamoCreacion(String rawBody) {
        JsonNode root = parse(rawBody);
        Map<String, String> errors = new LinkedHashMap<>();
        requireNumber(root, errors, "usuarioId", "El id del usuario es obligatorio");
        requireNumber(root, errors, "libroId", "El id del libro es obligatorio");
        requireText(root, errors, "fechaPrestamo", "La fecha del prestamo es obligatoria");
        requireText(root, errors, "fechaDevolucionEstimada", "La fecha estimada de devolucion es obligatoria");
        validate(errors);
        return normalize(root);
    }

    public String validatePrestamoActualizacion(String rawBody) {
        JsonNode root = parse(rawBody);
        Map<String, String> errors = new LinkedHashMap<>();
        requireText(root, errors, "fechaDevolucionEstimada", "La fecha estimada de devolucion es obligatoria");
        validate(errors);
        return normalize(root);
    }

    public String validatePrestamoDevolucion(String rawBody) {
        JsonNode root = parse(rawBody);
        Map<String, String> errors = new LinkedHashMap<>();
        requireText(root, errors, "fechaDevolucionReal", "La fecha de devolucion es obligatoria");
        validate(errors);
        return normalize(root);
    }

    public String validateLibroCreacion(String rawBody) {
        JsonNode root = parse(rawBody);
        Map<String, String> errors = new LinkedHashMap<>();
        requireText(root, errors, "titulo", "El titulo es obligatorio");
        requireText(root, errors, "autor", "El autor es obligatorio");
        requireText(root, errors, "isbn", "El ISBN es obligatorio");
        validate(errors);
        return normalize(root);
    }

    public String validateLibroEstado(String rawBody) {
        JsonNode root = parse(rawBody);
        Map<String, String> errors = new LinkedHashMap<>();
        requireText(root, errors, "estado", "El estado es obligatorio");
        validate(errors);
        return normalize(root);
    }

    private JsonNode parse(String rawBody) {
        try {
            String body = rawBody == null ? "{}" : rawBody.strip();
            if (!body.isEmpty() && body.charAt(0) == '\uFEFF') {
                body = body.substring(1);
            }
            return objectMapper.readTree(body);
        } catch (JsonProcessingException ex) {
            throw new InvalidRequestException("La solicitud contiene errores de validacion",
                    Map.of("body", "El cuerpo JSON no es valido"));
        }
    }

    private String normalize(JsonNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new InvalidRequestException("La solicitud contiene errores de validacion",
                    Map.of("body", "No fue posible procesar el cuerpo JSON"));
        }
    }

    private void requireText(JsonNode root, Map<String, String> errors, String field, String message) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || node.asText().trim().isEmpty()) {
            errors.put(field, message);
        }
    }

    private void requireNumber(JsonNode root, Map<String, String> errors, String field, String message) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isNumber()) {
            errors.put(field, message);
        }
    }

    private void validate(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new InvalidRequestException("La solicitud contiene errores de validacion", errors);
        }
    }
}