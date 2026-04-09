package com.biblioteca.libros.controller;

import com.biblioteca.libros.dto.LibroResponse;
import com.biblioteca.libros.service.LibroService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class LibroGraphQLController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibroGraphQLController.class);

    private final LibroService libroService;

    public LibroGraphQLController(LibroService libroService) {
        this.libroService = libroService;
    }

    @QueryMapping
    public List<LibroResponse> libros() {
        LOGGER.info("[GraphQL] Consulta de todos los libros recibida");
        return libroService.listar();
    }

    @QueryMapping
    public LibroResponse libro(@Argument Long id) {
        LOGGER.info("[GraphQL] Consulta de libro por id={}", id);
        return libroService.obtener(id);
    }
}
