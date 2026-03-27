package com.biblioteca.libros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearLibroRequest(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 150, message = "El titulo no puede superar 150 caracteres")
        String titulo,
        @NotBlank(message = "El autor es obligatorio")
        @Size(max = 120, message = "El autor no puede superar 120 caracteres")
        String autor,
        @NotBlank(message = "El ISBN es obligatorio")
        @Size(max = 20, message = "El ISBN no puede superar 20 caracteres")
        String isbn,
        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        String descripcion
) {
}
