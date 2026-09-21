package com.rev.otus.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Конфигурация Spring Security.
 *
 * <p><b>Что настраивается:</b>
 * <ul>
 *   <li>OAuth2 Resource Server с проверкой JWT от Keycloak</li>
 *   <li>Правила доступа к эндпоинтам</li>
 *   <li>Извлечение ролей из JWT ({@code realm_access.roles})</li>
 * </ul>
 *
 * <p><b>Философия безопасности:</b> "всё запрещено, кроме явно разрешённого"
 * (fail-safe). Любой новый эндпоинт по умолчанию недоступен — нужно явно
 * разрешить в правилах.
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Настраивает цепочку фильтров Spring Security.
     *
     * <p><b>Правила доступа:</b>
     * <ul>
     *   <li>{@code POST /api/v1/profile} — публичный (регистрация)</li>
     *   <li>{@code /health}, {@code /actuator/**} — публичные</li>
     *   <li>{@code /v3/api-docs/**}, {@code /swagger-ui/**} — публичные (Swagger)</li>
     *   <li>{@code /api/v1/profile/**} — требуют роль {@code user}</li>
     *   <li>Всё остальное — запрещено ({@code denyAll})</li>
     * </ul>
     *
     * @param http HttpSecurity builder
     * @return настроенная цепочка фильтров
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        log.info("Configuring SecurityFilterChain: OAuth2 Resource Server with Keycloak JWT");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Публичные эндпоинты
                        .requestMatchers(HttpMethod.POST, "/api/v1/profile").permitAll()
                        .requestMatchers(
                                "/health",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()

                        // Защищённые эндпоинты
                        .requestMatchers("/api/v1/profile/**").hasRole("user")

                        // Всё остальное — запрещено (fail-safe)
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        log.info("SecurityFilterChain configured successfully");
        return http.build();
    }

    /**
     * Конвертер JWT → Authentication.
     *
     * <p>Извлекает роли из {@code realm_access.roles} и добавляет префикс {@code ROLE_}.
     *
     * <p><b>Пример:</b>
     * <ul>
     *   <li>В JWT: {@code realm_access.roles: ["default-roles-otus", "user"]}</li>
     *   <li>В Spring Security: {@code ROLE_default-roles-otus}, {@code ROLE_user}</li>
     *   <li>{@code hasRole("user")} проверяет {@code ROLE_user}</li>
     * </ul>
     *
     * @return настроенный конвертер
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || realmAccess.get("roles") == null) {
                return List.of();
            }
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });
        return converter;
    }
}