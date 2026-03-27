package com.biblioteca.bff.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class PrestamoCrearRequest {

        @NotNull(message = "El id del usuario es obligatorio")
        private Long usuarioId;

        @NotNull(message = "El id del libro es obligatorio")
        private Long libroId;

        @NotNull(message = "La fecha del prestamo es obligatoria")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate fechaPrestamo;

        @NotNull(message = "La fecha estimada de devolucion es obligatoria")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate fechaDevolucionEstimada;

        @Size(max = 500, message = "La observacion no puede superar 500 caracteres")
        private String observacion;

        public Long getUsuarioId() {
                return usuarioId;
        }

        public void setUsuarioId(Long usuarioId) {
                this.usuarioId = usuarioId;
        }

        public Long getLibroId() {
                return libroId;
        }

        public void setLibroId(Long libroId) {
                this.libroId = libroId;
        }

        public LocalDate getFechaPrestamo() {
                return fechaPrestamo;
        }

        public void setFechaPrestamo(LocalDate fechaPrestamo) {
                this.fechaPrestamo = fechaPrestamo;
        }

        public LocalDate getFechaDevolucionEstimada() {
                return fechaDevolucionEstimada;
        }

        public void setFechaDevolucionEstimada(LocalDate fechaDevolucionEstimada) {
                this.fechaDevolucionEstimada = fechaDevolucionEstimada;
        }

        public String getObservacion() {
                return observacion;
        }

        public void setObservacion(String observacion) {
                this.observacion = observacion;
        }
}
