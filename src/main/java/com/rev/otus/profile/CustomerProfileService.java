package com.rev.otus.profile;

import com.rev.otus.common.exception.BusinessException;
import com.rev.otus.common.exception.error.ErrorCode;
import com.rev.otus.keycloak.KeycloakAdminService;
import com.rev.otus.profile.dto.CreateResult;
import com.rev.otus.profile.dto.ProfileDto;
import com.rev.otus.profile.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Бизнес-логика CRUD операций над профилем пользователя.
 *
 * <p><b>Особенности:</b>
 * <ul>
 *   <li>POST идемпотентен: повторный запрос с теми же {@code username + email}
 *       обновляет не-уникальные поля профиля и возвращает {@code 200 OK}</li>
 *   <li>При создании профиля также создаётся пользователь в Keycloak</li>
 *   <li>При удалении профиля удаляется пользователь из Keycloak</li>
 *   <li>{@code username} неизменяем после создания</li>
 * </ul>
 *
 * @see CustomerProfile
 * @see ProfileMapper
 * @see KeycloakAdminService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final CustomerProfileRepository repository;
    private final ProfileMapper profileMapper;
    private final KeycloakAdminService keycloakAdminService;

    /**
     * Идемпотентное создание профиля (POST).
     *
     * <p><b>Алгоритм:</b>
     * <ol>
     *   <li>Поиск профиля по {@code username + email}</li>
     *   <li>Если найден — обновить не-уникальные поля, вернуть {@code created=false}</li>
     *   <li>Если не найден:
     *     <ul>
     *       <li>Проверить уникальность {@code username} → 409</li>
     *       <li>Проверить уникальность {@code email} → 409</li>
     *       <li>Создать пользователя в Keycloak</li>
     *       <li>Назначить роль {@code user}</li>
     *       <li>Сохранить профиль → {@code created=true}</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param dto входящий DTO с данными профиля
     * @return результат с флагом {@code created}: true → 201, false → 200
     * @throws BusinessException если username/email занят или Keycloak вернул ошибку
     */
    @Transactional
    public CreateResult create(ProfileDto dto) {
        log.info("Creating profile: username={}, email={}", dto.username(), dto.email());

        if (dto.preferredLanguage() == null) {
            dto = dto.toBuilder().preferredLanguage("ru").build();
        }

        // 1. Поиск по username + email (идемпотентность)
        Optional<CustomerProfile> existing = repository.findByUsernameAndEmail(dto.username(), dto.email());

        if (existing.isPresent()) {
            log.info("Profile already exists, updating non-unique fields: username={}", dto.username());
            CustomerProfile profile = existing.get();
            profile.setFirstName(dto.firstName());
            profile.setLastName(dto.lastName());
            profile.setMiddleName(dto.middleName());
            profile.setPhone(dto.phone());
            profile.setBirthDate(dto.birthDate());
            profile.setPreferredLanguage(dto.preferredLanguage());

            CustomerProfile saved = repository.save(profile);
            return new CreateResult(profileMapper.toDto(saved), false);
        }

        // 2. Проверка уникальности
        if (repository.existsByUsername(dto.username())) {
            log.warn("Username already taken: {}", dto.username());
            throw new BusinessException(ErrorCode.DUPLICATE_ENTITY,
                    "username", dto.username(), null,
                    "error.profile.username_taken", dto.username());
        }
        if (repository.existsByEmail(dto.email())) {
            log.warn("Email already taken: {}", dto.email());
            throw new BusinessException(ErrorCode.DUPLICATE_ENTITY,
                    "email", dto.email(), null,
                    "error.profile.email_taken", dto.email());
        }

        // 3. Создание в Keycloak
        String keycloakUserId = keycloakAdminService.createUser(dto);

        // 4. Сохранение профиля
        CustomerProfile profile = profileMapper.toEntity(dto);
        profile.setKeycloakUserId(keycloakUserId);

        CustomerProfile saved = repository.save(profile);
        log.info("Profile created: id={}, keycloakUserId={}", saved.getId(), keycloakUserId);

        return new CreateResult(profileMapper.toDto(saved), true);
    }

    /**
     * Получить профиль по {@code keycloakUserId} (GET).
     *
     * @param keycloakUserId {@code sub} из JWT
     * @return DTO профиля
     * @throws BusinessException если профиль не найден (404)
     */
    @Transactional(readOnly = true)
    public ProfileDto getByKeycloakUserId(String keycloakUserId) {
        log.debug("Getting profile by keycloakUserId={}", keycloakUserId);

        CustomerProfile profile = repository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "keycloakUserId", keycloakUserId, null,
                        "error.profile.not_found.by_id", keycloakUserId));

        return profileMapper.toDto(profile);
    }

    /**
     * Полное обновление профиля (PUT).
     *
     * <p><b>Особенности:</b>
     * <ul>
     *   <li>Все поля из DTO перезаписываются (включая {@code null})</li>
     *   <li>{@code username} и {@code keycloakUserId} не меняются</li>
     *   <li>Если {@code email} меняется — проверяется уникальность</li>
     * </ul>
     *
     * @param keycloakUserId {@code sub} из JWT
     * @param dto            входящий DTO
     * @return обновлённый профиль
     * @throws BusinessException если профиль не найден или email занят
     */
    @Transactional
    public ProfileDto update(String keycloakUserId, ProfileDto dto) {
        log.info("Updating profile (PUT): keycloakUserId={}", keycloakUserId);

        CustomerProfile profile = repository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "keycloakUserId", keycloakUserId, null,
                        "error.profile.not_found.by_id", keycloakUserId));

        // Проверка уникальности email (если меняется)
        if (!profile.getEmail().equals(dto.email()) && repository.existsByEmail(dto.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ENTITY,
                    "email", dto.email(), null,
                    "error.profile.email_taken", dto.email());
        }

        profileMapper.updateEntity(dto, profile);
        CustomerProfile saved = repository.save(profile);

        log.info("Profile updated: id={}", saved.getId());
        return profileMapper.toDto(saved);
    }

    /**
     * Частичное обновление профиля (PATCH).
     *
     * <p><b>Особенности:</b>
     * <ul>
     *   <li>{@code null} поля из DTO <b>не перезаписывают</b> профиль</li>
     *   <li>{@code username} и {@code keycloakUserId} не меняются</li>
     *   <li>Если {@code email} передан и меняется — проверяется уникальность</li>
     * </ul>
     *
     * @param keycloakUserId {@code sub} из JWT
     * @param dto            входящий DTO
     * @return обновлённый профиль
     * @throws BusinessException если профиль не найден или email занят
     */
    @Transactional
    public ProfileDto patch(String keycloakUserId, ProfileDto dto) {
        log.info("Updating profile (PATCH): keycloakUserId={}", keycloakUserId);

        CustomerProfile profile = repository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "keycloakUserId", keycloakUserId, null,
                        "error.profile.not_found.by_id", keycloakUserId));

        // Проверка уникальности email (если передан и меняется)
        if (dto.email() != null
                && !profile.getEmail().equals(dto.email())
                && repository.existsByEmail(dto.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ENTITY,
                    "email", dto.email(), null,
                    "error.profile.email_taken", dto.email());
        }

        profileMapper.patchEntity(dto, profile);
        CustomerProfile saved = repository.save(profile);

        log.info("Profile patched: id={}", saved.getId());
        return profileMapper.toDto(saved);
    }

    /**
     * Удаление профиля и пользователя из Keycloak (DELETE).
     *
     * <p><b>Порядок:</b>
     * <ol>
     *   <li>Удалить пользователя из Keycloak</li>
     *   <li>Удалить профиль из БД</li>
     * </ol>
     *
     * @param keycloakUserId {@code sub} из JWT
     * @throws BusinessException если профиль не найден
     */
    @Transactional
    public void delete(String keycloakUserId) {
        log.info("Deleting profile: keycloakUserId={}", keycloakUserId);

        CustomerProfile profile = repository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "keycloakUserId", keycloakUserId, null,
                        "error.profile.not_found.by_id", keycloakUserId));

        // Удаляем из Keycloak
        keycloakAdminService.deleteUser(profile.getKeycloakUserId());

        // Удаляем из БД
        repository.delete(profile);
        log.info("Profile deleted: id={}", profile.getId());
    }
}