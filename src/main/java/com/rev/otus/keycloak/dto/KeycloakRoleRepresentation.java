package com.rev.otus.keycloak.dto;

/**
 * DTO роли Keycloak.
 */
public record KeycloakRoleRepresentation(
        String id,
        String name,
        String description,
        Boolean composite,
        Boolean clientRole,
        String containerId
) {
}