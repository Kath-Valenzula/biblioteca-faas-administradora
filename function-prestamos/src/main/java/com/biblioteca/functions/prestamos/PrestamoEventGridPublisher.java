package com.biblioteca.functions.prestamos;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import com.microsoft.azure.functions.ExecutionContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class PrestamoEventGridPublisher {

    static final String EVENTO_PRESTAMO_CREADO = "Biblioteca.PrestamoCreado";
    static final String EVENTO_PRESTAMO_DEVUELTO = "Biblioteca.PrestamoDevuelto";

    private static final String TOPIC_ENDPOINT_SETTING = "EVENTGRID_TOPIC_ENDPOINT";
    private static final String TOPIC_KEY_SETTING = "EVENTGRID_TOPIC_KEY";
    private static final String TOPIC_NAME_SETTING = "EVENTGRID_TOPIC_NAME";

    private PrestamoEventGridPublisher() {
    }

    static void publicar(String tipoEvento, Map<String, Object> prestamo, ExecutionContext context) {
        String endpoint = System.getenv(TOPIC_ENDPOINT_SETTING);
        String key = System.getenv(TOPIC_KEY_SETTING);
        String topicName = System.getenv(TOPIC_NAME_SETTING);

        if (isBlank(endpoint) || isBlank(key)) {
            context.getLogger().warning("Event Grid no configurado; se omite publicacion del evento " + tipoEvento);
            return;
        }

        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("prestamoId", prestamo.get("id"));
        data.put("usuarioId", prestamo.get("usuarioId"));
        data.put("libroId", prestamo.get("libroId"));
        data.put("tipoEvento", tipoEvento);
        data.put("fechaEvento", OffsetDateTime.now(ZoneOffset.UTC).toString());
        data.put("estado", prestamo.get("estado"));
        data.put("correlationId", correlationId);

        EventGridEvent event = new EventGridEvent(
                "/biblioteca/prestamos/" + prestamo.get("id"),
                tipoEvento,
                BinaryData.fromObject(data),
                "1.0"
        );

        EventGridPublisherClient<EventGridEvent> client = new EventGridPublisherClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(key))
                .buildEventGridEventPublisherClient();

        client.sendEvent(event);
        context.getLogger().info(String.format(
                "Evento Event Grid publicado: topic=%s, tipoEvento=%s, prestamoId=%s, usuarioId=%s, libroId=%s, correlationId=%s",
                safeTopicName(topicName), tipoEvento, prestamo.get("id"), prestamo.get("usuarioId"),
                prestamo.get("libroId"), correlationId));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safeTopicName(String topicName) {
        return isBlank(topicName) ? "N/A" : topicName;
    }
}
