package com.rev.otus.common.exception.error;

import com.rev.otus.common.exception.GlobalExceptionHandler;
import com.rev.otus.common.utils.TraceIdUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.rev.otus.common.exception.error.ApiError.FieldError;

import java.time.Instant;
import java.util.List;

/**
 * Фабрика для создания {@link ApiError} и {@link FieldError}.
 *
 * <p><b>Назначение:</b> централизованное создание объектов ошибок API с автоматическим
 * заполнением общих полей (timestamp, traceId, локализованные сообщения).
 *
 * <p><b>Особенности:</b>
 * <ul>
 *   <li>Автоматически добавляет timestamp в момент создания ошибки</li>
 *   <li>Извлекает traceId из MDC для отслеживания запроса</li>
 *   <li>Локализует сообщения через {@link MessageResolver}</li>
 *   <li>Поддерживает кастомные сообщения поверх стандартных</li>
 * </ul>
 *
 * <p><b>Примеры использования:</b>
 * <pre>{@code
 * // В GlobalExceptionHandler
 * var error = errorFactory.create(ErrorCode.VALIDATION_ERROR, details);
 * var errorWithCustomMessage = errorFactory.create(ErrorCode.NOT_FOUND, null, "User not found");
 * var fieldError = errorFactory.createFieldError(ErrorCode.REQUIRED_FIELD, "email", null, null, null);
 * }</pre>
 *
 * @see ApiError
 * @see FieldError
 * @see MessageResolver
 * @see GlobalExceptionHandler
 */
@Component
@RequiredArgsConstructor
public class ApiErrorFactory {

    private final MessageResolver messageResolver;

    /**
     * Создаёт {@link ApiError} без деталей и кастомного сообщения.
     *
     * <p><b>Пример:</b>
     * <pre>{@code
     * ApiError error = errorFactory.create(ErrorCode.INTERNAL_ERROR);
     * // Результат: timestamp=now, message="Internal server error", code="INTERNAL_ERROR"
     * }</pre>
     *
     * @param code код ошибки {@link ErrorCode}
     * @return {@link ApiError} с заполненными timestamp, traceId и локализованным сообщением
     */
    public ApiError create(final ErrorCode code) {
        return create(code, null, null);
    }

    /**
     * Создаёт {@link ApiError} с деталями ошибок по полям.
     *
     * <p><b>Пример:</b>
     * <pre>{@code
     * var details = List.of(
     *     errorFactory.createFieldError(ErrorCode.REQUIRED_FIELD, "name", null, null, null),
     *     errorFactory.createFieldError(ErrorCode.INVALID_EMAIL, "email", "invalid", null, null)
     * );
     * ApiError error = errorFactory.create(ErrorCode.VALIDATION_ERROR, details);
     * }</pre>
     *
     * @param code    код ошибки {@link ErrorCode}
     * @param details список деталей ошибок по полям (может быть {@code null})
     * @return {@link ApiError} с заполненными timestamp, traceId, деталями и локализованным сообщением
     */
    public ApiError create(final ErrorCode code, final List<FieldError> details) {
        return create(code, details, null);
    }

    /**
     * Создаёт {@link ApiError} с деталями и кастомным сообщением.
     *
     * <p>Кастомное сообщение используется вместо стандартного локализованного сообщения
     * для указанного {@link ErrorCode}. Полезно для ситуаций, когда нужно добавить
     * дополнительный контекст (например, ID сущности).
     *
     * <p><b>Пример:</b>
     * <pre>{@code
     * ApiError error = errorFactory.create(
     *     ErrorCode.NOT_FOUND,
     *     null,
     *     "FFC with ID 12345 not found"
     * );
     * // message будет "FFC with ID 12345 not found", а не стандартное сообщение для NOT_FOUND
     * }</pre>
     *
     * <p><b>Важно:</b> Для кода {@link ErrorCode#INTERNAL_ERROR} кастомное сообщение
     * игнорируется — всегда возвращается "Internal server error" (безопасность).
     *
     * @param code          код ошибки {@link ErrorCode}
     * @param details       список деталей ошибок по полям (может быть {@code null})
     * @param customMessage кастомное сообщение (если {@code null} или пустое — используется стандартное)
     * @return {@link ApiError} с заполненными timestamp, traceId, деталями и сообщением
     */
    public ApiError create(final ErrorCode code, final List<FieldError> details, final String customMessage) {
        final String message = this.resolveMessage(code, customMessage);

        return ApiError.builder()
                .timestamp(Instant.now())
                .message(message)
                .code(code.code())
                .traceId(TraceIdUtils.getCurrentTraceId())
                .details(details)
                .build();
    }

    /**
     * Создаёт {@link FieldError} с локализованным сообщением.
     *
     * <p>Сообщение формируется через {@link MessageResolver}:
     * <ul>
     *   <li>Если указан {@code detailKey} — используется ключ с переданными аргументами</li>
     *   <li>Если {@code detailKey} равен {@code null} — используется стандартный ключ для {@link ErrorCode}</li>
     * </ul>
     *
     * <p><b>Примеры:</b>
     * <pre>{@code
     * // Стандартное сообщение для ErrorCode
     * errorFactory.createFieldError(ErrorCode.REQUIRED_FIELD, "email", null, null, null);
     * // message = "Field 'email' is required"
     *
     * // Кастомный ключ с аргументами
     * errorFactory.createFieldError(
     *     ErrorCode.INVALID_FORMAT,
     *     "age",
     *     "abc",
     *     "error.invalid_format.detail",
     *     new Object[]{"abc", "Integer"}
     * );
     * // message = "Failed to convert 'abc' to type Integer"
     * }</pre>
     *
     * @param code         код ошибки {@link ErrorCode} (используется как fallback для сообщения)
     * @param field        имя поля, в котором произошла ошибка
     * @param rejectedValue отклонённое значение (может быть {@code null})
     * @param detailKey    ключ для локализации сообщения (если {@code null} — используется стандартный для code)
     * @param args         аргументы для форматирования сообщения (если {@code null} — без аргументов)
     * @return {@link FieldError} с заполненными полем, сообщением, кодом и отклонённым значением
     */
    public FieldError createFieldError(final ErrorCode code, final String field,
                                       final Object rejectedValue, final String detailKey,
                                       final Object... args) {
        final String message = detailKey != null
                ? messageResolver.resolve(detailKey, args)
                : messageResolver.resolve(code);

        return FieldError.builder()
                .field(field)
                .message(message)
                .code(code.code())
                .rejectedValue(rejectedValue)
                .build();
    }

    /**
     * Создаёт {@link FieldError} с готовым сообщением.
     *
     * <p>Используется, когда сообщение уже сформировано программно или получено
     * из внешнего источника (например, из исключения).
     *
     * <p><b>Пример:</b>
     * <pre>{@code
     * // Сообщение из исключения
     * errorFactory.createFieldError(
     *     "user.email",
     *     exception.getMessage(),
     *     "VALIDATION_ERROR",
     *     exception.getValue()
     * );
     * }</pre>
     *
     * @param field         имя поля, в котором произошла ошибка
     * @param message       готовое сообщение об ошибке (уже локализованное)
     * @param code          код ошибки (строка, например "REQUIRED_FIELD")
     * @param rejectedValue отклонённое значение (может быть {@code null})
     * @return {@link FieldError} с заполненными полем, сообщением, кодом и отклонённым значением
     */
    public FieldError createFieldError(final String field, final String message, final String code, final Object rejectedValue) {
        return FieldError.builder()
                .field(field)
                .message(message)
                .code(code)
                .rejectedValue(rejectedValue)
                .build();
    }

    /**
     * Формирует сообщение для {@link ApiError}.
     *
     * <p><b>Правила выбора сообщения:</b>
     * <ol>
     *   <li>Для {@link ErrorCode#INTERNAL_ERROR} всегда возвращается "Internal server error" (безопасность)</li>
     *   <li>Иначе, если передан {@code customMessage} и он не пустой — используется он</li>
     *   <li>Иначе — локализованное сообщение через {@link MessageResolver#resolve(ErrorCode)}</li>
     * </ol>
     *
     * @param code          код ошибки
     * @param customMessage кастомное сообщение (может быть {@code null})
     * @return итоговое сообщение для ApiError
     */
    private String resolveMessage(final ErrorCode code, final String customMessage) {
        if (code == ErrorCode.INTERNAL_ERROR) {
            return "Internal server error";
        }
        if (customMessage != null && !customMessage.isBlank()) {
            return customMessage;
        }
        return messageResolver.resolve(code);
    }
}