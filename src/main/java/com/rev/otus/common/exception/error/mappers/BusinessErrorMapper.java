package com.rev.otus.common.exception.error.mappers;

import com.rev.otus.common.exception.BusinessException;
import com.rev.otus.common.exception.error.ApiError;
import com.rev.otus.common.exception.error.ApiErrorFactory;
import com.rev.otus.common.exception.error.ErrorCode;
import com.rev.otus.common.exception.error.MessageResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Маппинг бизнес-исключений в {@link ApiError}.
 *
 * <p><b>Назначение:</b> преобразует {@link BusinessException} в готовый {@link ApiError}
 * с локализацией сообщений и детализацией по полям.
 *
 * <p><b>Особенности:</b>
 * <ul>
 *   <li>Поддерживает локализацию через messageKey + args</li>
 *   <li>Автоматически формирует FieldError при указании поля</li>
 *   <li>Стандартизирует ответы для всех бизнес-ошибок</li>
 * </ul>
 *
 * @see BusinessException
 * @see ApiError
 * @see ApiErrorFactory
 */
@Component
@RequiredArgsConstructor
public class BusinessErrorMapper {
    private final ApiErrorFactory errorFactory;
    private final MessageResolver messageResolver;

    /**
     * Преобразует бизнес-исключение в ApiError.
     *
     * <p><b>Алгоритм:</b>
     * <ol>
     *   <li>Определяет HTTP статус из {@link ErrorCode}</li>
     *   <li>Формирует детали ошибки, если указано поле</li>
     *   <li>Локализует сообщение (приоритет: userMessage → messageKey → стандартное)</li>
     * </ol>
     *
     * <p><b>Примеры:</b>
     * <pre>{@code
     * // Стандартная ошибка
     * var exception = new BusinessException(ErrorCode.NOT_FOUND);
     * var apiError = mapper.mapToApiError(exception);
     *
     * // Ошибка с полем
     * var exception = new BusinessException(ErrorCode.DUPLICATE_ENTITY, "name", "duplicate");
     * var apiError = mapper.mapToApiError(exception);
     *
     * // Ошибка с локализованным сообщением
     * var exception = new BusinessException(ErrorCode.NOT_FOUND, null, null,
     *         "error.cur.not_found", objId, parId);
     * var apiError = mapper.mapToApiError(exception);
     * }</pre>
     *
     * @param exception бизнес-исключение
     * @return готовый {@link ApiError} для возврата клиенту
     */
    public ApiError mapToApiError(final BusinessException exception) {
        final List<ApiError.FieldError> details = exception.getField() == null
                ? null
                : List.of(errorFactory.createFieldError(
                exception.getErrorCode(),
                exception.getField(),
                exception.getRejectedValue(),
                null
        ));

        // Локализация сообщения
        String localizedMessage = exception.getUserMessage();
        if (localizedMessage == null && exception.getMessageKey() != null) {
            localizedMessage = messageResolver.resolve(exception.getMessageKey(), exception.getMessageArgs());
        }

        return errorFactory.create(exception.getErrorCode(), details, localizedMessage);
    }
}
