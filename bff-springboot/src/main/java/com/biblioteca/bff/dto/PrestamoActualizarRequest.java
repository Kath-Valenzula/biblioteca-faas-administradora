package com.biblioteca.bff.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class PrestamoActualizarRequest {

        @NotNull(message = "La fecha estimada de devolucion es obligatoria")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate fechaDevolucionEstimada;

        @Size(max = 500, message = "La observacion no puede superar 500 caracteres")
        private String observacion;

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
