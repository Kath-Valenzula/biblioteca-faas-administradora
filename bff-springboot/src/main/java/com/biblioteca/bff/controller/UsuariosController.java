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
@RequestMapping("/api/usuarios")
public class UsuariosController {

    private final DownstreamProxyService proxyService;
    private final RequestValidationService requestValidationService;
    private final String usuariosBaseUrl;

    public UsuariosController(
            DownstreamProxyService proxyService,
            RequestValidationService requestValidationService,
            @Value("${services.usuarios.base-url}") String usuariosBaseUrl
    ) {
        this.proxyService = proxyService;
        this.requestValidationService = requestValidationService;
        this.usuariosBaseUrl = usuariosBaseUrl;
    }

    @GetMapping
    public ResponseEntity<String> listar() {
        return proxyService.get(usuariosBaseUrl, "/usuarios");
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> obtener(@PathVariable Long id) {
        return proxyService.get(usuariosBaseUrl, "/usuarios/" + id);
    }

    @PostMapping
    public ResponseEntity<String> crear(@RequestBody String requestBody) {
        return proxyService.post(usuariosBaseUrl, "/usuarios", requestValidationService.validateUsuario(requestBody));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> actualizar(@PathVariable Long id, @RequestBody String requestBody) {
        return proxyService.put(usuariosBaseUrl, "/usuarios/" + id, requestValidationService.validateUsuario(requestBody));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        return proxyService.delete(usuariosBaseUrl, "/usuarios/" + id);
    }
}
