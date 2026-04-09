package com.biblioteca.bff.controller;

import com.biblioteca.bff.service.DownstreamProxyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graphql")
public class GraphQLProxyController {

    private final DownstreamProxyService proxyService;
    private final String usuariosBaseUrl;
    private final String prestamosBaseUrl;
    private final String librosBaseUrl;

    public GraphQLProxyController(
            DownstreamProxyService proxyService,
            @Value("${services.usuarios.base-url}") String usuariosBaseUrl,
            @Value("${services.prestamos.base-url}") String prestamosBaseUrl,
            @Value("${services.libros.base-url}") String librosBaseUrl
    ) {
        this.proxyService = proxyService;
        this.usuariosBaseUrl = usuariosBaseUrl;
        this.prestamosBaseUrl = prestamosBaseUrl;
        this.librosBaseUrl = librosBaseUrl;
    }

    @PostMapping("/usuarios")
    public ResponseEntity<String> graphqlUsuarios(@RequestBody String body) {
        return proxyService.post(usuariosBaseUrl, "/graphql", body);
    }

    @PostMapping("/prestamos")
    public ResponseEntity<String> graphqlPrestamos(@RequestBody String body) {
        return proxyService.post(prestamosBaseUrl, "/graphql", body);
    }

    @PostMapping("/libros")
    public ResponseEntity<String> graphqlLibros(@RequestBody String body) {
        return proxyService.post(librosBaseUrl, "/graphql", body);
    }
}
