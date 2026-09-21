package com.rev.otus.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

/**
 * Централизованные утилиты для логирования.
 *
 * <p>Содержит методы для единообразного логирования HTTP-запросов,
 * ошибок, контроллеров и security-событий.
 */
@Slf4j
@UtilityClass
public class LoggingUtils {

    // ==================== Error Logging ====================

    /**
     * Унифицированное логирование ошибок с учётом их критичности.
     *
     * <p><b>Правила логирования:</b>
     * <ul>
     *   <li><b>5xx ошибки:</b> {@code ERROR} уровень + полный стектрейс</li>
     *   <li><b>4xx ошибки:</b> {@code WARN} уровень без стектрейса</li>
     *   <li><b>4xx ошибки в DEBUG режиме:</b> дополнительно логируется стектрейс</li>
     * </ul>
     *
     * <p><b>Формат лога:</b>
     * <pre>{@code
     * WARN traceId=550e8400 GET /api/ffc/123 failed: FFC with ID 123 not found
     * }</pre>
     *
     * <p><b>Компоненты лога:</b>
     * <ul>
     *   <li>{@code traceId} — из MDC (если нет — "no-trace")</li>
     *   <li>{@code method} — HTTP метод (GET, POST, etc.)</li>
     *   <li>{@code uri} — путь запроса</li>
     *   <li>{@code message} — сообщение исключения</li>
     * </ul>
     *
     * <p><b>Безопасность:</b>
     * Не логирует чувствительные данные (пароли, токены) — это должно быть
     * отфильтровано на уровне контроллера или DTO.
     *
     * @param request HTTP-запрос (для извлечения метода и URI)
     * @param exception      исключение (для извлечения сообщения и стектрейса)
     * @param status  HTTP статус (определяет уровень логирования)
     */
    public void logError(final HttpServletRequest request, final Exception exception, final HttpStatus status) {
        final String tid = TraceIdUtils.getCurrentTraceId();
        final String errorMsg = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName();

        if (status.is5xxServerError()) {
            log.error("traceId={} {} {} failed: {}", tid, request.getMethod(), request.getRequestURI(), errorMsg, exception);
        } else {
            log.warn("traceId={} {} {} failed: {}", tid, request.getMethod(), request.getRequestURI(), errorMsg);
            if (log.isDebugEnabled()) {
                log.debug("Stack trace for client error: ", exception);
            }
        }
    }

}
