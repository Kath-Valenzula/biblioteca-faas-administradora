package com.biblioteca.bff.controller;

import com.biblioteca.bff.service.DownstreamProxyService;
import com.biblioteca.bff.service.RequestValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/libros")
public class LibrosController {

    private final DownstreamProxyService proxyService;
    private final RequestValidationService requestValidationService;
    private final String librosBaseUrl;

    public LibrosController(
            DownstreamProxyService proxyService,
            RequestValidationService requestValidationService,
            @Value("${services.libros.base-url}") String librosBaseUrl
    ) {
        this.proxyService = proxyService;
        this.requestValidationService = requestValidationService;
        this.librosBaseUrl = librosBaseUrl;
    }

    @GetMapping
    public ResponseEntity<String> listar() {
        return proxyService.get(librosBaseUrl, "/libros");
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> obtener(@PathVariable Long id) {
        return proxyService.get(librosBaseUrl, "/libros/" + id);
    }

    @GetMapping("/{id}/disponibilidad")
    public ResponseEntity<String> disponibilidad(@PathVariable Long id) {
        return proxyService.get(librosBaseUrl, "/libros/" + id + "/disponibilidad");
    }

    @PostMapping
    public ResponseEntity<String> crear(@RequestBody String requestBody) {
        return proxyService.post(librosBaseUrl, "/libros", requestValidationService.validateLibroCreacion(requestBody));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<String> actualizarEstado(@PathVariable Long id, @RequestBody String requestBody) {
        return proxyService.put(librosBaseUrl, "/libros/" + id + "/estado", requestValidationService.validateLibroEstado(requestBody));
    }
}
