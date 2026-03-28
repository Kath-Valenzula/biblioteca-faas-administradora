package com.biblioteca.bff.service;

import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;

final class DownstreamUrlResolver {

    URI resolve(String configuredBaseUrl, String downstreamPath) {
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            throw new IllegalArgumentException("La URL base del servicio remoto no esta configurada");
        }

        URI baseUri = URI.create(configuredBaseUrl.trim());
        String basePath = normalizePath(baseUri.getRawPath());
        String requestPath = normalizePath(downstreamPath);
        String resolvedPath = joinPaths(basePath, requestPath);

        return UriComponentsBuilder.newInstance()
                .scheme(baseUri.getScheme())
                .userInfo(baseUri.getUserInfo())
                .host(baseUri.getHost())
                .port(baseUri.getPort())
                .path(resolvedPath)
                .query(baseUri.getRawQuery())
                .fragment(baseUri.getRawFragment())
                .build(true)
                .toUri();
    }

    private String joinPaths(String basePath, String requestPath) {
        if (requestPath.isEmpty()) {
            return basePath;
        }
        if (basePath.isEmpty()) {
            return requestPath;
        }
        if (basePath.equals(requestPath) || basePath.endsWith(requestPath)) {
            return basePath;
        }

        String resourceRoot = extractResourceRoot(requestPath);
        if (!resourceRoot.isEmpty() && basePath.endsWith(resourceRoot)) {
            return basePath + requestPath.substring(resourceRoot.length());
        }
        return basePath + requestPath;
    }

    private String extractResourceRoot(String path) {
        if (path.isEmpty()) {
            return "";
        }
        int nextSlash = path.indexOf('/', 1);
        return nextSlash == -1 ? path : path.substring(0, nextSlash);
    }

    private String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }

        String normalized = rawPath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
