package com.biblioteca.bff.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class PrestamoDevolucionRequest {

        @NotNull(message = "La fecha de devolucion es obligatoria")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate fechaDevolucionReal;

        @Size(max = 500, message = "La observacion no puede superar 500 caracteres")
        private String observacion;

        public LocalDate getFechaDevolucionReal() {
                return fechaDevolucionReal;
        }

        public void setFechaDevolucionReal(LocalDate fechaDevolucionReal) {
                this.fechaDevolucionReal = fechaDevolucionReal;
        }

        public String getObservacion() {
                return observacion;
        }

        public void setObservacion(String observacion) {
                this.observacion = observacion;
        }
}
