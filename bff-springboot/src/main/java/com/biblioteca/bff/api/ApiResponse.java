package com.biblioteca.bff.api;

public record ApiResponse<T>(boolean success, String message, T data) {
}
