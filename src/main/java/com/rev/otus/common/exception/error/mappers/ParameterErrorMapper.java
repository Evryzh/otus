package com.rev.otus.common.exception.error.mappers;

import com.rev.otus.common.exception.error.ApiError.FieldError;
import com.rev.otus.common.exception.error.ErrorCode;
import com.rev.otus.common.exception.error.MessageResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Optional;

/**
 * Маппинг ошибок параметров запроса.
 *
 * <p><b>Назначение:</b> преобразует исключения, связанные с параметрами HTTP-запроса,
 * в стандартизированные {@link FieldError} для детализации ответа API.
 *
 * <p><b>Обрабатываемые исключения:</b>
 * <ul>
 *   <li>{@link MissingServletRequestParameterException} — отсутствие обязательного параметра</li>
 *   <li>{@link MethodArgumentTypeMismatchException} — неверный тип параметра</li>
 * </ul>
 *
 * @see MissingServletRequestParameterException
 * @see MethodArgumentTypeMismatchException
 */
@Component
@RequiredArgsConstructor
public class ParameterErrorMapper {

    private final MessageResolver messageResolver;

    /**
     * Маппинг ошибки отсутствия обязательного параметра запроса.
     *
     * <p>Возникает, когда в запросе отсутствует параметр, помеченный как
     * {@code @RequestParam(required = true)} или {@code @RequestParam} без значения по умолчанию.
     *
     * <p><b>Пример:</b>
     * <pre>{@code
     * // Контроллер
     * @GetMapping("/users")
     * public User getUser(@RequestParam Long id) { ... }
     *
     * // Запрос: GET /users (без параметра id)
     * // Результат: FieldError с полем "id" и кодом MISSING_PARAMETER
     * }</pre>
     *
     * @param exception исключение с именем отсутствующего параметра
     * @return список с одним {@link FieldError} с кодом {@code MISSING_PARAMETER}
     */
    public List<FieldError> mapFieldErrors(final MissingServletRequestParameterException exception) {
        final String parameterName = exception.getParameterName();
        return List.of(FieldError.builder()
                .field(parameterName)
                .message(messageResolver.resolve("error.detail.missing_parameter", parameterName))
                .code(ErrorCode.MISSING_PARAMETER.code())
                .build()
        );
    }

    /**
     * Маппинг ошибки неверного типа параметра запроса.
     *
     * <p>Возникает, когда Spring не может преобразовать строковое значение параметра
     * в ожидаемый тип метода контроллера.
     *
     * <p><b>Примеры:</b>
     * <ul>
     *   <li>{@code @RequestParam Long id} со значением {@code "abc"} → нечисловое значение</li>
     *   <li>{@code @RequestParam Boolean active} со значением {@code "yes"} → невалидный boolean</li>
     *   <li>{@code @RequestParam UUID token} со значением {@code "invalid-uuid"} → неверный формат UUID</li>
     * </ul>
     *
     * <p><b>Структура ответа:</b>
     * <ul>
     *   <li>{@code field} — имя параметра (например, "id")</li>
     *   <li>{@code rejectedValue} — переданное значение (например, "abc")</li>
     *   <li>{@code code} — TYPE_MISMATCH</li>
     * </ul>
     *
     * @param exception исключение с информацией о параметре и ожидаемом типе
     * @return список с одним {@link FieldError} с кодом {@code TYPE_MISMATCH}
     */
    public List<FieldError> mapFieldErrors(final MethodArgumentTypeMismatchException exception) {
        final String value = Optional.ofNullable(exception.getValue()).map(Object::toString).orElse(null);
        final String type = Optional.ofNullable(exception.getRequiredType()).map(Class::getSimpleName).orElse("unknown");

        return List.of(FieldError.builder()
                .field(exception.getName())
                .message(messageResolver.resolve("error.detail.type_mismatch", value, type))
                .code(ErrorCode.TYPE_MISMATCH.code())
                .rejectedValue(value)
                .build()
        );
    }
}