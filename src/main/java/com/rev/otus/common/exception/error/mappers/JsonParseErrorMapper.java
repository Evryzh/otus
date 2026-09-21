package com.rev.otus.common.exception.error.mappers;

import com.rev.otus.common.exception.error.ApiError.FieldError;
import com.rev.otus.common.exception.error.ErrorCode;
import com.rev.otus.common.exception.error.MessageResolver;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.internal.annotation.SuppressFBWarnings;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.ValueInstantiationException;

import java.util.List;

/**
 * Маппинг ошибок парсинга JSON в теле запроса {@link HttpMessageNotReadableException}.
 *
 * <p><b>Назначение:</b> преобразует исключения чтения HTTP-сообщения в стандартизированный
 * результат {@link JsonParseResult}, содержащий код ошибки и детали.
 *
 * <p><b>Поддерживаемые типы исключений (cause):</b>
 * <ul>
 *   <li>{@link InvalidFormatException} — неверный формат данных (например, "abc" вместо LocalDate)</li>
 *   <li>{@link MismatchedInputException} — неверный тип (массив вместо объекта, null вместо значения)</li>
 *   <li>{@link ValueInstantiationException} — ошибка создания DTO (например, null в @NotNull поле)</li>
 *   <li>Другие — общая синтаксическая ошибка JSON</li>
 * </ul>
 *
 * @see HttpMessageNotReadableException
 * @see InvalidFormatException
 * @see MismatchedInputException
 * @see ValueInstantiationException
 */
@Component
@RequiredArgsConstructor
public class JsonParseErrorMapper {

    private final MessageResolver messageResolver;

    /**
     * Результат маппинга ошибки парсинга JSON.
     *
     * @param errorCode код ошибки
     * @param details   детали ошибки (список с одним элементом)
     */
    public record JsonParseResult(
            ErrorCode errorCode,
            @SuppressFBWarnings("EI_EXPOSE_REP")
            List<FieldError> details
    ) {
        /**
         * Компактный конструктор для обеспечения иммутабельности списка {@code details}.
         *
         * @param details исходный список, на основе которого создается иммутабельная копия
         */
        public JsonParseResult {
            if (details != null) {
                details = List.copyOf(details);
            }
        }
    }

    /**
     * Маппинг ошибки парсинга JSON в результат с кодом ошибки и деталями.
     *
     * <p><b>Алгоритм:</b>
     * <ol>
     *   <li>Извлекает cause из {@link HttpMessageNotReadableException}</li>
     *   <li>Определяет тип Jackson-исключения</li>
     *   <li>Извлекает информацию о поле и отклонённом значении</li>
     *   <li>Подбирает соответствующий {@link ErrorCode}</li>
     *   <li>Формирует локализованное сообщение об ошибке</li>
     * </ol>
     *
     * @param exception исключение {@link HttpMessageNotReadableException}
     * @return {@link JsonParseResult} с кодом ошибки и деталями
     */
    public JsonParseResult mapFieldErrors(final HttpMessageNotReadableException exception) {
        final Throwable cause = exception.getCause();

        if (cause instanceof InvalidFormatException formatException) {
            return mapInvalidFormatException(formatException);
        }
        if (cause instanceof MismatchedInputException inputException) {
            return mapMismatchedInputException(inputException);
        }
        if (cause instanceof ValueInstantiationException valueException) {
            return mapValueInstantiationException(valueException);
        }
        return mapGenericJsonError(exception, cause);
    }

    /**
     * Маппинг {@link InvalidFormatException} (неверный формат данных).
     *
     * <p><b>Примеры:</b>
     * <ul>
     *   <li>"2023-13-45" → LocalDate (несуществующая дата)</li>
     *   <li>"abc" → Integer (строка вместо числа)</li>
     *   <li>"truee" → Boolean (невалидный boolean)</li>
     * </ul>
     *
     * @param exception исключение Jackson о неверном формате
     * @return {@link JsonParseResult} с кодом {@code INVALID_FORMAT}
     */
    private JsonParseResult mapInvalidFormatException(final InvalidFormatException exception) {
        final String field = extractFieldName(exception);
        final Object rejectedValue = exception.getValue();
        final Class<?> targetType = exception.getTargetType();

        final FieldError detail = FieldError.builder()
                .field(field)
                .message(messageResolver.resolve("error.detail.invalid_format",
                        rejectedValue, targetType != null ? targetType.getSimpleName() : "unknown"))
                .code(ErrorCode.INVALID_FORMAT.code())
                .rejectedValue(safeValue(rejectedValue))
                .build();

        return new JsonParseResult(ErrorCode.INVALID_FORMAT, List.of(detail));
    }

    /**
     * Маппинг {@link MismatchedInputException} (неверный тип данных).
     *
     * <p><b>Примеры:</b>
     * <ul>
     *   <li>Массив вместо объекта: {@code []} вместо {@code {}}</li>
     *   <li>Объект вместо массива: {@code {}} вместо {@code []}</li>
     *   <li>Отсутствие обязательного поля: {@code null} в поле с аннотацией {@code @NotNull}</li>
     * </ul>
     *
     * @param exception исключение Jackson о несоответствии типов
     * @return {@link JsonParseResult} с кодом {@code INVALID_TYPE}
     */
    private JsonParseResult mapMismatchedInputException(final MismatchedInputException exception) {
        final String field = extractFieldName(exception);

        final FieldError detail = FieldError.builder()
                .field(field)
                .message(messageResolver.resolve("error.detail.invalid_type",
                        field != null ? field : "request body"))
                .code(ErrorCode.INVALID_TYPE.code())
                .build();

        return new JsonParseResult(ErrorCode.INVALID_TYPE, List.of(detail));
    }

    /**
     * Маппинг {@link ValueInstantiationException} (ошибка создания DTO).
     *
     * <p><b>Примеры:</b>
     * <ul>
     *   <li>Ошибка в конструкторе DTO</li>
     *   <li>Проблемы с десериализацией вложенных объектов</li>
     *   <li>Отсутствие дефолтного конструктора</li>
     *   <li>Некорректное значение перечисления (enum)</li>
     * </ul>
     *
     * @param exception исключение Jackson об ошибке создания объекта
     * @return {@link JsonParseResult} с кодом {@code INVALID_VALUE}
     */
    private JsonParseResult mapValueInstantiationException(final ValueInstantiationException exception) {
        final String field = extractFieldName(exception);

        final FieldError detail = FieldError.builder()
                .field(field)
                .message(messageResolver.resolve("error.detail.invalid_value",
                        field != null ? field : "request body"))
                .code(ErrorCode.INVALID_VALUE.code())
                .build();

        return new JsonParseResult(ErrorCode.INVALID_VALUE, List.of(detail));
    }

    /**
     * Маппинг прочих ошибок JSON (синтаксические ошибки).
     *
     * <p><b>Примеры:</b>
     * <ul>
     *   <li>Незакрытые фигурные скобки: {@code {"name": "John"}}</li>
     *   <li>Лишние запятые: {@code {"name": "John",}}</li>
     *   <li>Неверный синтаксис JSON: {@code {name: "John"}} (отсутствуют кавычки)</li>
     *   <li>Пустое тело запроса</li>
     *   <li>Некорректный escape-символ: {@code {"path": "C:\temp"}}</li>
     * </ul>
     *
     * @param exception исходное исключение {@link HttpMessageNotReadableException}
     * @param cause    причина исключения (может быть {@code null})
     * @return {@link JsonParseResult} с кодом {@code MALFORMED_JSON}
     */
    private JsonParseResult mapGenericJsonError(final HttpMessageNotReadableException exception, final Throwable cause) {
        String message = cause != null ? cause.getMessage() : exception.getMessage();
        if (message == null) {
            message = "Malformed JSON or invalid request body";
        }

        final FieldError detail = FieldError.builder()
                .field(null)
                .message(messageResolver.resolve("error.detail.malformed_json", message))
                .code(ErrorCode.MALFORMED_JSON.code())
                .rejectedValue(message)
                .build();

        return new JsonParseResult(ErrorCode.MALFORMED_JSON, List.of(detail));
    }

    /**
     * Извлекает имя поля из Jackson исключения.
     *
     * <p><b>Алгоритм:</b>
     * <ol>
     *   <li>Получает путь к месту ошибки через {@link JacksonException#getPath()}</li>
     *   <li>Берёт последний сегмент пути (наиболее глубокое место ошибки)</li>
     *   <li>Получает имя свойства непосредственно из сегмента пути</li>
     * </ol>
     *
     * <p><b>Примеры работы:</b>
     * <ul>
     *   <li>{@code "com.example.UserDto.email"} → {@code "email"}</li>
     *   <li>{@code "com.example.UserDto.users[0].name"} → {@code "name"}</li>
     *   <li>{@code "com.example.UserDto.users[0][1]"} → {@code "1"} (индекс массива)</li>
     *   <li>{@code "com.example.UserDto.attributes['age']"} → {@code "age"} (ключ Map)</li>
     *   <li>Пустой путь → {@code null}</li>
     * </ul>
     *
     * @param exception исключение Jackson (например, {@link InvalidFormatException})
     * @return имя поля или {@code null}, если поле не определено
     */
    private String extractFieldName(final JacksonException exception) {
        final List<JacksonException.Reference> path = exception.getPath();
        if (path == null || path.isEmpty()) {
            return null;
        }

        final JacksonException.Reference lastRef = path.getLast();
        final String propertyName = lastRef.getPropertyName();
        if (propertyName != null && !propertyName.isBlank()) {
            return propertyName;
        }
        return lastRef.getIndex() >= 0 ? Integer.toString(lastRef.getIndex()) : null;
    }

    /**
     * Безопасное преобразование отклонённого значения для логирования и вывода в ответе.
     *
     * <p><b>Правила обработки:</b>
     * <ul>
     *   <li>{@code null} → {@code null}</li>
     *   <li>Строки длиннее 100 символов → обрезаются с добавлением "..."</li>
     *   <li>Числа и булевы значения → возвращаются как есть</li>
     *   <li>Сложные объекты → возвращается имя класса</li>
     * </ul>
     *
     * <p><b>Примеры:</b>
     * <ul>
     *   <li>{@code "very long string... (100+ chars)"} → {@code "very long string..."}</li>
     *   <li>{@code 12345} → {@code 12345}</li>
     *   <li>{@code new UserDto()} → {@code "UserDto"}</li>
     * </ul>
     *
     * @param value отклонённое значение
     * @return безопасное представление значения
     */
    private Object safeValue(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str.length() > 100 ? str.substring(0, 100) + "..." : str;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return value.getClass().getSimpleName();
    }
}
