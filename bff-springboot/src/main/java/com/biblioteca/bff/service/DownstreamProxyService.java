package com.biblioteca.bff.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class DownstreamProxyService {

    private final RestClient.Builder restClientBuilder;

    public DownstreamProxyService(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public ResponseEntity<String> get(String baseUrl, String path) {
        return exchange(HttpMethod.GET, baseUrl, path, null);
    }

    public ResponseEntity<String> post(String baseUrl, String path, Object body) {
        return exchange(HttpMethod.POST, baseUrl, path, body);
    }

    public ResponseEntity<String> put(String baseUrl, String path, Object body) {
        return exchange(HttpMethod.PUT, baseUrl, path, body);
    }

    public ResponseEntity<String> delete(String baseUrl, String path) {
        return exchange(HttpMethod.DELETE, baseUrl, path, null);
    }

        @SuppressWarnings("null")
    private ResponseEntity<String> exchange(HttpMethod method, String baseUrl, String path, Object body) {
        RestClient client = restClientBuilder.baseUrl(Objects.requireNonNull(baseUrl)).build();
        try {
            RestClient.RequestBodySpec requestSpec = client.method(Objects.requireNonNull(method))
                .uri(Objects.requireNonNull(path))
                .accept(MediaType.APPLICATION_JSON);

            DownstreamResult result = body == null
                ? requestSpec.exchange((request, response) -> new DownstreamResult(
                    response.getStatusCode(),
                    extractBody(response.getBody())
                ))
                : requestSpec
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((request, response) -> new DownstreamResult(
                    response.getStatusCode(),
                    extractBody(response.getBody())
                ));

            return ResponseEntity.status(result.status())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result.body().isBlank() ? "{}" : result.body());
        } catch (RestClientException ex) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"success\":false,\"message\":\"No fue posible contactar un servicio interno\",\"data\":null}");
        }
    }

    @SuppressWarnings("null")
    private String extractBody(InputStream body) throws IOException {
        if (body == null) {
            return "";
        }
        return StreamUtils.copyToString(body, StandardCharsets.UTF_8);
    }

    private record DownstreamResult(HttpStatusCode status, String body) {
    }
}
