package com.biblioteca.bff.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServiceBusProducerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBusProducerService.class);

    private final ServiceBusSenderClient senderClient;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public ServiceBusProducerService(
            @Value("${azure.servicebus.connection-string}") String connectionString,
            @Value("${azure.servicebus.queue-name}") String queueName,
            ObjectMapper objectMapper
    ) {
        this.queueName = queueName;
        this.objectMapper = objectMapper;
        ServiceBusSenderClient client = null;
        if (connectionString != null && !connectionString.isBlank()) {
            try {
                client = new ServiceBusClientBuilder()
                        .connectionString(connectionString)
                        .sender()
                        .queueName(queueName)
                        .buildClient();
                LOGGER.info("ServiceBusSenderClient inicializado para la cola '{}'", queueName);
            } catch (Exception ex) {
                LOGGER.warn("No fue posible inicializar ServiceBusSenderClient (EDA S5 desactivado): {}", ex.getMessage());
            }
        } else {
            LOGGER.warn("SERVICEBUS_CONNECTION_STRING no configurado — EDA S5 (Service Bus) desactivado.");
        }
        this.senderClient = client;
    }

    public void enviarNotificacion(Object payload) {
        if (senderClient == null) {
            LOGGER.warn("ServiceBusSenderClient no disponible — mensaje de notificacion omitido (EDA S5 inactivo).");
            return;
        }
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("No fue posible serializar el mensaje a JSON", ex);
        }

        try {
            ServiceBusMessage message = new ServiceBusMessage(jsonPayload);
            senderClient.sendMessage(message);
            LOGGER.info("Mensaje enviado a cola '{}': {}", queueName, jsonPayload);
        } catch (Exception ex) {
            LOGGER.error("Error enviando mensaje a cola '{}': {}", queueName, ex.getMessage(), ex);
            throw new RuntimeException("No fue posible enviar el mensaje al Service Bus", ex);
        }
    }

    @PreDestroy
    public void close() {
        if (senderClient != null) {
            senderClient.close();
            LOGGER.info("ServiceBusSenderClient cerrado correctamente");
        }
    }
}
