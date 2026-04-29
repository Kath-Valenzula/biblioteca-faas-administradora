package com.biblioteca.functions.notificaciones;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

public class PrestamoEventGridConsumerFunction {

    @FunctionName("PrestamoEventGridConsumer")
    public void consumirEventoPrestamo(
            @EventGridTrigger(name = "event")
            String event,
            final ExecutionContext context
    ) {
        try {
            context.getLogger().info("=== EVENTO EVENT GRID RECIBIDO ===");
            context.getLogger().info("Evento recibido: " + event);

            JsonNode root = JsonSupport.mapper().readTree(event);
            JsonNode data = root.has("data") && root.get("data").isObject() ? root.get("data") : root;

            String eventType = textOrDefault(root, "eventType", textOrDefault(data, "tipoEvento", "N/A"));
            String prestamoId = textOrDefault(data, "prestamoId", "N/A");
            String usuarioId = textOrDefault(data, "usuarioId", "N/A");
            String libroId = textOrDefault(data, "libroId", "N/A");
            String estado = textOrDefault(data, "estado", "N/A");
            String correlationId = textOrDefault(data, "correlationId", "N/A");

            context.getLogger().info(String.format(
                    "Procesando evento de prestamo: tipoEvento=%s, prestamoId=%s, usuarioId=%s, libroId=%s, estado=%s, correlationId=%s",
                    eventType, prestamoId, usuarioId, libroId, estado, correlationId));

            // Simulacion de notificacion: en produccion aqui iria SendGrid / Logic Apps / etc.
            String accion = eventType.contains("Creado") ? "PRESTAMO REGISTRADO" : "DEVOLUCION REGISTRADA";
            context.getLogger().info(String.format(
                    "[NOTIFICACION SIMULADA] Para: usuario-%s | Asunto: %s | " +
                    "Detalle: prestamoId=%s, libroId=%s, estado=%s | correlationId=%s",
                    usuarioId, accion, prestamoId, libroId, estado, correlationId));
            context.getLogger().info(String.format(
                    "Notificacion generada via Event Grid: canal de alertas activado para usuarioId=%s, prestamoId=%s [tipo=%s]",
                    usuarioId, prestamoId, eventType));

            context.getLogger().info("=== EVENTO EVENT GRID PROCESADO EXITOSAMENTE ===");
        } catch (Exception ex) {
            context.getLogger().severe("Error procesando evento Event Grid: " + ex.getMessage());
            throw new RuntimeException("Error procesando evento Event Grid", ex);
        }
    }

    private String textOrDefault(JsonNode node, String fieldName, String defaultValue) {
        if (node != null && node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return defaultValue;
    }
}
