package com.biblioteca.functions.notificaciones;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules();

    private JsonSupport() {
    }

    public static ObjectMapper mapper() {
        return OBJECT_MAPPER;
    }
}
