package com.biblioteca.bff.controller;

import com.biblioteca.bff.service.DownstreamProxyService;
import com.biblioteca.bff.service.RequestValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamosController {

    private final DownstreamProxyService proxyService;
    private final RequestValidationService requestValidationService;
    private final String prestamosBaseUrl;

    public PrestamosController(
            DownstreamProxyService proxyService,
            RequestValidationService requestValidationService,
            @Value("${services.prestamos.base-url}") String prestamosBaseUrl
    ) {
        this.proxyService = proxyService;
        this.requestValidationService = requestValidationService;
        this.prestamosBaseUrl = prestamosBaseUrl;
    }

    @GetMapping
    public ResponseEntity<String> listar() {
        return proxyService.get(prestamosBaseUrl, "/prestamos");
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> obtener(@PathVariable Long id) {
        return proxyService.get(prestamosBaseUrl, "/prestamos/" + id);
    }

    @PostMapping
    public ResponseEntity<String> crear(@RequestBody String requestBody) {
        return proxyService.post(prestamosBaseUrl, "/prestamos", requestValidationService.validatePrestamoCreacion(requestBody));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> actualizar(@PathVariable Long id, @RequestBody String requestBody) {
        return proxyService.put(prestamosBaseUrl, "/prestamos/" + id, requestValidationService.validatePrestamoActualizacion(requestBody));
    }

    @PostMapping("/{id}/devolucion")
    public ResponseEntity<String> devolver(@PathVariable Long id, @RequestBody String requestBody) {
        return proxyService.post(prestamosBaseUrl, "/prestamos/" + id + "/devolucion", requestValidationService.validatePrestamoDevolucion(requestBody));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        return proxyService.delete(prestamosBaseUrl, "/prestamos/" + id);
    }
}
