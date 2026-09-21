package com.rev.otus.common.exception.error;

import com.rev.otus.common.exception.GlobalExceptionHandler;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Коды ошибок API.
 *
 * <p><b>Назначение:</b> централизованное хранение всех возможных кодов ошибок API
 * с привязкой к HTTP статусам и ключам локализации сообщений.
 *
 * <p><b>Структура:</b>
 * <ul>
 *   <li>Каждый enum-константа имеет соответствующий HTTP статус</li>
 *   <li>Строковое представление = имя константы (например, "VALIDATION_ERROR")</li>
 *   <li>Ключ для локализации = "error." + имя константы</li>
 * </ul>
 *
 * <p><b>Категории ошибок и HTTP статусы:</b>
 * <table border="1">
 *   <caption>Соответствие кодов ошибок HTTP статусам</caption>
 *   <tr><th>Категория</th><th>Коды ошибок</th><th>HTTP статус</th></tr>
 *   <tr><td rowspan="7">Ошибки клиента (400)</td>
 *       <td>VALIDATION_ERROR, TYPE_MISMATCH, MISSING_PARAMETER,<br>
 *           INVALID_FORMAT, INVALID_TYPE, INVALID_VALUE, MALFORMED_JSON</td>
 *       <td>400 Bad Request</td>
 *    </tr>
 *    <tr><td>NOT_FOUND</td><td>404 Not Found</td></tr>
 *    <tr><td>METHOD_NOT_ALLOWED</td><td>405 Method Not Allowed</td></tr>
 *    <tr><td>UNSUPPORTED_MEDIA_TYPE</td><td>415 Unsupported Media Type</td></tr>
 *    <tr><td>BUSINESS_RULE_VIOLATION</td><td>409 Conflict</td></tr>
 *    <tr><td>INTERNAL_ERROR</td><td>500 Internal Server Error</td></tr>
 * </table>
 *
 * <p><b>Примеры использования:</b>
 * <pre>{@code
 * // В GlobalExceptionHandler
 * var status = ErrorCode.VALIDATION_ERROR.getStatus();  // HttpStatus.BAD_REQUEST
 * var code = ErrorCode.NOT_FOUND.code();                // "NOT_FOUND"
 * var messageKey = ErrorCode.INTERNAL_ERROR.messageKey(); // "error.INTERNAL_ERROR"
 * }</pre>
 *
 * @see GlobalExceptionHandler
 * @see ApiErrorFactory
 * @see HttpStatus
 */
@Getter
public enum ErrorCode {

    // ==================== 400 Bad Request ====================

    /** Неверный формат данных при парсинге JSON (например, "abc" → LocalDate). */
    INVALID_FORMAT(HttpStatus.BAD_REQUEST),

    /** Неверный тип данных в JSON (массив вместо объекта, null в обязательном поле). */
    INVALID_TYPE(HttpStatus.BAD_REQUEST),

    /** Ошибка создания DTO при десериализации (ошибка конструктора, проблемы с enum). */
    INVALID_VALUE(HttpStatus.BAD_REQUEST),

    /** Синтаксическая ошибка JSON (незакрытые скобки, лишние запятые и т.д.). */
    MALFORMED_JSON(HttpStatus.BAD_REQUEST),

    /** Отсутствие обязательного параметра запроса (@RequestParam(required = true)). */
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST),

    /** Неверный тип параметра запроса (например, "abc" → Long). */
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST),

    /** Ошибки валидации тела запроса (@Valid, @NotNull, @Size и т.д.). */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),

    // ==================== 404 Not Found ====================

    /** Ресурс не найден (URL не существует или сущность не найдена). */
    NOT_FOUND(HttpStatus.NOT_FOUND),

    // ==================== 405 Method Not Allowed ====================

    /** HTTP метод не поддерживается для данного эндпоинта (POST вместо GET). */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),

    // ==================== 415 Unsupported Media Type ====================

    /** Content-Type не поддерживается (ожидался application/json, пришёл text/plain). */
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    // ==================== 409 Conflict ====================

    /** Нарушение бизнес-правил (конфликт данных, дубликат, некорректное состояние). */
    BUSINESS_RULE_VIOLATION(HttpStatus.CONFLICT),

    /** Дубликат записи (нарушение уникальности, например, name уже существует). */
    DUPLICATE_ENTITY(HttpStatus.CONFLICT),

    /** Нарушение ссылочной целостности (сущность имеет связанные записи и не может быть изменена/удалена). */
    REFERENCED_ENTITY_EXISTS(HttpStatus.CONFLICT),

    // ==================== 500 Internal Server Error ====================

    /** Необработанная внутренняя ошибка сервера (fallback для всех непредвиденных исключений). */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(final HttpStatus status) {
        this.status = status;
    }

    /**
     * Возвращает строковый код ошибки.
     *
     * <p>Используется в {@link ApiError#code()} для программной обработки на клиенте.
     *
     * <p><b>Пример:</b>
     * <pre>{@code
     * ErrorCode.VALIDATION_ERROR.code() // "VALIDATION_ERROR"
     * }</pre>
     *
     * @return имя константы enum (например, "VALIDATION_ERROR")
     */
    public String code() {
        return name();
    }

    /**
     * Возвращает ключ для локализации сообщения об ошибке.
     *
     * <p>Ключ формируется по шаблону: {@code "error." + имя_константы}.
     * Соответствующие сообщения должны быть определены в файлах локализации
     * (например, {@code messages.properties}, {@code messages_ru_RU.properties}).
     *
     * <p><b>Примеры ключей и сообщений:</b>
     * <pre>{@code
     * error.VALIDATION_ERROR=Validation failed
     * error.NOT_FOUND=Resource not found
     * error.INTERNAL_ERROR=Internal server error
     * }</pre>
     *
     * <p><b>Использование:</b>
     * <pre>{@code
     * String key = ErrorCode.NOT_FOUND.messageKey(); // "error.NOT_FOUND"
     * String message = messageResolver.resolve(key); // локализованное сообщение
     * }</pre>
     *
     * @return ключ для {@link MessageResolver} (например, "error.NOT_FOUND")
     * @see MessageResolver#resolve(ErrorCode)
     */
    public String messageKey() {
        return "error." + name();
    }
}