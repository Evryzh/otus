# 1.2.0
- Первый релиз для ДЗ 2, только health
# 1.4.0
- API для профиля пользователя (Create, Read, Update, Delete)
- Идемпотентный POST /api/v1/profile
- Подсистема безопасности (OAuth2 Resource Server, Keycloak)
- Интеграция с Keycloak Admin API (создание/удаление пользователей, роли)
- Единый формат ответов об ошибках (ApiError) с локализацией (ru/en)
- Трассировка запросов (X-Trace-Id, MDC)
- Swagger UI (springdoc-openapi)
- Flyway-миграции
