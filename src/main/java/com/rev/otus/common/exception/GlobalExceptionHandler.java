package com.rev.otus.common.exception;

import com.rev.otus.common.exception.error.ApiError;
import com.rev.otus.common.exception.error.ApiErrorFactory;
import com.rev.otus.common.exception.error.ErrorCode;
import com.rev.otus.common.exception.error.mappers.BusinessErrorMapper;
import com.rev.otus.common.exception.error.mappers.JsonParseErrorMapper;
import com.rev.otus.common.exception.error.mappers.ParameterErrorMapper;
import com.rev.otus.common.exception.error.mappers.ValidationErrorMapper;
import com.rev.otus.common.utils.LoggingUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Глобальный обработчик исключений REST API.
 *
 * <p><b>Назначение:</b>
 * Перехватывает все исключения, возникающие в контроллерах и сервисах,
 * преобразует их в стандартизированный JSON-ответ {@link ApiError}.
 *
 * <p><b>Архитектура обработки:</b>
 * <ol>
 *   <li>Перехват конкретного типа исключения</li>
 *   <li>Логирование с traceId для отслеживания запроса</li>
 *   <li>Маппинг исключения в структуру {@link ApiError}</li>
 *   <li>Возврат HTTP-ответа с соответствующим статусом</li>
 * </ol>
 *
 * <p><b>Категории обрабатываемых исключений:</b>
 * <ul>
 *   <li><b>Ошибки парсинга JSON</b> — неверный формат, тип данных {@link HttpMessageNotReadableException} (400)</li>
 *   <li><b>Ошибки параметров запроса</b> — отсутствие {@link MissingServletRequestParameterException},
 *   неверный тип {@link MethodArgumentTypeMismatchException} (400)</li>
 *   <li><b>Ошибки валидации</b> — {@link MethodArgumentNotValidException}, {@link ConstraintViolationException} (400)</li>
 *   <li><b>HTTP ошибки</b> — {@link NoResourceFoundException} 404, {@link HttpRequestMethodNotSupportedException} 405,
 *   {@link HttpMediaTypeNotSupportedException} 415</li>
 *   <li><b>Бизнес-ошибки</b> — {@link BusinessException} (400, 404, 409)</li>
 *   <li><b>Необработанные исключения</b> — fallback {@link Exception} 500</li>
 * </ul>
 *
 * <p><b>Формат ответа:</b>
 * <pre>{@code
 * {
 *   "timestamp": "2024-01-15T10:30:00.000Z",
 *   "message": "Человеко-читаемое сообщение (локализованное)",
 *   "code": "STABLE_ERROR_CODE",
 *   "traceId": "550e8400-e29b-41d4-a716-446655440000",
 *   "details": [
 *     {
 *       "field": "имя_поля",
 *       "message": "Описание ошибки поля",
 *       "code": "FIELD_ERROR_CODE",
 *       "rejectedValue": "отклонённое_значение"
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @see ApiError
 * @see ApiErrorFactory
 * @see BusinessException
 * @see ErrorCode
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ApiErrorFactory errorFactory;

    private final JsonParseErrorMapper jsonParseMapper;
    private final ParameterErrorMapper parameterMapper;
    private final ValidationErrorMapper validationMapper;
    private final BusinessErrorMapper businessMapper;

    // ==================== JSON Parse Errors ====================

    /**
     * Обработчик ошибок парсинга JSON в теле запроса.
     *
     * <p><b>Когда вызывается:</b>
     * <ul>
     *   <li>Некорректный JSON синтаксис (незакрытые скобки, лишние запятые)</li>
     *   <li>Неверный тип данных (строка вместо числа, массив вместо объекта)</li>
     *   <li>Неверный формат даты/времени</li>
     *   <li>Неизвестные поля (если включена строгая проверка)</li>
     *   <li>Ошибка в конструкторе DTO при десериализации</li>
     * </ul>
     *
     * <p><b>Типы Jackson-исключений и их маппинг:</b>
     * <table border="1">
     *   <tr><th>Исключение Jackson</th><th>Код ошибки</th><th>Пример</th></tr>
     *   <tr><td>{@code InvalidFormatException}</td><td>{@code INVALID_FORMAT}</td><td>"abc" → LocalDate</td></tr>
     *   <tr><td>{@code MismatchedInputException}</td><td>{@code INVALID_TYPE}</td><td>[] → {}</td></tr>
     *   <tr><td>{@code ValueInstantiationException}</td><td>{@code INVALID_VALUE}</td><td>null в @NotNull поле</td></tr>
     *   <tr><td>Другие</td><td>{@code MALFORMED_JSON}</td><td>Синтаксическая ошибка</td></tr>
     * </table>
     *
     * @param request HTTP-запрос (для логирования)
     * @param exception исключение-обёртка над Jackson-исключением
     * @return {@link ApiError} с кодом, соответствующим причине ошибки
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleJsonParseError(final HttpServletRequest request, final HttpMessageNotReadableException exception) {
        LoggingUtils.logError(request, exception, HttpStatus.BAD_REQUEST);
        final JsonParseErrorMapper.JsonParseResult result  = jsonParseMapper.mapFieldErrors(exception);
        return errorFactory.create(result.errorCode(), result.details());
    }

    // ==================== Parameter Errors ====================

    /**
     * Обработчик ошибки отсутствия обязательного параметра запроса.
     *
     * <p><b>Когда вызывается:</b>
     * <ul>
     *   <li>В URL отсутствует {@code @RequestParam(required = true)} параметр</li>
     * </ul>
     *
     * @param request HTTP-запрос (для логирования)
     * @param exception исключение с именем отсутствующего параметра
     * @return {@link ApiError} с кодом {@code MISSING_PARAMETER}
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParameter(final HttpServletRequest request,
                                           final MissingServletRequestParameterException exception) {
        LoggingUtils.logError(request, exception, HttpStatus.BAD_REQUEST);
        return errorFactory.create(ErrorCode.MISSING_PARAMETER, parameterMapper.mapFieldErrors(exception));
    }

    /**
     * Обработчик ошибки неверного типа параметра запроса.
     *
     * <p><b>Когда вызывается:</b>
     * <ul>
     *   <li>Spring не может преобразовать строковый параметр в ожидаемый тип</li>
     *   <li>Пример: {@code @RequestParam Long id} со значением "abc"</li>
     * </ul>
     *
     * <p><b>Особенности:</b>
     * <ul>
     *   <li>Извлекает переданное значение (может быть null)</li>
     *   <li>Определяет ожидаемый тип (Long, Integer, Boolean, UUID, etc.)</li>
     *   <li>Формирует локализованное сообщение с подстановкой значения и типа</li>
     * </ul>
     *
     * @param request HTTP-запрос (для логирования)
     * @param exception исключение с информацией о параметре и ожидаемом типе
     * @return {@link ApiError} с кодом {@code TYPE_MISMATCH}
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(final HttpServletRequest request, final MethodArgumentTypeMismatchException exception) {
        LoggingUtils.logError(request, exception, HttpStatus.BAD_REQUEST);
        return errorFactory.create(ErrorCode.TYPE_MISMATCH, parameterMapper.mapFieldErrors(exception));
    }

    // ==================== Validation ====================

    /**
     * Обработчик ошибок валидации тела запроса {@code @RequestBody}.
     *
     * <p><b>Как включается валидация:</b>
     * <ul>
     *   <li>{@code @Valid @RequestBody Dto dto} — стандартная валидация</li>
     *   <li>{@code @Validated(Group.class) @RequestBody Dto dto} — валидация по группам</li>
     *   <li>{@code @Valid} на вложенных полях DTO для каскадной проверки</li>
     * </ul>
     *
     * <p><b>Когда вызывается:</b>
     * <ul>
     *   <li>Нарушение аннотаций валидации в теле запроса</li>
     *   <li>{@code @NotNull}, {@code @Size}, {@code @Pattern} и т.д.</li>
     *   <li>Кастомные аннотации валидации (например, {@code @NoXSS})</li>
     * </ul>
     *
     * <p><b>Особенности обработки:</b>
     * <ul>
     *   <li>Всегда возвращает HTTP 400 Bad Request (через {@code @ResponseStatus})</li>
     *   <li>Каждая ошибка поля преобразуется в отдельный {@link ApiError.FieldError}</li>
     *   <li>Для XSS-атак значение заменяется на маркер {@code [INJECTED]}</li>
     *   <li>Длинные строки обрезаются для безопасности</li>
     *   <li>Аннотации маппятся в стабильные коды: {@code NotNull → REQUIRED_FIELD}</li>
     * </ul>
     *
     * @param request   HTTP-запрос (для логирования URI и метода)
     * @param exception исключение с {@link org.springframework.validation.BindingResult}
     * @return {@link ApiError} с кодом {@code VALIDATION_ERROR} и списком ошибок полей
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(final HttpServletRequest request, final MethodArgumentNotValidException exception) {
        LoggingUtils.logError(request, exception, HttpStatus.BAD_REQUEST);
        return errorFactory.create(ErrorCode.VALIDATION_ERROR, validationMapper.mapFieldErrors(exception));
    }

    /**
     * Обработчик ошибок валидации параметров метода и возвращаемых значений.
     *
     * <p>Перехватывает нарушения Bean Validation, обнаруженные напрямую
     * для аргументов метода и возвращаемого значения.
     *
     * <p><b>Как включается валидация:</b>
     * <ul>
     *   <li>{@code @Validated} на классе контроллера — обязательно для валидации параметров метода</li>
     *   <li>{@code @Positive @RequestParam Long id} — валидация отдельных параметров запроса</li>
     *   <li>{@code @NotBlank @PathVariable String code} — валидация path-переменных</li>
     *   <li>Аннотации Bean Validation на возвращаемом значении метода</li>
     * </ul>
     *
     * <p><b>Когда вызывается:</b>
     * <ul>
     *   <li>Валидация одиночных параметров (например, {@code @Positive @RequestParam Long id})</li>
     *   <li>Валидация path-переменных ({@code @PathVariable @NotBlank String code})</li>
     *   <li>Валидация возвращаемого значения ({@code @Validated} на классе контроллера)</li>
     *   <li>Программная валидация через {@link jakarta.validation.Validator}</li>
     * </ul>
     *
     * <p><b>Отличия от {@link #handleValidation(HttpServletRequest, MethodArgumentNotValidException)}:</b>
     * <ul>
     *   <li>Тот обрабатывает валидацию тела запроса ({@code @RequestBody})</li>
     *   <li>Этот — валидацию параметров запроса, path-переменных и возвращаемых значений</li>
     *   <li>Формат ошибок идентичен, клиент не видит разницы</li>
     * </ul>
     *
     * @param request HTTP-запрос (для логирования)
     * @param exception исключение с набором {@link jakarta.validation.ConstraintViolation}
     * @return {@link ApiError} с кодом {@code VALIDATION_ERROR} и списком нарушений
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(final HttpServletRequest request, final ConstraintViolationException exception) {
        LoggingUtils.logError(request, exception, HttpStatus.BAD_REQUEST);
        return errorFactory.create(ErrorCode.VALIDATION_ERROR, validationMapper.mapFieldErrors(exception));
    }

    // ==================== HTTP Status Errors ====================

    /**
     * Обработчик запросов к несуществующим ресурсам (404).
     *
     * <p><b>Когда вызывается:</b>
     * <ul>
     *   <li>Запрос к URL, который не маппится ни на один контроллер</li>
     *   <li>Запрос к статическому ресурсу, которого нет</li>
     * </ul>
     *
     * <p><b>Важно:</b>
     * Не путать с бизнес-ошибкой "сущность не найдена" — там тоже 404,
     * но через {@link BusinessException} с дополнительным контекстом.
     *
     * @param request HTTP-запрос (для логирования)
     * @param exception      исключение Spring о недоступном ресурсе
     * @return {@link ApiError} с кодом {@code NOT_FOUND}
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(final HttpServletRequest request, final NoResourceFoundException exception) {
        LoggingUtils.logError(request, exception, HttpStatus.NOT_FOUND);
        return errorFactory.create(ErrorCode.NOT_FOUND);
    }

    /**
     * Обработчик запросов с неверным HTTP-методом (405).
     *
     * <p><b>Когда вызывается:</b>
     * <ul>
     *   <li>POST на эндпоинт, который принимает только GET</li>
     *   <li>DELETE на эндпоинт, который принимает только POST</li>
     *   <li>PUT на эндпоинт без соответствующего маппинга</li>
     * </ul>
     *
     * <p><b>Дополнительно:</b>
     * Spring автоматически добавляет заголовок {@code Allow: GET} в ответ.
     *
     * @param request HTTP-запрос (для логирования)
     * @param exception      исключение с информацией о не поддерживаемом методе
     * @return {@link ApiError} с кодом {@code METHOD_NOT_ALLOWED}
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiError handleMethodNotSupported(final HttpServletRequest request,
                                             final HttpRequestMethodNotSupportedException exception) {
        LoggingUtils.logError(request, exception, HttpStatus.METHOD_NOT_ALLOWED);
        return errorFactory.create(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * Обработчик запросов с неверным Content-Type (415).
     *
     * <p><b>Когда вызывается:</b>
     * <ul>
     *   <li>Контроллер ожидает {@code application/json}, а клиент шлёт {@code text/plain}</li>
     *   <li>Контроллер ожидает {@code multipart/form-data}, а клиент шлёт {@code application/x-www-form-urlencoded}</li>
     *   <li>Отсутствует заголовок {@code Content-Type}, когда он обязателен</li>
     * </ul>
     *
     * @param request HTTP-запрос (для логирования)
     * @param exception      исключение с информацией о не поддерживаемом Content-Type
     * @return {@link ApiError} с кодом {@code UNSUPPORTED_MEDIA_TYPE}
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiError handleMediaTypeNotSupported(final HttpServletRequest request,
                                                final HttpMediaTypeNotSupportedException exception) {
        LoggingUtils.logError(request, exception, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        return errorFactory.create(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    // ==================== Business Exception ====================

    /**
     * Обработчик бизнес-исключений {@link BusinessException}.
     *
     * <p><b>Когда вызывается:</b>
     * <ul>
     *   <li>Нарушение бизнес-правил</li>
     *   <li>Ресурс не найден ({@link ErrorCode#NOT_FOUND})</li>
     *   <li>Конфликт данных ({@link ErrorCode#BUSINESS_RULE_VIOLATION})</li>
     *   <li>Любые проверки в сервисном слое</li>
     * </ul>
     *
     * <p><b>Особенности:</b>
     * <ul>
     *   <li>HTTP статус берётся из {@link ErrorCode#getStatus()}</li>
     *   <li>Если указано поле {@code exception.getField()}, создаётся детализация в {@code details}</li>
     *   <li>Если указано {@code exception.getUserMessage()}, оно используется вместо стандартного</li>
     *   <li>Возвращает {@link ResponseEntity} с кастомным статусом (может быть 200, 400, 404, 409 и т.д.)</li>
     * </ul>
     *
     * @param request   HTTP-запрос (для логирования URI и метода)
     * @param exception бизнес-исключение с кодом ошибки и опциональными деталями
     * @return ResponseEntity с {@link ApiError} и HTTP-статусом из кода ошибки
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(final HttpServletRequest request, final BusinessException exception) {
        final HttpStatus status = exception.getErrorCode().getStatus();
        LoggingUtils.logError(request, exception, status);

        return ResponseEntity.status(status).body(businessMapper.mapToApiError(exception));
    }

    // ==================== Fallback ====================

    /**
     * Fallback-обработчик для всех необработанных исключений (500).
     *
     * <p><b>Когда вызывается:</b>
     * <ul>
     *   <li>Любое исключение, для которого нет специфичного {@code @ExceptionHandler}</li>
     *   <li>{@code NullPointerException}, {@code IllegalArgumentException}</li>
     *   <li>Ошибки базы данных ({@code DataAccessException})</li>
     *   <li>Ошибки сериализации/десериализации</li>
     *   <li>Любые другие {@code RuntimeException} и {@code Exception}</li>
     * </ul>
     *
     * <p><b>Особенности:</b>
     * <ul>
     *   <li>Всегда возвращает HTTP 500 Internal Server Error</li>
     *   <li>Логирует с уровнем ERROR (в отличие от WARN для клиентских ошибок)</li>
     *   <li>Логирует полный стектрейс для отладки</li>
     *   <li>Возвращает обезличенное сообщение (без технических деталей)</li>
     *   <li>Не раскрывает внутреннюю структуру исключения клиенту (безопасность)</li>
     * </ul>
     *
     * <p><b>Логирование:</b>
     * <pre>{@code
     * 2024-01-15 10:30:00.123 ERROR [traceId=550e8400] GET /api/ffc failed: Connection refused
     * java.net.ConnectException: Connection refused
     *     at sun.nio.ch.Net.connect0(Native Method)
     *     ...
     * }</pre>
     *
     * @param request HTTP-запрос (для логирования)
     * @param exception      любое необработанное исключение
     * @return {@link ApiError} с кодом {@code INTERNAL_ERROR}
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleGeneric(final HttpServletRequest request, final Exception exception) {
        LoggingUtils.logError(request, exception, HttpStatus.INTERNAL_SERVER_ERROR);
        return errorFactory.create(ErrorCode.INTERNAL_ERROR);
    }

}