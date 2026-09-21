package com.rev.otus.common.exception;

import com.rev.otus.common.exception.error.ErrorCode;
import lombok.Getter;

import java.io.Serial;

/**
 * Базовое исключение для бизнес-ошибок.
 *
 * <p><b>Когда использовать:</b>
 * <ul>
 *   <li>Нарушение бизнес-правил (значение в БД не найдено, дубликат записи и т.п.)</li>
 *   <li>Ошибки валидации на уровне бизнес-логики</li>
 *   <li>Конфликты данных (оптимистичная блокировка)</li>
 *   <li>Любые проверки в сервисном слое</li>
 * </ul>
 *
 * <p><b>Когда НЕ использовать:</b>
 * <ul>
 *   <li>Технические ошибки (SQLException, IOException) — пусть летят как RuntimeException</li>
 *   <li>Ошибки валидации входных данных — для этого есть Bean Validation</li>
 *   <li>Ошибки аутентификации/авторизации — для этого Spring Security</li>
 * </ul>
 *
 * <p><b>Примеры использования:</b>
 * <pre>{@code
 * // Простая ошибка
 * throw new BusinessException(ErrorCode.NOT_FOUND);
 *
 * // Ошибка с указанием проблемного поля
 * throw new BusinessException(
 *     ErrorCode.BUSINESS_RULE_VIOLATION,
 *     "ffcId",                          // поле
 *     request.ffcId()                   // значение
 * );
 *
 * // Ошибка с кастомным сообщением (без локализации)
 * throw new BusinessException(
 *     ErrorCode.NOT_FOUND,
 *     null,
 *     null,
 *     "FFC with ID 12345 not found"
 * );
 *
 * // Ошибка с локализованным сообщением
 * throw new BusinessException(
 *     ErrorCode.NOT_FOUND,
 *     null,
 *     null,
 *     "error.cur.not_found",           // ключ
 *     objId, parId                     // аргументы
 * );
 * }</pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final String field;
    private final Object rejectedValue;
    private final String userMessage;
    private final String messageKey;
    private final Object[] messageArgs;

    /**
     * Создаёт бизнес-исключение только с кодом ошибки.
     *
     * @param errorCode код ошибки
     */
    public BusinessException(final ErrorCode errorCode) {
        this(errorCode, null, null, null);
    }

    /**
     * Создаёт бизнес-исключение с указанием проблемного поля и отклонённого значения.
     *
     * @param errorCode     код ошибки
     * @param field         имя поля
     * @param rejectedValue отклонённое значение
     */
    public BusinessException(final ErrorCode errorCode, final String field, final Object rejectedValue) {
        this(errorCode, field, rejectedValue, null);
    }

    /**
     * Создаёт бизнес-исключение с кастомным сообщением.
     *
     * @param errorCode   код ошибки
     * @param field       имя поля (может быть {@code null})
     * @param rejectedValue отклонённое значение (может быть {@code null})
     * @param userMessage кастомное сообщение для пользователя (может быть {@code null})
     */
    public BusinessException(final ErrorCode errorCode, final String field,
                             final Object rejectedValue, final String userMessage) {
        this(errorCode, field, rejectedValue, userMessage, null, new Object[0]);
    }

    /**
     * Создаёт бизнес-исключение с локализованным сообщением.
     *
     * @param errorCode   код ошибки
     * @param field       имя поля (может быть {@code null})
     * @param rejectedValue отклонённое значение (может быть {@code null})
     * @param messageKey  ключ для локализации сообщения
     * @param messageArgs аргументы для локализации
     */
    public BusinessException(final ErrorCode errorCode, final String field,
                             final Object rejectedValue, final String messageKey, final Object... messageArgs) {
        this(errorCode, field, rejectedValue, null, messageKey, messageArgs);
    }

    /**
     * Создаёт бизнес-исключение с полным набором параметров.
     *
     * @param errorCode     код ошибки
     * @param field         имя поля (может быть {@code null})
     * @param rejectedValue отклонённое значение (может быть {@code null})
     * @param userMessage   кастомное сообщение для пользователя (может быть {@code null})
     * @param messageKey  ключ для локализации сообщения
     * @param messageArgs аргументы для локализации
     */
    public BusinessException(final ErrorCode errorCode, final String field,
                             final Object rejectedValue, final String userMessage,
                             final String messageKey, final Object... messageArgs) {
        super(userMessage != null ? userMessage : messageKey != null ? messageKey : errorCode.code());
        this.errorCode = errorCode;
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.userMessage = userMessage;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs != null ? messageArgs.clone() : new Object[0];
    }
}
