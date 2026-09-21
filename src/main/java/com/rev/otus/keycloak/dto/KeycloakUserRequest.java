package com.rev.otus.keycloak.dto;

import java.util.List;

/**
 * DTO для запроса создания пользователя в Keycloak.
 */
public record KeycloakUserRequest(
        String username,
        String email,
        String firstName,
        String lastName,
        Boolean enabled,
        Boolean emailVerified,
        List<Credential> credentials
) {

    /**
     * Пароль пользователя в Keycloak.
     *
     * @param type      тип credential (всегда "password")
     * @param value     пароль
     * @param temporary true — требует смены при первом входе
     */
    public record Credential(
            String type,
            String value,
            Boolean temporary
    ) {}

    public static KeycloakUserRequest withPassword(
            String username,
            String password,
            String email,
            String firstName,
            String lastName) {
        return new KeycloakUserRequest(
                username,
                email,
                firstName,
                lastName,
                true,
                false,
                List.of(new Credential("password", password, false))
        );
    }
}