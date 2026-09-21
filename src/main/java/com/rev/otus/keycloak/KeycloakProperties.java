package com.rev.otus.keycloak;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурация Keycloak Admin API.
 *
 * <p>Читает свойства из {@code application.yaml} с префиксом {@code keycloak}.
 */
@Data
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    /** Базовый URL Keycloak. */
    private String baseUrl;

    /** Realm, в котором создаются пользователи. */
    private String realm;

    /** Client ID приложения. */
    private String clientId;

    /** Настройки администратора Keycloak. */
    private Admin admin = new Admin();

    @Data
    public static class Admin {
        private String username;
        private String password;
    }
}