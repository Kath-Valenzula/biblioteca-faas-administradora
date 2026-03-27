package com.biblioteca.functions.prestamos;

import java.time.LocalDate;

public class PrestamoActualizarRequest {

    private LocalDate fechaDevolucionEstimada;
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
