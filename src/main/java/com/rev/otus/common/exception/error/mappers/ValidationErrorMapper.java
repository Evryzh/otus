package com.rev.otus.common.exception.error.mappers;

import com.rev.otus.common.exception.GlobalExceptionHandler;
import com.rev.otus.common.exception.error.ApiError.FieldError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

/**
 * Маппинг ошибок валидации в {@link FieldError}.
 *
 * <p><b>Назначение:</b> преобразует исключения валидации Spring и Bean Validation
 * в стандартизированные {@link FieldError} для детализации ответа API.
 *
 * <p><b>Обрабатываемые исключения:</b>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} — ошибки валидации {@code @RequestBody}</li>
 *   <li>{@link ConstraintViolationException} — ошибки валидации параметров метода и {@code @PathVariable}</li>
 * </ul>
 *
 * <p><b>Особенности маппинга:</b>
 * <ul>
 *   <li>Аннотации валидации маппятся в стабильные коды ошибок:
 *       {@code @NotNull} → {@code REQUIRED_FIELD}, {@code @Size} → {@code INVALID_SIZE} и т.д.</li>
 *   <li>XSS-атаки (аннотация {@code @NoXSS}) помечаются маркером {@code [INJECTED]}</li>
 *   <li>Длинные строки обрезаются</li>
 * </ul>
 *
 * @see MethodArgumentNotValidException
 * @see ConstraintViolationException
 * @see GlobalExceptionHandler#handleValidation
 * @see GlobalExceptionHandler#handleConstraintViolation
 */
@Component
@NoArgsConstructor
public class ValidationErrorMapper {

    private static final String INJECTED_MARKER = "[INJECTED]";
    private static final int MAX_LOG_LENGTH = 200;

    /**
     * Маппинг ошибки валидации тела запроса.
     *
     * <p>Обрабатывает {@link MethodArgumentNotValidException}, которая возникает при
     * нарушении аннотаций валидации в объекте, помеченном {@code @Valid @RequestBody}.
     *
     * <p><b>Пример:</b>
     * <pre>{@code
     * // DTO
     * public class UserDto {
     *     @NotBlank(message = "Name is required")
     *     private String name;
     *
     *     @Email
     *     private String email;
     * }
     *
     * // Контроллер
     * @PostMapping("/users")
     * public User create(@Valid @RequestBody UserDto dto) { ... }
     *
     * // Запрос с ошибками: {"name": "", "email": "invalid"}
     * // Результат: два FieldError с кодами REQUIRED_FIELD и INVALID_EMAIL
     * }</pre>
     *
     * @param exception исключение с {@link org.springframework.validation.BindingResult}
     * @return список {@link FieldError} с деталями всех нарушенных полей
     */
    public List<FieldError> mapFieldErrors(final MethodArgumentNotValidException exception) {
        return exception.getBindingResult().getFieldErrors().stream().map(this::mapFieldError).toList();
    }

    /**
     * Маппинг ошибки валидации параметров метода и возвращаемых значений.
     *
     * <p>Обрабатывает {@link ConstraintViolationException}, которая возникает при:
     * <ul>
     *   <li>Валидации параметров метода (с аннотацией {@code @Validated} на классе)</li>
     *   <li>Валидации {@code @PathVariable} и {@code @RequestParam}</li>
     *   <li>Валидации возвращаемого значения</li>
     * </ul>
     *
     * <p><b>Пример:</b>
     * <pre>{@code
     * // Контроллер с @Validated
     * @RestController
     * @Validated
     * public class UserController {
     *
     *     @GetMapping("/users/{id}")
     *     public User getById(@Positive @PathVariable Long id) { ... }
     *
     *     @GetMapping("/search")
     *     public List<User> search(@Size(min = 3) @RequestParam String query) { ... }
     * }
     *
     * // Запрос: GET /users/-1
     * // Результат: FieldError с полем "id" и кодом OUT_OF_RANGE
     * }</pre>
     *
     * @param exception исключение с набором {@link ConstraintViolation}
     * @return список {@link FieldError} с деталями всех нарушений
     */
    public List<FieldError> mapFieldErrors(final ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream().map(this::mapFieldError).toList();
    }

    // ==================== Одиночные ошибки ====================

    /**
     * Маппинг одного Spring {@link org.springframework.validation.FieldError}.
     *
     * <p>Извлекает имя поля, сообщение об ошибке и отклонённое значение.
     *
     * @param error ошибка поля из {@link org.springframework.validation.BindingResult}
     * @return {@link FieldError} со стабильным кодом, сообщением и безопасным значением
     */
    private FieldError mapFieldError(final org.springframework.validation.FieldError error) {
        final String code = this.mapCode(error.getCode());

        return FieldError.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .code(code)
                .rejectedValue(safeValue(error.getRejectedValue()))
                .build();
    }

    /**
     * Маппинг одного {@link ConstraintViolation}.
     *
     * <p>Извлекает имя поля из property path (например, {@code getById.id} → {@code "id"}).
     *
     * <p><b>Примеры извлечения поля:</b>
     * <ul>
     *   <li>{@code "createUser.name"} → {@code "name"}</li>
     *   <li>{@code "getById.id"} → {@code "id"}</li>
     *   <li>{@code "search.query"} → {@code "query"}</li>
     * </ul>
     *
     * @param violation нарушение Bean Validation
     * @return {@link FieldError} со стабильным кодом, сообщением и безопасным значением
     */
    private FieldError mapFieldError(final ConstraintViolation<?> violation) {
        final String path = violation.getPropertyPath().toString();
        final int lastDot = path.lastIndexOf('.');
        final String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;

        final String annotation = violation.getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();
        final String code = this.mapCode(annotation);
        final boolean isXss = "NoXSS".equals(annotation);

        return FieldError.builder()
                .field(field)
                .message(violation.getMessage())
                .code(code)
                .rejectedValue(isXss ? INJECTED_MARKER : safeValue(violation.getInvalidValue()))
                .build();
    }

    // ==================== Private helpers ====================

    /**
     * Маппинг аннотации валидации в стабильный код ошибки.
     *
     * <p><b>Таблица маппинга:</b>
     * <ul>
     *   <li>{@code NotNull}, {@code NotBlank}, {@code NotEmpty} → {@code REQUIRED_FIELD}</li>
     *   <li>{@code Size} → {@code INVALID_SIZE}</li>
     *   <li>{@code Pattern} → {@code INVALID_FORMAT}</li>
     *   <li>{@code Min}, {@code Max}, {@code Positive}, {@code PositiveOrZero},
     *   {@code Negative}, {@code NegativeOrZero} → {@code OUT_OF_RANGE}</li>
     *   <li>{@code Email} → {@code INVALID_EMAIL}</li>
     *   <li>{@code NoXSS} → {@code MALICIOUS_CONTENT}</li>
     *   <li>Неизвестные → {@code VALIDATION_FAILED}</li>
     * </ul>
     *
     * @param annotationType имя аннотации валидации (например, "NotNull", "Size")
     * @return стабильный код ошибки для API
     */
    private String mapCode(final String annotationType) {
        if (annotationType == null) {
            return "VALIDATION_FAILED";
        }
        return switch (annotationType) {
            case "NotNull", "NotBlank", "NotEmpty" -> "REQUIRED_FIELD";
            case "Size" -> "INVALID_SIZE";
            case "Pattern" -> "INVALID_FORMAT";
            case "Min", "Max", "Positive", "PositiveOrZero", "Negative", "NegativeOrZero" -> "OUT_OF_RANGE";
            case "Email" -> "INVALID_EMAIL";
            case "NoXSS" -> "MALICIOUS_CONTENT";
            default -> "VALIDATION_FAILED";
        };
    }

    /**
     * Безопасное преобразование отклонённого значения.
     *
     * <p><b>Правила обработки:</b>
     * <ul>
     *   <li>Строки → обрезаются</li>
     *   <li>Числа и булевы значения → возвращаются как есть</li>
     *   <li>Остальные типы → {@code null}</li>
     * </ul>
     *
     * @param value отклонённое значение (может быть {@code null})
     * @return безопасное представление значения
     */
    private Object safeValue(final Object value) {
        if (value instanceof String str) {
            return truncate(str);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return null;
    }

    /**
     * Обрезает строку до максимальной длины для логирования.
     *
     * <p>Если строка длиннее {@value #MAX_LOG_LENGTH} символов, она обрезается
     * и в конец добавляется "...".
     *
     * @param value исходная строка (может быть {@code null})
     * @return обрезанная строка, исходная строка (если она короче лимита)
     *         или {@code null}, если входное значение было {@code null}
     */
    public static String truncate(final String value) {
        if (value == null || value.length() <= MAX_LOG_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_LOG_LENGTH - 3) + "...";
    }
}
