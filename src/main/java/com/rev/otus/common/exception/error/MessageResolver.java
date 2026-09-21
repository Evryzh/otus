package com.rev.otus.common.exception.error;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Локализация сообщений об ошибках.
 *
 * <p><b>Назначение:</b> централизованное получение локализованных сообщений
 * для кодов ошибок API и произвольных ключей с поддержкой параметров.
 *
 * <p><b>Особенности:</b>
 * <ul>
 *   <li>Автоматически использует локаль текущего запроса из {@link LocaleContextHolder}</li>
 *   <li>Для {@link ErrorCode#INTERNAL_ERROR} всегда возвращает английское сообщение (безопасность)</li>
 *   <li>Поддерживает параметризованные сообщения (например, "Field {0} is required")</li>
 *   <li>При отсутствии сообщения для ключа возвращает fallback-значение с маркером "???"</li>
 * </ul>
 *
 * <p><b>Примеры использования:</b>
 * <pre>{@code
 * // Получение сообщения по ErrorCode
 * String message = messageResolver.resolve(ErrorCode.NOT_FOUND);
 * // Результат: локализованное сообщение из файла, например "Ресурс не найден"
 *
 * // Получение параметризованного сообщения
 * String message = messageResolver.resolve("error.invalid_format.detail", new Object[]{"abc", "Integer"});
 * // Результат: "Не удалось преобразовать 'abc' в тип Integer"
 * }</pre>
 *
 * <p><b>Файлы локализации (messages.properties):</b>
 * <pre>{@code
 * error.NOT_FOUND=Resource not found
 * error.VALIDATION_ERROR=Validation failed
 * error.invalid_format.detail=Failed to convert '{0}' to type {1}
 * }</pre>
 *
 * @see MessageSource
 * @see ErrorCode#messageKey()
 * @see LocaleContextHolder
 */
@Component
@RequiredArgsConstructor
public class MessageResolver {

    private final MessageSource messageSource;

    /**
     * Возвращает локализованное сообщение для кода ошибки.
     *
     * <p>Сообщение извлекается по ключу {@link ErrorCode#messageKey()}
     * с использованием локали текущего запроса.
     *
     * <p><b>Пример:</b>
     * <pre>{@code
     * // Для ErrorCode.NOT_FOUND и локали ru_RU
     * String message = messageResolver.resolve(ErrorCode.NOT_FOUND);
     * // Результат: "Ресурс не найден" (из messages_ru_RU.properties)
     * }</pre>
     *
     * <p><b>Особенности:</b>
     * <ul>
     *   <li>Для {@link ErrorCode#INTERNAL_ERROR} всегда возвращается "Internal server error"
     *       без попытки локализации (безопасность — не раскрываем детали)</li>
     *   <li>Если сообщение не найдено, возвращается {@link ErrorCode#code()} как fallback</li>
     * </ul>
     *
     * @param code код ошибки {@link ErrorCode}
     * @return локализованное сообщение для указанного кода ошибки
     */
    public String resolve(final ErrorCode code) {
        if (code == ErrorCode.INTERNAL_ERROR) {
            return "Internal server error";
        }
        return messageSource.getMessage(
                code.messageKey(),
                null,
                code.code(),
                LocaleContextHolder.getLocale()
        );
    }

    /**
     * Возвращает локализованное сообщение по ключу с аргументами.
     *
     * <p>Используется для параметризованных сообщений, где значения подставляются
     * вместо плейсхолдеров {@code {0}}, {@code {1}} и т.д.
     *
     * <p><b>Примеры:</b>
     * <pre>{@code
     * // Ключ: error.invalid_format.detail=Failed to convert '{0}' to type {1}
     * String message = messageResolver.resolve(
     *     "error.invalid_format.detail",
     *     new Object[]{"abc", "Integer"}
     * );
     * // Результат: "Failed to convert 'abc' to type Integer"
     *
     * // Ключ: error.missing_parameter.detail=Parameter '{0}' is required
     * String message = messageResolver.resolve(
     *     "error.missing_parameter.detail",
     *     new Object[]{"id"}
     * );
     * // Результат: "Parameter 'id' is required"
     * }</pre>
     *
     * <p><b>Fallback:</b>
     * Если сообщение для указанного ключа не найдено, возвращается строка
     * {@code "???key???"} (например, "???unknown.key???"), что помогает
     * быстро обнаружить отсутствующие ключи при разработке.
     *
     * @param key  ключ сообщения в файлах локализации (например, "error.invalid_format.detail")
     * @param args аргументы для подстановки в сообщение (может быть {@code null})
     * @return локализованное параметризованное сообщение или fallback при отсутствии ключа
     */
    public String resolve(final String key, final Object... args) {
        return messageSource.getMessage(
                key,
                args,
                "???" + key + "???",
                LocaleContextHolder.getLocale()
        );
    }
}