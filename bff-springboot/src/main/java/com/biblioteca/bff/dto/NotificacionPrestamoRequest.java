package com.biblioteca.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NotificacionPrestamoRequest {

    @NotNull(message = "El id del prestamo es obligatorio")
    private Long prestamoId;

    @NotNull(message = "El id del usuario es obligatorio")
    private Long usuarioId;

    @NotBlank(message = "El titulo del libro es obligatorio")
    private String libroTitulo;

    @NotBlank(message = "El tipo de notificacion es obligatorio")
    private String tipo;

    public NotificacionPrestamoRequest() {
    }

    public Long getPrestamoId() {
        return prestamoId;
    }

    public void setPrestamoId(Long prestamoId) {
        this.prestamoId = prestamoId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getLibroTitulo() {
        return libroTitulo;
    }

    public void setLibroTitulo(String libroTitulo) {
        this.libroTitulo = libroTitulo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
