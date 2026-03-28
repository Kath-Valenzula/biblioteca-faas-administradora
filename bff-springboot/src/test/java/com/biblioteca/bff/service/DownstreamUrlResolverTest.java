package com.biblioteca.bff.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.api.Test;

class DownstreamUrlResolverTest {

    private final DownstreamUrlResolver resolver = new DownstreamUrlResolver();

    @Test
    void appendsRouteWhenBaseUrlEndsAtApiRoot() {
        URI resolved = resolver.resolve("https://biblioteca.azurewebsites.net/api", "/usuarios");

        assertEquals("https://biblioteca.azurewebsites.net/api/usuarios", resolved.toString());
    }

    @Test
    void preservesConfiguredRouteWhenBaseUrlAlreadyContainsResourcePath() {
        URI resolved = resolver.resolve("https://biblioteca.azurewebsites.net/api/usuarios", "/usuarios");

        assertEquals("https://biblioteca.azurewebsites.net/api/usuarios", resolved.toString());
    }

    @Test
    void preservesConfiguredRouteAndQueryStringForNestedRequests() {
        URI resolved = resolver.resolve(
                "https://biblioteca.azurewebsites.net/api/prestamos?code=abc123",
                "/prestamos/7/devolucion");

        assertEquals("https://biblioteca.azurewebsites.net/api/prestamos/7/devolucion?code=abc123",
                resolved.toString());
    }

    @Test
    void appendsNestedRequestWhenBaseUrlOnlyContainsApiSegment() {
        URI resolved = resolver.resolve("https://biblioteca.azurewebsites.net/api/", "/usuarios/2");

        assertEquals("https://biblioteca.azurewebsites.net/api/usuarios/2", resolved.toString());
    }
}
