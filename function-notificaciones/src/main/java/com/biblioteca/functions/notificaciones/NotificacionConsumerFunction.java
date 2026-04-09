package com.biblioteca.functions.notificaciones;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

public class NotificacionConsumerFunction {

    @FunctionName("NotificacionConsumer")
    public void consumirNotificacion(
            @ServiceBusQueueTrigger(
                    name = "message",
                    queueName = "%SERVICEBUS_QUEUE_NAME%",
                    connection = "ServiceBusConnection"
            )
            String message,
            final ExecutionContext context
    ) {
        try {
            context.getLogger().info("=== NOTIFICACION RECIBIDA DE LA COLA ===");
            context.getLogger().info("Mensaje recibido: " + message);

            var mapper = JsonSupport.mapper();
            var root = mapper.readTree(message);

            String prestamoId = root.has("prestamoId") ? root.get("prestamoId").asText() : "N/A";
            String usuarioId = root.has("usuarioId") ? root.get("usuarioId").asText() : "N/A";
            String libroTitulo = root.has("libroTitulo") ? root.get("libroTitulo").asText() : "N/A";
            String tipo = root.has("tipo") ? root.get("tipo").asText() : "N/A";

            context.getLogger().info(String.format(
                    "Procesando notificacion: tipo=%s, prestamoId=%s, usuarioId=%s, libro='%s'",
                    tipo, prestamoId, usuarioId, libroTitulo));

            context.getLogger().info(String.format(
                    "Simulando envio de correo de confirmacion de prestamo para usuarioId=%s, libro='%s'",
                    usuarioId, libroTitulo));

            context.getLogger().info("=== NOTIFICACION PROCESADA EXITOSAMENTE ===");
        } catch (Exception ex) {
            context.getLogger().severe("Error procesando notificacion de la cola: " + ex.getMessage());
            throw new RuntimeException("Error procesando mensaje de Service Bus", ex);
        }
    }
}
