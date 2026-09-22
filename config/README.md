# Конфигурация приложения

В этой директории лежат шаблоны конфигурационных файлов и описание переменных окружения.

## Файлы

| Файл           | Назначение                  | Коммитим? |
|----------------|-----------------------------|-----------|
| `.env.example` | Шаблон переменных окружения | ✅ Да     |
| `README.md`    | Этот файл                   | ✅ Да     |

## Локальная разработка

1. Скопируйте `.env.example` в корень проекта как `.env`:

cp config/.env.example .env

2. Заполните `.env` своими значениями. Не коммитьте его.

3. Убедитесь, что `.env` добавлен в `.gitignore`.

4. В IntelliJ подключите `.env` через плагин EnvFile:
   - Run → Edit Configurations → OtusApplication → EnvFile.
   - Включите Enable EnvFile.
   - Добавьте файл `.env`.

## Kubernetes

В K8s переменные окружения не читаются из `.env`. Вместо этого используются:

- ConfigMap `otus-users-config` — несекретные параметры
(URL БД, URL Keycloak, realm, client ID, флаг Flyway).
- Secret `otus-users-secret` — секреты (пароль БД, пароль admin Keycloak).

Оба подключаются к Deployment через `envFrom`:

envFrom:
- configMapRef:
  name: otus-users-config
- secretRef:
  name: otus-users-secret

Тогда `application.yaml` внутри контейнера читает те же переменные,
что и локально — код не меняется.

## Переменные окружения

### PostgreSQL

| Переменная                 | Описание             | По умолчанию                          | Где в K8s |
|----------------------------|----------------------|---------------------------------------|-----------|
| SPRING_DATASOURCE_URL      | JDBC URL базы данных | jdbc:postgresql://localhost:5433/otus | ConfigMap |
| SPRING_DATASOURCE_USERNAME | Пользователь БД      | otus                                  | ConfigMap |
| SPRING_DATASOURCE_PASSWORD | Пароль БД            | —                                     | Secret    |

### Keycloak (OAuth2 Resource Server)

| Переменная         | Описание             | По умолчанию          | Где в K8s |
|--------------------|----------------------|-----------------------|-----------|
| KEYCLOAK_BASE_URL  | Базовый URL Keycloak | http://localhost:8080 | ConfigMap |
| KEYCLOAK_REALM     | Realm для приложения | otus                  | ConfigMap |
| KEYCLOAK_CLIENT_ID | Client ID приложения | otus-users            | ConfigMap |

Дополнительно можно переопределить OIDC endpoints явно
(по умолчанию формируются из KEYCLOAK_BASE_URL и KEYCLOAK_REALM):

| Переменная                                              | Описание                      |
|---------------------------------------------------------|-------------------------------|
| SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI    | Issuer URI (проверка JWT)     |
| SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI   | JWK Set URI (публичные ключи) |

### Keycloak Admin API

Используется для создания пользователей в Keycloak при POST /api/v1/profile.

| Переменная                | Описание              | По умолчанию | Где в K8s  |
|---------------------------|-----------------------|--------------|------------|
| KEYCLOAK_ADMIN_USERNAME   | Логин администратора  | admin        | ConfigMap  |
| KEYCLOAK_ADMIN_PASSWORD   | Пароль администратора | —            | Secret     |

### Приложение

| Переменная    | Описание            | По умолчанию | Где в K8s  |
|---------------|---------------------|--------------|------------|
| SERVER_PORT   | Порт приложения     | 8000         | —          |
| LOG_LEVEL     | Уровень логирования | INFO         | ConfigMap  |

### Flyway

| Переменная       | Описание                     | Локально | K8s                          |
|------------------|------------------------------|----------|------------------------------|
| FLYWAY_ENABLED   | Включить миграции при старте | true     | false (миграции через Job)   |

В K8s миграции выполняет отдельный Job с профилем `migration`.
Профиль явно включает Flyway (application-migration.yaml), независимо от FLYWAY_ENABLED.

### Swagger / OpenAPI

| Переменная                     | Описание             | По умолчанию | Где в K8s  |
|--------------------------------|----------------------|--------------|------------|
| SPRINGDOC_SWAGGER_UI_ENABLED   | Включить Swagger UI  | false        | —          |
| SPRINGDOC_API_DOCS_ENABLED     | Включить OpenAPI     | false        | —          |

По умолчанию Swagger выключен (безопасный дефолт). Включается локально через `.env` или
переменные окружения.

### Профиль Spring

| Переменная                 | Описание                                            |
|----------------------------|-----------------------------------------------------|
| SPRING_PROFILES_ACTIVE     | migration — профиль для K8s Job (только Flyway)     |

Обычно задавать не нужно — применяется автоматически в Job'е.

## Безопасность

- `.env` никогда не коммитится. Он в `.gitignore`.
- В git попадает только `.env.example` — без реальных значений.
- В K8s секреты хранятся в `Secret`, не в `ConfigMap`.
- Пароли в `Secret` кодируются в base64, но это не шифрование — при доступе к кластеру они читаются.
Для продакшена используют Vault, Sealed Secrets или внешние secret-менеджеры. 
Для ДЗ достаточно стандартного `Secret`.