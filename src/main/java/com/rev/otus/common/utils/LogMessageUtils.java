package com.rev.otus.common.utils;

import lombok.experimental.UtilityClass;

/**
 * Утилитарный класс с шаблонами для логирования.
 */
@UtilityClass
public class LogMessageUtils {

    /**
     * Шаблон для логирования входящего HTTP-запроса.
     */
    public static final String INCOMING_REQ =
            "INCOMING_REQUEST: method={} uri={} ip={} userAgent={}";

    /**
     * Шаблон для логирования завершения запроса (базовый).
     */
    public static final String REQ_COMPLETED =
            "REQUEST_COMPLETED: method={} uri={} httpStatus={}";

    /**
     * Шаблон для логирования завершения запроса с длительностью.
     */
    public static final String REQ_COMPLETED_DUR =
            "REQUEST_COMPLETED: method={} uri={} httpStatus={} duration={}ms";

    /**
     * Шаблон для логирования старта контроллера.
     */
    public static final String CTRL_START =
            "CONTROLLER_EXECUTION_STARTED: method={} userRoles={} status=AUTHORIZED";

    /**
     * Шаблон для логирования завершения контроллера.
     */
    public static final String CTRL_COMPLETED =
            "CONTROLLER_EXECUTION_COMPLETED: method={} userRoles={}";

    /**
     * Шаблон для логирования ошибки контроллера.
     */
    public static final String CTRL_FAILED =
            "CONTROLLER_EXECUTION_FAILED: method={} userRoles={}";

    /**
     * Шаблон для логирования ошибки контроллера с деталями.
     */
    public static final String CTRL_FAILED_ERR =
            "CONTROLLER_EXECUTION_FAILED: method={} userRoles={} error={}";

    /**
     * Шаблон для логирования клиентской ошибки.
     */
    public static final String CLIENT_ERR =
            "Client error for {} {}";

    /**
     * Шаблон для логирования клиентской ошибки с деталями.
     */
    public static final String CLT_ERR_DETAILS =
            "traceId={} {} {} failed: {}";

    /**
     * Шаблон для логирования отказа в доступе.
     */
    public static final String ACCESS_DENIED =
            "Access denied for {} {}: user={} roles={}";

    /**
     * Шаблон для логирования ошибки аутентификации.
     */
    public static final String AUTH_FAILED =
            "Authentication failed for {} {}";

    /**
     * Шаблон для логирования ошибки аутентификации с деталями.
     */
    public static final String AUTH_FAILED_ERR =
            "Authentication failed for {} {}: {}";

    /**
     * Шаблон для логирования внутренней ошибки.
     */
    public static final String INTERNAL_ERR =
            "INTERNAL ERROR: method={}, uri={}";

    /**
     * Шаблон для логирования бизнес-ошибки.
     */
    public static final String BUSINESS_ERR =
            "INTERNAL ERROR: method={}, uri={}, business logic error: {}";

    /**
     * Шаблон для логирования непредвиденной ошибки.
     */
    public static final String UNEXPECTED_ERR =
            "INTERNAL ERROR: method={}, uri={}, unexpected error: {}";
}
