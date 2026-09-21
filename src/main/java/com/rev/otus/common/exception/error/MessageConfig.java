package com.rev.otus.common.exception.error;

import lombok.NoArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Конфигурация интернационализации (i18n) сообщений об ошибках.
 *
 * <p><b>Назначение:</b> настраивает Spring {@link MessageSource} для поддержки
 * локализованных сообщений в {@link MessageResolver} и во всём приложении.
 *
 * <p><b>Что настраивается:</b>
 * <ul>
 *   <li><b>basename</b> — базовое имя файлов сообщений ({@code messages})</li>
 *   <li><b>encoding</b> — кодировка UTF-8 для поддержки кириллицы и спецсимволов</li>
 *   <li><b>useCodeAsDefaultMessage</b> — fallback на ключ сообщения при отсутствии перевода</li>
 * </ul>
 *
 * <p><b>Файлы локализации (в {@code src/main/resources}):</b>
 * <pre>
 * src/main/resources/
 *   ├── messages.properties          (по умолчанию, английский)
 *   ├── messages_en.properties       (английский явно)
 *   ├── messages_ru.properties       (русский)
 *   └── messages_xx.properties       (другие языки)
 * </pre>
 *
 * <p><b>Пример содержимого {@code messages.properties}:</b>
 * <pre>{@code
 * error.NOT_FOUND=Resource not found
 * error.VALIDATION_ERROR=Validation failed
 * error.INTERNAL_ERROR=Internal server error
 * error.invalid_format.detail=Failed to convert '{0}' to type {1}
 * error.missing_parameter.detail=Parameter '{0}' is required
 * }</pre>
 *
 * <p><b>Пример содержимого {@code messages_ru.properties}:</b>
 * <pre>{@code
 * error.NOT_FOUND=Ресурс не найден
 * error.VALIDATION_ERROR=Ошибка валидации
 * error.INTERNAL_ERROR=Внутренняя ошибка сервера
 * error.invalid_format.detail=Не удалось преобразовать '{0}' в тип {1}
 * error.missing_parameter.detail=Отсутствует обязательный параметр '{0}'
 * }</pre>
 *
 * <p><b>Как это работает:</b>
 * <ol>
 *   <li>Spring создаёт бин {@code messageSource} с указанными настройками</li>
 *   <li>{@link MessageResolver} использует этот бин через внедрение зависимости</li>
 *   <li>При вызове {@code messageResolver.resolve(ErrorCode.NOT_FOUND)} Spring автоматически
 *       выбирает файл в соответствии с локалью текущего запроса</li>
 *   <li>Локаль извлекается из {@code Accept-Language} заголовка HTTP-запроса</li>
 * </ol>
 *
 * <p><b>Логика выбора файла:</b>
 * <ul>
 *   <li>Если клиент отправил заголовок {@code Accept-Language: ru-RU} → используется {@code messages_ru.properties}</li>
 *   <li>Если клиент отправил {@code Accept-Language: en-US} → используется {@code messages_en.properties}</li>
 *   <li>Если подходящего файла нет или заголовок отсутствует → используется {@code messages.properties}</li>
 * </ul>
 *
 * <p><b>Пример запроса с локализацией:</b>
 * <pre>
 * GET /api/users/123
 * Accept-Language: ru-RU,ru;q=0.9,en;q=0.8
 * </pre>
 * Ответ будет содержать сообщения на русском языке.
 *
 * @see MessageSource
 * @see ResourceBundleMessageSource
 * @see MessageResolver
 * @see org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
 */
@Configuration
@NoArgsConstructor
public class MessageConfig {

    /**
     * Создаёт и настраивает бин {@link MessageSource} для локализации сообщений.
     *
     * <p><b>Конфигурация:</b>
     * <ul>
     *   <li><b>basename = "messages"</b> — ищет файлы {@code messages.properties},
     *       {@code messages_ru.properties} и т.д. в classpath</li>
     *   <li><b>encoding = "UTF-8"</b> — обеспечивает корректное отображение кириллицы
     *       и специальных символов</li>
     *   <li><b>useCodeAsDefaultMessage = true</b> — если сообщение не найдено,
     *       возвращается сам ключ (полезно для отладки)</li>
     * </ul>
     *
     * <p><b>Пример использования в коде:</b>
     * <pre>{@code
     * @Component
     * public class MessageResolver {
     *     private final MessageSource messageSource;
     *
     *     public String resolve(String key, Object[] args) {
     *         return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
     *     }
     * }
     * }</pre>
     *
     * @return настроенный {@link MessageSource} бин для внедрения в другие компоненты
     */
    @Bean
    public MessageSource messageSource() {
        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}