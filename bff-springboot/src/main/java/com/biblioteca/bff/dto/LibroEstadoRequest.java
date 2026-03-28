package com.biblioteca.bff.dto;

import jakarta.validation.constraints.NotBlank;

public class LibroEstadoRequest {

        @NotBlank(message = "El estado es obligatorio")
        private String estado;

        public String getEstado() {
                return estado;
        }

        public void setEstado(String estado) {
                this.estado = estado;
        }
}
