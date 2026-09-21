package com.rev.otus.profile;

import com.rev.otus.common.validation.ValidationGroups;
import com.rev.otus.profile.dto.CreateResult;
import com.rev.otus.profile.dto.ProfileDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер для CRUD операций над профилем пользователя.
 *
 * <p><b>Endpoints:</b>
 * <ul>
 *   <li>{@code POST /api/v1/profile} — создание (публичный)</li>
 *   <li>{@code GET /api/v1/profile} — чтение (роль {@code user})</li>
 *   <li>{@code PUT /api/v1/profile} — полное обновление (роль {@code user})</li>
 *   <li>{@code PATCH /api/v1/profile} — частичное обновление (роль {@code user})</li>
 *   <li>{@code DELETE /api/v1/profile} — удаление (роль {@code user})</li>
 * </ul>
 *
 * <p><b>Идентификация:</b> во всех защищённых операциях используется
 * {@code sub} из JWT — это {@code keycloakUserId} пользователя.
 *
 * @see CustomerProfileService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "CRUD операции над профилем пользователя")
public class CustomerProfileController {

    private final CustomerProfileService service;

    /**
     * Создание профиля (идемпотентное).
     *
     * <p><b>Коды ответа:</b>
     * <ul>
     *   <li>{@code 201 Created} — профиль создан впервые</li>
     *   <li>{@code 200 OK} — профиль уже существовал, обновлены не-уникальные поля</li>
     *   <li>{@code 400 Bad Request} — ошибка валидации DTO</li>
     *   <li>{@code 409 Conflict} — username или email занят</li>
     * </ul>
     *
     * @param dto входящий DTO
     * @return созданный или обновлённый профиль
     */
    @PostMapping
    @Operation(summary = "Создать профиль",
            description = "Идемпотентная операция: при повторном запросе с теми же username+email обновляет не-уникальные поля")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Профиль создан"),
            @ApiResponse(responseCode = "200", description = "Профиль уже существовал, обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "409", description = "Username или email занят")
    })
    public ResponseEntity<ProfileDto> create(
            @Validated(ValidationGroups.OnCreate.class) @RequestBody ProfileDto dto) {

        CreateResult result = service.create(dto);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.dto());
    }

    /**
     * Получить свой профиль.
     *
     * <p><b>Коды ответа:</b>
     * <ul>
     *   <li>{@code 200 OK} — профиль найден</li>
     *   <li>{@code 401 Unauthorized} — нет JWT</li>
     *   <li>{@code 403 Forbidden} — нет роли {@code user}</li>
     *   <li>{@code 404 Not Found} — профиль не найден</li>
     * </ul>
     *
     * @param jwt JWT текущего пользователя
     * @return профиль пользователя
     */
    @GetMapping
    @Operation(summary = "Получить профиль",
            description = "Возвращает профиль текущего пользователя по sub из JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль найден"),
            @ApiResponse(responseCode = "401", description = "Нет JWT"),
            @ApiResponse(responseCode = "403", description = "Нет роли user"),
            @ApiResponse(responseCode = "404", description = "Профиль не найден")
    })
    public ResponseEntity<ProfileDto> get(@AuthenticationPrincipal Jwt jwt) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(service.getByKeycloakUserId(keycloakUserId));
    }

    /**
     * Полное обновление профиля (PUT).
     *
     * <p><b>Коды ответа:</b>
     * <ul>
     *   <li>{@code 200 OK} — профиль обновлён</li>
     *   <li>{@code 400 Bad Request} — ошибка валидации</li>
     *   <li>{@code 404 Not Found} — профиль не найден</li>
     *   <li>{@code 409 Conflict} — новый email занят</li>
     * </ul>
     *
     * @param dto входящий DTO
     * @param jwt JWT текущего пользователя
     * @return обновлённый профиль
     */
    @PutMapping
    @Operation(summary = "Обновить профиль (PUT)",
            description = "Полная замена всех полей профиля")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "404", description = "Профиль не найден"),
            @ApiResponse(responseCode = "409", description = "Email занят")
    })
    public ResponseEntity<ProfileDto> update(
            @Validated(ValidationGroups.OnPut.class) @RequestBody ProfileDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(service.update(keycloakUserId, dto));
    }

    /**
     * Частичное обновление профиля (PATCH).
     *
     * <p><b>Коды ответа:</b>
     * <ul>
     *   <li>{@code 200 OK} — профиль обновлён</li>
     *   <li>{@code 400 Bad Request} — ошибка валидации</li>
     *   <li>{@code 404 Not Found} — профиль не найден</li>
     *   <li>{@code 409 Conflict} — новый email занят</li>
     * </ul>
     *
     * @param dto входящий DTO
     * @param jwt JWT текущего пользователя
     * @return обновлённый профиль
     */
    @PatchMapping
    @Operation(summary = "Обновить профиль (PATCH)",
            description = "Частичное обновление: null-поля не меняются")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "404", description = "Профиль не найден"),
            @ApiResponse(responseCode = "409", description = "Email занят")
    })
    public ResponseEntity<ProfileDto> patch(
            @Validated(ValidationGroups.OnPatch.class) @RequestBody ProfileDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(service.patch(keycloakUserId, dto));
    }

    /**
     * Удаление профиля.
     *
     * <p><b>Коды ответа:</b>
     * <ul>
     *   <li>{@code 204 No Content} — профиль удалён</li>
     *   <li>{@code 404 Not Found} — профиль не найден</li>
     * </ul>
     *
     * @param jwt JWT текущего пользователя
     * @return пустой ответ
     */
    @DeleteMapping
    @Operation(summary = "Удалить профиль",
            description = "Удаляет профиль из БД и пользователя из Keycloak")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Профиль удалён"),
            @ApiResponse(responseCode = "404", description = "Профиль не найден")
    })
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt) {
        String keycloakUserId = jwt.getSubject();
        service.delete(keycloakUserId);
        return ResponseEntity.noContent().build();
    }
}