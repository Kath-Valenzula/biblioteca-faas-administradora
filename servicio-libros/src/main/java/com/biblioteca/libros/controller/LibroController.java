package com.biblioteca.libros.controller;

import com.biblioteca.libros.api.ApiResponse;
import com.biblioteca.libros.dto.ActualizarEstadoLibroRequest;
import com.biblioteca.libros.dto.CrearLibroRequest;
import com.biblioteca.libros.dto.LibroResponse;
import com.biblioteca.libros.service.LibroService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
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
public class LibroController {

    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LibroResponse>>> listar() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Listado de libros obtenido correctamente", libroService.listar()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LibroResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Libro obtenido correctamente", libroService.obtener(id)));
    }

    @GetMapping("/{id}/disponibilidad")
    public ResponseEntity<ApiResponse<Map<String, Object>>> disponibilidad(@PathVariable Long id) {
        boolean disponible = libroService.estaDisponible(id);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Disponibilidad consultada correctamente",
                Map.of("libroId", id, "disponible", disponible)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LibroResponse>> crear(@Valid @RequestBody CrearLibroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Libro registrado correctamente", libroService.crear(request)));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<LibroResponse>> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoLibroRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Estado del libro actualizado correctamente",
                libroService.actualizarEstado(id, request)
        ));
    }
}
