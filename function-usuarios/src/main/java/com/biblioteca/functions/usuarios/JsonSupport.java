package com.biblioteca.functions.usuarios;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class JsonSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private JsonSupport() {
    }

    public static ObjectMapper mapper() {
        return OBJECT_MAPPER;
    }

    public static HttpResponseMessage response(
            HttpRequestMessage<Optional<String>> request,
            HttpStatus status,
            boolean success,
            String message,
            Object data
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", success);
        payload.put("message", message);
        payload.put("data", data);
        try {
            return request.createResponseBuilder(status)
                    .header("Content-Type", "application/json")
                    .body(OBJECT_MAPPER.writeValueAsString(payload))
                    .build();
        } catch (JsonProcessingException ex) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body("{\"success\":false,\"message\":\"No fue posible serializar la respuesta\",\"data\":null}")
                    .build();
        }
    }
}
