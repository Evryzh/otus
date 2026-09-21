package com.rev.otus.keycloak;

import com.rev.otus.common.exception.BusinessException;
import com.rev.otus.common.exception.error.ErrorCode;
import com.rev.otus.keycloak.dto.KeycloakRoleRepresentation;
import com.rev.otus.keycloak.dto.KeycloakUserRequest;
import com.rev.otus.profile.dto.ProfileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Сервис для работы с Keycloak Admin API.
 *
 * <p><b>Назначение:</b> централизует все вызовы административного API Keycloak,
 * необходимые для управления жизненным циклом пользователей приложения.
 *
 * <p><b>Что делает:</b>
 * <ul>
 *   <li>Создаёт пользователей в Keycloak ({@link #createUser(ProfileDto)})</li>
 *   <li>Назначает роли ({@link #assignRole(String, String)})</li>
 *   <li>Удаляет пользователей ({@link #deleteUser(String)})</li>
 *   <li>Получает admin-токен для вызовов ({@link #getAdminToken()})</li>
 * </ul>
 *
 * <p><b>Аутентификация:</b> для вызовов Admin API используется отдельный
 * admin-токен, получаемый через {@code admin-cli} клиент master realm.
 * Этот токен <b>не</b> связан с JWT обычных пользователей.
 *
 * <p><b>Обработка ошибок:</b> все ошибки Keycloak преобразуются в
 * {@link BusinessException} с локализованными сообщениями
 * (ключи вида {@code error.keycloak.*}).
 *
 * @see KeycloakProperties
 * @see KeycloakUserRequest
 * @see KeycloakRoleRepresentation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private final RestClient restClient;
    private final KeycloakProperties properties;

    /**
     * Создаёт пользователя в Keycloak и назначает ему роль {@code user}.
     *
     * <p><b>Алгоритм:</b>
     * <ol>
     *   <li>Получить admin-токен</li>
     *   <li>Отправить POST-запрос на создание пользователя</li>
     *   <li>Извлечь UUID пользователя из заголовка {@code Location}</li>
     *   <li>Назначить роль {@code user}</li>
     * </ol>
     *
     * <p><b>Обработка ошибок:</b>
     * <ul>
     *   <li>409 Conflict → {@link ErrorCode#DUPLICATE_ENTITY} (пользователь уже есть)</li>
     *   <li>Прочие ошибки → {@link ErrorCode#INTERNAL_ERROR}</li>
     * </ul>
     *
     * @param dto данные профиля (username, password, email, firstName, lastName)
     * @return UUID созданного пользователя в Keycloak
     * @throws BusinessException если Keycloak вернул ошибку
     */
    public String createUser(ProfileDto dto) {
        log.info("Creating user in Keycloak: username={}", dto.username());

        String adminToken = getAdminToken();
        String url = properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users";

        KeycloakUserRequest request = KeycloakUserRequest.withPassword(
                dto.username(),
                dto.password(),
                dto.email(),
                dto.firstName(),
                dto.lastName()
        );

        try {
            String location = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getFirst(HttpHeaders.LOCATION);

            if (location == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, null, null, "error.keycloak.no_user_id");
            }

            String userId = location.substring(location.lastIndexOf('/') + 1);
            log.info("User created in Keycloak: userId={}", userId);

            assignRole(userId, "user");
            return userId;

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409) {
                log.warn("User already exists in Keycloak: username={}", dto.username());
                throw new BusinessException(ErrorCode.DUPLICATE_ENTITY,
                        "username", dto.username(),
                        "error.keycloak.user_already_exists");
            }
            log.error("Failed to create user in Keycloak: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, null, null, null,
                    "error.keycloak.creation_failed", e.getStatusText());
        }
    }

    /**
     * Назначает роль пользователю в realm.
     *
     * <p><b>Алгоритм:</b>
     * <ol>
     *   <li>Получить admin-токен</li>
     *   <li>Получить данные роли по имени через {@code GET /roles/{name}}</li>
     *   <li>Назначить роль через {@code POST /users/{id}/role-mappings/realm}</li>
     * </ol>
     *
     * <p><b>Почему нужно получать роль:</b> Keycloak требует передать
     * в теле запроса полное представление роли (с {@code id} и {@code name}),
     * а не только её имя.
     *
     * @param userId   UUID пользователя в Keycloak
     * @param roleName имя роли (например, {@code user})
     * @throws BusinessException если роль не найдена или Keycloak вернул ошибку
     */
    public void assignRole(String userId, String roleName) {
        log.info("Assigning role to user: userId={}, role={}", userId, roleName);

        String adminToken = getAdminToken();
        String realm = properties.getRealm();
        String baseUrl = properties.getBaseUrl();

        String roleUrl = baseUrl + "/admin/realms/" + realm + "/roles/" + roleName;

        KeycloakRoleRepresentation role = restClient.get()
                .uri(roleUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(KeycloakRoleRepresentation.class);

        if (role == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, null, null, null,
                    "error.keycloak.role_not_found", roleName);
        }

        String assignUrl = baseUrl + "/admin/realms/" + realm
                + "/users/" + userId + "/role-mappings/realm";

        restClient.post()
                .uri(assignUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();

        log.info("Role assigned successfully: userId={}, role={}", userId, roleName);
    }

    /**
     * Удаляет пользователя из Keycloak.
     *
     * <p><b>Особенности:</b>
     * <ul>
     *   <li>Если пользователь не найден (404) — не считается ошибкой,
     *       логируется предупреждение</li>
     *   <li>Другие ошибки → {@link ErrorCode#INTERNAL_ERROR}</li>
     * </ul>
     *
     * @param userId UUID пользователя в Keycloak
     * @throws BusinessException если Keycloak вернул ошибку (кроме 404)
     */
    public void deleteUser(String userId) {
        log.info("Deleting user from Keycloak: userId={}", userId);

        String adminToken = getAdminToken();
        String url = properties.getBaseUrl()
                + "/admin/realms/" + properties.getRealm() + "/users/" + userId;

        try {
            restClient.delete()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .toBodilessEntity();
            log.info("User deleted from Keycloak: userId={}", userId);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                log.warn("User not found in Keycloak: userId={}", userId);
            } else {
                log.error("Failed to delete user from Keycloak: {} {}",
                        e.getStatusCode(), e.getResponseBodyAsString());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        null, null,
                        "error.keycloak.deletion_failed");
            }
        }
    }

    /**
     * Получает admin-токен для работы с Keycloak Admin API.
     *
     * <p><b>Endpoint:</b> {@code POST /realms/master/protocol/openid-connect/token}
     *
     * <p><b>Параметры запроса:</b>
     * <ul>
     *   <li>{@code grant_type=password}</li>
     *   <li>{@code client_id=admin-cli}</li>
     *   <li>{@code username} / {@code password} — учётные данные администратора</li>
     * </ul>
     *
     * <p><b>Токен имеет срок жизни</b> (по умолчанию 5 минут) и
     * <b>не кэшируется</b> — при каждом вызове Admin API получается новый.
     * Для продакшена стоит добавить кэширование.
     *
     * @return access token администратора
     * @throws BusinessException если не удалось получить токен
     */
    private String getAdminToken() {
        String url = properties.getBaseUrl()
                + "/realms/master/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", properties.getAdmin().getUsername());
        body.add("password", properties.getAdmin().getPassword());
        body.add("grant_type", "password");
        body.add("client_id", "admin-cli");

        Map<String, Object> responseBody = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (responseBody == null || responseBody.get("access_token") == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    null, null,
                    "error.keycloak.token_failed");
        }

        return (String) responseBody.get("access_token");
    }
}