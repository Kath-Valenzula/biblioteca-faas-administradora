package com.biblioteca.libros.dto;

public record LibroResponse(
        Long id,
        String titulo,
        String autor,
        String isbn,
        String estado,
        String descripcion,
        boolean disponible
) {
}
