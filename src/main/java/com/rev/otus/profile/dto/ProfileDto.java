package com.rev.otus.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rev.otus.common.validation.NotBlankIfPresent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.rev.otus.common.validation.ValidationGroups.OnCreate;
import static com.rev.otus.common.validation.ValidationGroups.OnPatch;
import static com.rev.otus.common.validation.ValidationGroups.OnPut;

/**
 * DTO для CRUD операций над профилем пользователя.
 */
@Jacksonized
@JsonInclude(NON_NULL)
@Builder(toBuilder = true)
@Schema(description = "Профиль пользователя")
public record ProfileDto(

        // ==================== Только для создания (POST) ====================

        @Schema(description = "Логин пользователя", example = "johndoe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(groups = OnCreate.class)
        @Size(max = 256, groups = OnCreate.class)
        String username,

        @Schema(description = "Пароль (только для создания)", example = "secret123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(groups = OnCreate.class)
        @Size(min = 6, max = 100, groups = OnCreate.class)
        String password,

        // ==================== Для создания и обновления ====================

        @Schema(description = "Email", example = "johndoe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(groups = {OnCreate.class, OnPut.class})
        @Email(groups = {OnCreate.class, OnPut.class, OnPatch.class})
        @Size(max = 255, groups = {OnCreate.class, OnPut.class, OnPatch.class})
        String email,

        @Schema(description = "Имя", example = "John")
        @NotBlank(groups = {OnCreate.class, OnPut.class})
        @Size(max = 100, groups = {OnCreate.class, OnPut.class})
        @NotBlankIfPresent(groups = OnPatch.class)
        String firstName,

        @Schema(description = "Фамилия", example = "Doe")
        @NotBlank(groups = {OnCreate.class, OnPut.class})
        @Size(max = 100, groups = {OnCreate.class, OnPut.class})
        @NotBlankIfPresent(groups = OnPatch.class)
        String lastName,

        @Schema(description = "Предпочитаемый язык", example = "ru", defaultValue = "ru")
        @Size(max = 10, groups = {OnCreate.class, OnPut.class, OnPatch.class})
        @NotBlankIfPresent(groups = {OnCreate.class, OnPut.class, OnPatch.class})
        String preferredLanguage,

        // ==================== Опциональные ====================

        @Schema(description = "Отчество", example = "Иванович")
        @Size(max = 100, groups = {OnCreate.class, OnPut.class, OnPatch.class})
        @NotBlankIfPresent(groups = {OnCreate.class, OnPut.class, OnPatch.class})
        String middleName,

        @Schema(description = "Телефон", example = "+71002003040")
        @Size(max = 20, groups = {OnCreate.class, OnPut.class, OnPatch.class})
        @NotBlankIfPresent(groups = {OnCreate.class, OnPut.class, OnPatch.class})
        String phone,

        @Schema(description = "Дата рождения", example = "1990-01-15")
        @Past(groups = {OnCreate.class, OnPut.class, OnPatch.class})
        LocalDate birthDate,

        // ==================== Только в ответе ====================

        @Schema(description = "Дата создания", accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime createdAt,

        @Schema(description = "Дата обновления", accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime updatedAt
) {}