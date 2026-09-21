package com.rev.otus.common.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Фильтр для управления traceId (идентификатором трассировки запроса).
 *
 * <p><b>Назначение:</b> обеспечивает уникальную идентификацию каждого HTTP-запроса
 * для связывания всех логов, сгенерированных в процессе обработки запроса.
 *
 * <p><b>Что делает:</b>
 * <ol>
 *   <li>Извлекает traceId из заголовка {@code X-Trace-Id} (если передан клиентом)</li>
 *   <li>Если заголовок отсутствует или пуст — генерирует новый {@link UUID}</li>
 *   <li>Помещает traceId в {@link MDC} (Mapped Diagnostic Context) для автоматического
 *       добавления во все логи через конфигурацию Logback</li>
 *   <li>Добавляет traceId в заголовок ответа {@code X-Trace-Id}</li>
 *   <li>Очищает MDC после обработки запроса (в блоке {@code finally})</li>
 * </ol>
 *
 * <p><b>Почему это важно:</b>
 * <ul>
 *   <li><b>Связность логов</b> — все логи одного запроса имеют одинаковый traceId</li>
 *   <li><b>Поддержка клиентов</b> — клиент может передать свой traceId для сквозной трассировки</li>
 *   <li><b>Отладка</b> — по traceId можно найти все логи, связанные с проблемным запросом</li>
 *   <li><b>Безопасность</b> — traceId не содержит чувствительной информации</li>
 * </ul>
 *
 * <p><b>Пример использования клиентом:</b>
 * <pre>{@code
 * // Клиент передаёт свой traceId
 * GET /api/users/123
 * X-Trace-Id: 550e8400-e29b-41d4-a716-446655440000
 *
 * // Сервер ответит с тем же traceId
 * HTTP/1.1 200 OK
 * X-Trace-Id: 550e8400-e29b-41d4-a716-446655440000
 * }</pre>
 *
 * <p><b>Пример логов (с конфигурацией Logback):</b>
 * <pre>{@code
 * 2024-01-15 10:30:00.123 INFO [traceId=550e8400] --- INCOMING REQUEST: GET /api/users/123
 * 2024-01-15 10:30:00.456 INFO [traceId=550e8400] --- Processing user request
 * 2024-01-15 10:30:00.789 INFO [traceId=550e8400] --- REQUEST COMPLETED: status 200
 * }</pre>
 *
 * <p><b>Порядок выполнения:</b>
 * Фильтр имеет наивысший приоритет ({@link Ordered#HIGHEST_PRECEDENCE}),
 * чтобы traceId был доступен во всех последующих фильтрах (например, {@link MvcLoggerFilter})
 * и в контроллерах.
 *
 * @see MvcLoggerFilter
 * @see MDC
 * @see UUID
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@NoArgsConstructor
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID = "traceId";
    public static final String HEADER = "X-Trace-Id";
    private static final int MAX_TRACE_ID_LEN = 128;

    /**
     * Очищает и валидирует traceId от потенциально опасных символов.
     *
     * @param traceId исходный traceId (может быть {@code null})
     * @return очищенный traceId или {@code null}, если после очистки строка пуста
     */
    private static String sanitizeTraceId(final String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return null;
        }
        // Оставляем только безопасные символы: буквы, цифры, дефис, подчёркивание, точка, двоеточие
        final String sanitized = traceId.replaceAll("[^a-zA-Z0-9\\-_:.]", "");
        if (sanitized.isEmpty()) {
            return null;
        }
        // Обрезаем слишком длинные значения
        if (sanitized.length() > MAX_TRACE_ID_LEN) {
            return sanitized.substring(0, MAX_TRACE_ID_LEN);
        }
        return sanitized;
    }

    /**
     * Обрабатывает HTTP-запрос: устанавливает traceId в MDC и заголовок ответа.
     *
     * @param request  HTTP-запрос
     * @param response HTTP-ответ
     * @param chain    цепочка фильтров
     * @throws IOException      если ошибка ввода-вывода
     * @throws ServletException если ошибка обработки сервлета
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        try {
            final HttpServletRequest httpRequest = (HttpServletRequest) request;
            final HttpServletResponse httpResponse = (HttpServletResponse) response;

            String traceId = httpRequest.getHeader(HEADER);
            final String sanitizedTraceId = sanitizeTraceId(traceId);
            if (sanitizedTraceId != null) {
                traceId = sanitizedTraceId;
            } else {
                traceId = UUID.randomUUID().toString();
            }

            MDC.put(TRACE_ID, traceId);
            httpResponse.setHeader(HEADER, traceId);

            chain.doFilter(request, response);

        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}