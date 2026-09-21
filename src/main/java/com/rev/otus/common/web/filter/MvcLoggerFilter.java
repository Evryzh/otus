package com.rev.otus.common.web.filter;

import com.rev.otus.common.exception.GlobalExceptionHandler;
import com.rev.otus.common.utils.LogMessageUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Фильтр для логирования HTTP-запросов и их выполнения.
 *
 * <p><b>Назначение:</b> логирует информацию о каждом входящем запросе
 * для мониторинга, аудита и отладки.
 *
 * <p><b>Что делает:</b>
 * <ol>
 *   <li>Логирует входящий запрос (метод, URI, IP клиента, User-Agent) — уровень INFO</li>
 *   <li>Замеряет время выполнения запроса</li>
 *   <li>Логирует завершение запроса с HTTP статусом и длительностью — уровень DEBUG</li>
 * </ol>
 *
 * <p><b>Почему это важно:</b>
 * <ul>
 *   <li><b>Аудит</b> — кто и что запрашивал (IP, User-Agent)</li>
 *   <li><b>Мониторинг</b> — можно отслеживать медленные запросы по длительности</li>
 *   <li><b>Отладка</b> — помогает воспроизвести проблемную ситуацию</li>
 *   <li><b>Безопасность</b> — логируются даже запросы, упавшие с ошибкой валидации</li>
 * </ul>
 *
 * <p><b>Пример логов:</b>
 * <pre>{@code
 * // Входящий запрос (INFO)
 * 2024-01-15 10:30:00.123 INFO [traceId=550e8400] --- INCOMING REQUEST: GET /api/users/123, client IP: 192.168.1.1,
 * User-Agent: Mozilla/5.0
 *
 * // Завершение запроса (DEBUG, при включённом debug-логировании)
 * 2024-01-15 10:30:00.456 DEBUG [traceId=550e8400] --- REQUEST COMPLETED: GET /api/users/123, status: 200, duration: 333.0 ms
 * }</pre>
 *
 * <p><b>Уровни логирования:</b>
 * <ul>
 *   <li><b>INFO</b> — всегда логируется входящий запрос (даже при ошибках)</li>
 *   <li><b>DEBUG</b> — завершение с длительностью логируется только при включённом DEBUG</li>
 * </ul>
 *
 * <p><b>Порядок выполнения:</b>
 * Фильтр имеет порядок {@code HIGHEST_PRECEDENCE + 1}, что гарантирует выполнение ПОСЛЕ {@link TraceIdFilter}
 * (который имеет HIGHEST_PRECEDENCE). Это позволяет логировать traceId в сообщениях.
 *
 * <p><b>Важно:</b> фильтр работает ДО контроллеров и ДО {@link GlobalExceptionHandler},
 * поэтому логирует даже запросы, упавшие с ошибками валидации или исключениями.
 *
 * @see TraceIdFilter
 * @see GlobalExceptionHandler
 * @see LogMessageUtils
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
@NoArgsConstructor
public class MvcLoggerFilter implements Filter {

    /**
     * Логирует HTTP-запрос и время его выполнения.
     *
     * <p><b>Алгоритм:</b>
     * <ol>
     *   <li>Логирует входящий запрос (уровень INFO) с IP клиента и User-Agent</li>
     *   <li>Запоминает время начала обработки</li>
     *   <li>Передаёт управление дальше по цепочке фильтров</li>
     *   <li>В блоке {@code finally} логирует завершение запроса с HTTP статусом и длительностью
     *       (уровень DEBUG)</li>
     * </ol>
     *
     * <p><b>Формат логов:</b>
     * <ul>
     *   <li>Входящий: {@code INCOMING REQUEST: {method} {uri}, client IP: {ip}, User-Agent: {userAgent}}</li>
     *   <li>Завершение: {@code REQUEST COMPLETED: {method} {uri}, status: {status}, duration: {duration} ms}</li>
     * </ul>
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

        final HttpServletRequest httpRequest = (HttpServletRequest) request;

        final String queryString = httpRequest.getQueryString();
        final String uri = queryString == null ? httpRequest.getRequestURI() : httpRequest.getRequestURI() + "?" + queryString;

        if (log.isInfoEnabled()) {
            final String xfHeader = httpRequest.getHeader("X-Forwarded-For");
            final String clientIP = xfHeader != null ? xfHeader.split(",")[0].trim() : httpRequest.getRemoteAddr();
            final String userAgent = httpRequest.getHeader("User-Agent");

            log.info(LogMessageUtils.INCOMING_REQ, httpRequest.getMethod(), uri, clientIP, userAgent);
        }

        final Instant startTime = Instant.now();

        try {
            chain.doFilter(request, response);
        } finally {
            if (log.isDebugEnabled()) {
                final HttpServletResponse httpResponse = (HttpServletResponse) response;
                final double millis = Duration.between(startTime, Instant.now()).toNanos() / 1_000_000.0;
                log.debug(LogMessageUtils.REQ_COMPLETED_DUR, httpRequest.getMethod(), uri, httpResponse.getStatus(), millis);
            }
        }
    }
}