package com.biblioteca.bff.controller;

import com.biblioteca.bff.api.ApiResponse;
import com.biblioteca.bff.dto.NotificacionPrestamoRequest;
import com.biblioteca.bff.service.DownstreamProxyService;
import com.biblioteca.bff.service.RequestValidationService;
import com.biblioteca.bff.service.ServiceBusProducerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(PrestamosController.class);

    private final DownstreamProxyService proxyService;
    private final RequestValidationService requestValidationService;
    private final ServiceBusProducerService serviceBusProducerService;
    private final String prestamosBaseUrl;

    public PrestamosController(
            DownstreamProxyService proxyService,
            RequestValidationService requestValidationService,
            ServiceBusProducerService serviceBusProducerService,
            @Value("${services.prestamos.base-url}") String prestamosBaseUrl
    ) {
        this.proxyService = proxyService;
        this.requestValidationService = requestValidationService;
        this.serviceBusProducerService = serviceBusProducerService;
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

    @PostMapping("/notificar")
    public ResponseEntity<ApiResponse<Object>> notificar(@Valid @RequestBody NotificacionPrestamoRequest request) {
        try {
            LOGGER.info("Recibida solicitud de notificacion para prestamoId={}, usuarioId={}, tipo={}",
                    request.getPrestamoId(), request.getUsuarioId(), request.getTipo());

            serviceBusProducerService.enviarNotificacion(request);

            LOGGER.info("Notificacion enviada exitosamente a la cola para prestamoId={}", request.getPrestamoId());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new ApiResponse<>(true, "Notificacion enviada a la cola correctamente", request));
        } catch (Exception ex) {
            LOGGER.error("Error enviando notificacion para prestamoId={}: {}",
                    request.getPrestamoId(), ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "No fue posible enviar la notificacion: " + ex.getMessage(), null));
        }
    }
}
