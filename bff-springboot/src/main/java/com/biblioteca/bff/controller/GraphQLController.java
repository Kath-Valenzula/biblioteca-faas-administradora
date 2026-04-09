package com.biblioteca.bff.controller;

import com.biblioteca.bff.service.DownstreamProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graphql")
public class GraphQLController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLController.class);

    private final DownstreamProxyService proxyService;
    private final String usuariosBaseUrl;
    private final String librosBaseUrl;

    public GraphQLController(
            DownstreamProxyService proxyService,
            @Value("${services.usuarios.base-url}") String usuariosBaseUrl,
            @Value("${services.libros.base-url}") String librosBaseUrl
    ) {
        this.proxyService = proxyService;
        this.usuariosBaseUrl = usuariosBaseUrl;
        this.librosBaseUrl = librosBaseUrl;
    }

    @PostMapping("/usuarios")
    public ResponseEntity<String> queryUsuarios(@RequestBody String body) {
        LOGGER.info("[BFF-GraphQL] Reenviando consulta GraphQL a function-usuarios");
        return proxyService.post(usuariosBaseUrl, "/graphql", body);
    }

    @PostMapping("/libros")
    public ResponseEntity<String> queryLibros(@RequestBody String body) {
        LOGGER.info("[BFF-GraphQL] Reenviando consulta GraphQL a servicio-libros");
        return proxyService.post(librosBaseUrl, "/graphql", body);
    }
}
