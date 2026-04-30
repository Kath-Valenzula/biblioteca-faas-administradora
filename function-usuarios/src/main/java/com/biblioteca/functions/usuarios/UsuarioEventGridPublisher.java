package com.biblioteca.functions.usuarios;

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

final class UsuarioEventGridPublisher {

    static final String EVENTO_USUARIO_ELIMINADO = "Biblioteca.UsuarioEliminado";

    private static final String TOPIC_ENDPOINT_SETTING = "EVENTGRID_TOPIC_ENDPOINT";
    private static final String TOPIC_KEY_SETTING = "EVENTGRID_TOPIC_KEY";
    private static final String TOPIC_NAME_SETTING = "EVENTGRID_TOPIC_NAME";

    private UsuarioEventGridPublisher() {
    }

    static void publicar(String tipoEvento, Map<String, Object> usuario, int prestamosEliminados, ExecutionContext context) {
        String endpoint = System.getenv(TOPIC_ENDPOINT_SETTING);
        String key = System.getenv(TOPIC_KEY_SETTING);
        String topicName = System.getenv(TOPIC_NAME_SETTING);

        if (isBlank(endpoint) || isBlank(key)) {
            // Best-effort: la BD ya confirmó, el evento es no-crítico (sin outbox pattern).
            context.getLogger().warning(
                    "Event Grid no configurado (faltan EVENTGRID_TOPIC_ENDPOINT / EVENTGRID_TOPIC_KEY); " +
                    "la operacion de BD ya fue confirmada — evento " + tipoEvento + " omitido (best-effort).");
            return;
        }

        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("usuarioId", usuario.get("id"));
        data.put("nombre", usuario.get("nombre"));
        data.put("correo", usuario.get("correo"));
        data.put("tipoEvento", tipoEvento);
        data.put("prestamosEliminados", prestamosEliminados);
        data.put("fechaEvento", OffsetDateTime.now(ZoneOffset.UTC).toString());
        data.put("correlationId", correlationId);

        EventGridEvent event = new EventGridEvent(
                "/biblioteca/usuarios/" + usuario.get("id"),
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
                "Evento Event Grid publicado: topic=%s, tipoEvento=%s, usuarioId=%s, prestamosEliminados=%d, correlationId=%s",
                isBlank(topicName) ? "N/A" : topicName, tipoEvento, usuario.get("id"), prestamosEliminados, correlationId));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
