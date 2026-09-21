package com.rev.otus.profile.dto;

/**
 * Результат идемпотентного POST /api/v1/profile.
 *
 * <p>Используется в {@code CustomerProfileController.create()} для выбора
 * HTTP-статуса:
 * <ul>
 *   <li>{@code created = true} → 201 Created</li>
 *   <li>{@code created = false} → 200 OK</li>
 * </ul>
 *
 * @param dto     созданный или обновлённый профиль
 * @param created true — профиль создан впервые, false — обновлён существующий
 */
public record CreateResult(
        ProfileDto dto,
        boolean created
) {}