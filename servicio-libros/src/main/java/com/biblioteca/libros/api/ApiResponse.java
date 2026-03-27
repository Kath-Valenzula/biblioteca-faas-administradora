package com.biblioteca.libros.api;

public record ApiResponse<T>(boolean success, String message, T data) {
}
