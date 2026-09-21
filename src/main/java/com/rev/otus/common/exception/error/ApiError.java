package com.rev.otus.common.exception.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.rev.otus.common.exception.GlobalExceptionHandler;
import lombok.Builder;
import org.apache.logging.log4j.internal.annotation.SuppressFBWarnings;

import java.time.Instant;
import java.util.List;

/**
 * DTO для ответа об ошибке API.
 *
 * <p><b>Назначение:</b> стандартизированный формат ответа при возникновении ошибок
 * в REST API. Используется во всех ответах с HTTP статусами 4xx и 5xx.
 *
 * <p><b>Формат JSON-ответа:</b>
 * <pre>{@code
 * {
 *   "timestamp": "2024-01-15T10:30:00.123Z",
 *   "message": "Validation failed",
 *   "code": "VALIDATION_ERROR",
 *   "traceId": "550e8400-e29b-41d4-a716-446655440000",
 *   "details": [
 *     {
 *       "field": "email",
 *       "message": "must be a well-formed email address",
 *       "code": "INVALID_EMAIL",
 *       "rejectedValue": "invalid"
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p><b>Поля:</b>
 * <ul>
 *   <li>{@code timestamp} — время ошибки в формате UTC (ISO 8601)</li>
 *   <li>{@code message} — локализованное сообщение для клиента</li>
 *   <li>{@code code} — стабильный код для программной обработки (например, {@code VALIDATION_ERROR})</li>
 *   <li>{@code traceId} — идентификатор запроса для отслеживания в логах</li>
 *   <li>{@code details} — детали ошибок по полям (для валидации и бизнес-правил)</li>
 * </ul>
 *
 * @see ApiErrorFactory
 * @see GlobalExceptionHandler
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                timezone = "UTC")
        Instant timestamp,        // Время возникновения ошибки (UTC)
        String message,           // Человеко-читаемое сообщение для клиента (локализованное)
        String code,              // Стабильный код ошибки для программной обработки
        String traceId,           // Уникальный идентификатор запроса для поддержки
        @SuppressFBWarnings("EI_EXPOSE_REP")
        List<FieldError> details  // Детали ошибок по полям (для валидации)
) {
    /**
     * Конструктор record, обеспечивающий неизменяемость списка деталей.
     *
     * <p>Создаёт неизменяемую копию списка {@code details}, если он не {@code null}.
     *
     * @param details список деталей ошибок (может быть {@code null})
     */
    public ApiError {
        if (details != null) {
            details = List.copyOf(details);
        }
    }

    /**
     * Детализация ошибки для конкретного поля запроса.
     *
     * <p><b>Назначение:</b> предоставляет дополнительную информацию об ошибке,
     * связанной с конкретным полем входных данных.
     *
     * <p><b>Где используется:</b>
     * <ul>
     *   <li>Валидация тела запроса — {@code @NotNull}, {@code @Size}, {@code @Pattern} и т.д.</li>
     *   <li>Десериализация JSON — неверный тип данных, неверный формат даты</li>
     *   <li>Бизнес-правила — ошибки, привязанные к конкретному полю</li>
     *   <li>Ошибки параметров запроса — отсутствие или неверный тип параметра</li>
     * </ul>
     *
     * <p><b>Важно:</b> Каждый объект {@code FieldError} описывает ровно одну ошибку одного поля.
     * Если поле имеет несколько ошибок, в списке будет несколько элементов с одинаковым {@code field}.
     *
     * <p><b>Примеры:</b>
     * <pre>{@code
     * // Ошибка валидации
     * FieldError.builder()
     *     .field("email")
     *     .message("must be a well-formed email address")
     *     .code("INVALID_EMAIL")
     *     .rejectedValue("invalid")
     *     .build();
     *
     * // Ошибка десериализации JSON
     * FieldError.builder()
     *     .field("age")
     *     .message("Failed to convert 'abc' to type Integer")
     *     .code("INVALID_FORMAT")
     *     .rejectedValue("abc")
     *     .build();
     *
     * // Ошибка бизнес-правила
     * FieldError.builder()
     *     .field("ffcId")
     *     .message("FFC with ID 12345 not found")
     *     .code("NOT_FOUND")
     *     .rejectedValue("12345")
     *     .build();
     * }</pre>
     *
     * @see ApiError#details
     */
    @Builder
    public record FieldError(
            String field,           // Имя поля, в котором произошла ошибка
            String message,         // Человеко-читаемое сообщение для клиента (локализованное)
            String code,            // Стабильный код ошибки поля (EN). Используется для программной обработки на клиенте
            Object rejectedValue    // Значение, которое не прошло валидацию (может быть null)
    ) {}
}
