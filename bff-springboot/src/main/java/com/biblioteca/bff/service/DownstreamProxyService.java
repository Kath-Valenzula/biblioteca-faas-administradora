package com.biblioteca.bff.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DownstreamProxyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownstreamProxyService.class);

    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final DownstreamUrlResolver urlResolver = new DownstreamUrlResolver();

    public DownstreamProxyService(
            @Value("${services.downstream.connect-timeout:5s}") Duration connectTimeout,
            @Value("${services.downstream.read-timeout:60s}") Duration readTimeout
    ) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
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
        URI targetUri;
        try {
            targetUri = urlResolver.resolve(baseUrl, path);
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Configuracion invalida para servicio remoto. baseUrl='{}', path='{}'",
                    baseUrl, path, ex);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "La configuracion del servicio remoto en el BFF no es valida");
        }

        try {
            RestClient.RequestBodySpec requestSpec = RestClient.builder()
                .requestFactory(buildRequestFactory())
                .build()
                .method(method)
                .uri(targetUri)
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
        } catch (ResourceAccessException ex) {
            return handleResourceAccess(method, targetUri, ex);
        } catch (RestClientException ex) {
            LOGGER.error("Error invocando servicio remoto {} {}", method, sanitizeUri(targetUri), ex);
            return buildErrorResponse(HttpStatus.BAD_GATEWAY,
                    "El BFF no pudo completar la llamada al servicio remoto");
        }
    }

    @SuppressWarnings("null")
    private String extractBody(InputStream body) throws IOException {
        if (body == null) {
            return "";
        }
        return StreamUtils.copyToString(body, StandardCharsets.UTF_8);
    }

    private ResponseEntity<String> handleResourceAccess(HttpMethod method, URI targetUri, ResourceAccessException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (isTimeout(cause)) {
            LOGGER.warn("Timeout invocando servicio remoto {} {}", method, sanitizeUri(targetUri), ex);
            return buildErrorResponse(HttpStatus.GATEWAY_TIMEOUT,
                    "El servicio remoto demoro demasiado en responder");
        }

        LOGGER.error("No fue posible conectar con servicio remoto {} {}", method, sanitizeUri(targetUri), ex);
        return buildErrorResponse(HttpStatus.BAD_GATEWAY,
                "No fue posible conectar con el servicio remoto. Verifica la URL configurada");
    }

    private boolean isTimeout(Throwable cause) {
        return cause instanceof SocketTimeoutException
                || cause instanceof InterruptedIOException
                || cause instanceof java.net.http.HttpTimeoutException;
    }

    private String sanitizeUri(URI uri) {
        return UriComponentsBuilder.newInstance()
                .scheme(uri.getScheme())
                .host(uri.getHost())
                .port(uri.getPort())
                .path(uri.getRawPath())
                .build(true)
                .toUriString();
    }

    private SimpleClientHttpRequestFactory buildRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());
        return requestFactory;
    }

    private ResponseEntity<String> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }

    private record DownstreamResult(HttpStatusCode status, String body) {
    }
}
