package com.biblioteca.libros.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarEstadoLibroRequest(
        @NotBlank(message = "El estado es obligatorio")
        String estado
) {
}
