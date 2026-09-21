package com.rev.otus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Конфигурация {@link RestClient} для вызовов Keycloak Admin API.
 *
 * <p>Создаёт единственный бин {@link RestClient}, который используется
 * в {@code KeycloakAdminService}.
 */
@Configuration
public class RestClientConfig {

    /**
     * Создаёт {@link RestClient} из автоматически настроенного builder.
     *
     * <p>Таймауты задаются в {@code application.yaml}:
     * <pre>{@code
     * spring:
     *   http:
     *     clients:
     *       connect-timeout: 5s
     *       read-timeout: 10s
     * }</pre>
     *
     * @param builder автоматически настроенный Spring Boot builder
     * @return настроенный {@link RestClient}
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}