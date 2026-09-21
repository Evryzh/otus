package com.rev.otus.common.utils;

import com.rev.otus.common.web.filter.TraceIdFilter;
import lombok.experimental.UtilityClass;
import org.slf4j.MDC;

/**
 * Утилитный класс для получения текущего traceId из MDC.
 *
 * <p><b>Назначение:</b> централизованное получение идентификатора трассировки
 * для логирования и формирования ответов API.
 *
 * <p><b>Пример использования:</b>
 * <pre>{@code
 * String traceId = TraceIdUtils.getCurrentTraceId();
 * log.info("traceId={} Processing request", traceId);
 * }</pre>
 *
 * @see TraceIdFilter
 * @see MDC
 */
@UtilityClass
public class TraceIdUtils {

    public String getCurrentTraceId() {
        final String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        return traceId != null ? traceId : "no-trace";
    }
}