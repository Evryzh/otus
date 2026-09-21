# ДЗ №4: Инфраструктурные паттерны

**Студент:** Евгений Рыжков (Evryzh)
**Репозиторий:** `https://github.com/Evryzh/otus`
**Ветка:** `feature/hw4`

---

## Задание

Сделать простейший RESTful CRUD по созданию, удалению, просмотру и обновлению пользователей.

Требования:

- База данных для приложения.
- Конфигурация — в ConfigMap.
- Доступы к БД — в Secret.
- Первоначальные миграции — в виде Job.
- Ingress на хост `arch.homework`.
- Postman-коллекция с CRUD-запросами.
- Проверка через `newman`.

**Задание со звёздочкой:** шаблонизация приложения в Helm-чартах.

---

## Архитектура

- **Приложение:** Spring Boot `otus-users`, порт 8000.
- **БД:** PostgreSQL, база `otus` (для приложения) и `keycloak` (для Keycloak).
- **Аутентификация:** Keycloak, realm `otus`, client `otus-users`.
- **Ingress:** nginx, хост `arch.homework`.
- **CRUD:** профиль пользователя в таблице `customer_profile`.

### Связь с Keycloak

При `POST /api/v1/profile` приложение:

1. Проверяет уникальность `username` и `email`.
2. Создаёт пользователя в Keycloak через Admin API.
3. Получает UUID (`keycloak_user_id`).
4. Назначает роль `user`.
5. Сохраняет профиль в `customer_profile`.

При `GET`, `PUT`, `DELETE` — работает с профилем по `sub` из JWT.

---

## Предварительные требования

- Работающий кластер Kubernetes
- Установленный **kubectl**, настроенный на кластер
- Установленный **Helm**
- Установленный **nginx-ingress controller**
- DNS-имена `arch.homework` и `keycloak.homework` резолвятся в адрес ingress-контроллера
- Ingress-контроллер принимает трафик на порту 80
- Установленный **newman** для проверки Postman-коллекции

> **Внимание:** Все пароли в этом ДЗ — учебные и предназначены только
> для локального развёртывания. В продакшене секреты должны храниться
> в Kubernetes Secret, Vault или внешнем секрет-менеджере.

---

## Развёртывание

### 1. PostgreSQL

Создать namespace и установить PostgreSQL:

```bash
kubectl create namespace otus
helm install postgres bitnami/postgresql \
  -n otus \
  -f hw/hw4/helm/postgres-values.yaml
```

Создать базу и пользователя для Keycloak:

```bash
kubectl exec -it -n otus postgres-postgresql-0 -- psql -U postgres -c "CREATE DATABASE keycloak;"
kubectl exec -it -n otus postgres-postgresql-0 -- psql -U postgres -c "CREATE USER keycloak WITH PASSWORD 'keycloak';"
kubectl exec -it -n otus postgres-postgresql-0 -- psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;"
kubectl exec -it -n otus postgres-postgresql-0 -- psql -U postgres -d keycloak -c "GRANT ALL ON SCHEMA public TO keycloak;"
kubectl exec -it -n otus postgres-postgresql-0 -- psql -U postgres -d keycloak -c "ALTER SCHEMA public OWNER TO keycloak;"
```

Подробнее — в `hw/hw4/helm/README.md`.

### 2. Keycloak

Установить Keycloak:

```bash
helm repo add codecentric https://codecentric.github.io/helm-charts
helm repo update
helm install keycloak codecentric/keycloakx \
  -n keycloak \
  -f hw/hw4/helm/keycloak-values.yaml
```

Исправить хост Ingress (чарт игнорирует `ingress.hostname`):

```bash
kubectl patch ingress keycloak-keycloakx -n keycloak --type=json \
  -p='[{"op": "replace", "path": "/spec/rules/0/host", "value": "keycloak.homework"}]'
```

Настроить Realm, Client, роль и тестового пользователя — команды в `hw/hw4/helm/README.md`.

### 3. Приложение (plain-манифесты)

Применить манифесты Kubernetes:

```bash
kubectl apply -f hw/hw4/k8s/
```

Порядок применения:

1. `namespace.yaml` — namespace `otus`.
2. `configmap.yaml` — конфигурация приложения.
3. `secret.yaml` — доступы к БД.
4. `job-migration.yaml` — Flyway-миграции.
5. `deployment.yaml` — приложение.
6. `service.yaml` — сервис.
7. `ingress.yaml` — Ingress на `arch.homework`.

Проверить:

```bash
kubectl get pods -n otus
kubectl get jobs -n otus
kubectl get ingress -n otus
```

### 4. Применение миграций

```bash
kubectl apply -f hw/hw4/k8s/job-migration.yaml
kubectl wait --for=condition=complete job/flyway-migration -n otus --timeout=120s
```

---

## Проверка через curl

```bash
# Получить токен
USER_TOKEN=$(curl -s -X POST \
  http://keycloak.homework/auth/realms/otus/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" -d "password=testpass" -d "grant_type=password" -d "client_id=otus-users" \
  | jq -r '.access_token')

# Создать профиль
curl -X POST http://arch.homework/api/v1/profile \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "secret123",
    "email": "johndoe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+71002003040"
  }'

# Получить профиль
curl -X GET http://arch.homework/api/v1/profile \
  -H "Authorization: Bearer $USER_TOKEN"

# Обновить профиль
curl -X PUT http://arch.homework/api/v1/profile \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName": "Jane", "phone": "+71009998877"}'

# Удалить профиль
curl -X DELETE http://arch.homework/api/v1/profile \
  -H "Authorization: Bearer $USER_TOKEN"
```

---

## Проверка через newman

Установить newman:

```bash
sudo apt update
sudo apt install -y nodejs npm
sudo npm install -g newman
```

Запустить коллекцию:

```bash
newman run hw/hw4/postman/otus-users.postman_collection.json
```

Ожидаемо: 4 запроса (POST, GET, PUT, DELETE), 0 ошибок.

---

## ⭐ Задание со звёздочкой: Helm-чарт приложения

Помимо plain-манифестов в `k8s/`, приложение можно развернуть через **Helm-чарт** `charts/otus-users/`. Чарт шаблонизирует те же ресурсы: Deployment, Service, Ingress, ConfigMap, Secret, Job миграций.

### Структура чарта

```
charts/otus-users/
├── Chart.yaml
├── values.yaml
└── templates/
    ├── _helpers.tpl
    ├── configmap.yaml
    ├── secret.yaml
    ├── job-migration.yaml
    ├── deployment.yaml
    ├── service.yaml
    └── ingress.yaml
```

### Установка через Helm

```bash
helm install otus-users hw/hw4/charts/otus-users \
  -n otus \
  --create-namespace
```

Или с переопределением параметров:

```bash
helm install otus-users hw/hw4/charts/otus-users \
  -n otus \
  --create-namespace \
  --set image.tag=1.4.0 \
  --set ingress.host=arch.homework
```

### Проверка

```bash
helm list -n otus
kubectl get pods -n otus
kubectl get ingress -n otus
```

---

## Формат ответов на ошибки

Все ошибки API возвращаются в едином формате — **`ApiError`**.

### Структура `ApiError`

```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "message": "Локализованное сообщение",
  "code": "STABLE_ERROR_CODE",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "details": [
    {
      "field": "имя_поля",
      "message": "Описание ошибки поля",
      "code": "FIELD_ERROR_CODE",
      "rejectedValue": "отклонённое_значение"
    }
  ]
}
```

### Поля

| Поле        | Описание                                                |
|-------------|---------------------------------------------------------|
| `timestamp` | Время ошибки в UTC (ISO 8601)                           |
| `message`   | Локализованное сообщение (зависит от `Accept-Language`) |
| `code`      | Стабильный код ошибки для программной обработки         |
| `traceId`   | Идентификатор запроса — для отладки в логах             |
| `details`   | Детали ошибок по полям (может отсутствовать)            |

### Коды ошибок

| Код                       | HTTP | Когда                        |
|---------------------------|------|------------------------------|
| `VALIDATION_ERROR`        | 400  | Ошибка валидации DTO         |
| `MISSING_PARAMETER`       | 400  | Отсутствует параметр запроса |
| `TYPE_MISMATCH`           | 400  | Неверный тип параметра       |
| `INVALID_FORMAT`          | 400  | Неверный формат данных       |
| `MALFORMED_JSON`          | 400  | Синтаксическая ошибка JSON   |
| `NOT_FOUND`               | 404  | Ресурс не найден             |
| `METHOD_NOT_ALLOWED`      | 405  | Неверный HTTP-метод          |
| `UNSUPPORTED_MEDIA_TYPE`  | 415  | Неверный Content-Type        |
| `DUPLICATE_ENTITY`        | 409  | Нарушение уникальности       |
| `BUSINESS_RULE_VIOLATION` | 409  | Нарушение бизнес-правила     |
| `INTERNAL_ERROR`          | 500  | Внутренняя ошибка сервера    |

### Пример: валидация DTO

**Запрос:**

```bash
curl -X POST http://arch.homework/api/v1/profile \
  -H "Content-Type: application/json" \
  -d '{"username": "", "email": "invalid"}'
```

**Ответ:**

```json
{
  "timestamp": "2026-09-21T01:00:00.000Z",
  "message": "Ошибка валидации запроса",
  "code": "VALIDATION_ERROR",
  "traceId": "…",
  "details": [
    {
      "field": "username",
      "message": "must not be blank",
      "code": "REQUIRED_FIELD",
      "rejectedValue": ""
    },
    {
      "field": "email",
      "message": "must be a well-formed email address",
      "code": "INVALID_EMAIL",
      "rejectedValue": "invalid"
    }
  ]
}
```

### Пример: профиль не найден

**Ответ:**

```json
{
  "timestamp": "…",
  "message": "Профиль пользователя с keycloakUserId '…' не найден",
  "code": "NOT_FOUND",
  "traceId": "…",
  "details": [
    {
      "field": "keycloakUserId",
      "message": "Ресурс не найден",
      "code": "NOT_FOUND",
      "rejectedValue": "…"
    }
  ]
}
```

### Пример: дубликат

**Ответ:**

```json
{
  "timestamp": "…",
  "message": "Логин 'johndoe' уже занят",
  "code": "DUPLICATE_ENTITY",
  "traceId": "…",
  "details": [
    {
      "field": "username",
      "message": "Duplicate entity",
      "code": "DUPLICATE_ENTITY",
      "rejectedValue": "johndoe"
    }
  ]
}
```

### Локализация

Сообщение (`message`) выбирается в зависимости от заголовка `Accept-Language`:

- `Accept-Language: ru` → русское сообщение.
- `Accept-Language: en` (или отсутствует) → английское.

### TraceId

Каждый запрос получает уникальный `traceId`:

- Передаётся в заголовке ответа `X-Trace-Id`.
- Дублируется в теле `ApiError.traceId`.
- Используется для поиска в логах.

**Поиск в логах:**

```bash
kubectl logs -n otus -l app.kubernetes.io/name=otus-users | grep "traceId=<id>"
```

### Удаление

```bash
helm uninstall otus-users -n otus
```

### Параметры `values.yaml`

| Параметр                    | Значение по умолчанию                        | Описание            |
|-----------------------------|----------------------------------------------|---------------------|
| `replicaCount`              | `2`                                          | Количество реплик   |
| `image.repository`          | `evryz/otus-users`                           | Образ приложения    |
| `image.tag`                 | `1.0.0`                                      | Тег образа          |
| `image.pullPolicy`          | `IfNotPresent`                               | Политика скачивания |
| `service.port`              | `80`                                         | Порт сервиса        |
| `service.targetPort`        | `8000`                                       | Порт приложения     |
| `ingress.enabled`           | `true`                                       | Включить Ingress    |
| `ingress.className`         | `nginx`                                      | Ingress-класс       |
| `ingress.host`              | `arch.homework`                              | Хост Ingress        |
| `postgres.host`             | `postgres-postgresql.otus.svc.cluster.local` | Хост БД             |
| `postgres.port`             | `5432`                                       | Порт БД             |
| `postgres.database`         | `otus`                                       | Имя БД              |
| `postgres.username`         | `otus`                                       | Пользователь БД     |
| `keycloak.url`              | `http://keycloak.homework/auth`              | URL Keycloak        |
| `keycloak.realm`            | `otus`                                       | Realm               |
| `keycloak.clientId`         | `otus-users`                                 | Client ID           |
| `keycloak.adminUsername`    | `admin`                                      | Admin Keycloak      |
| `resources.requests.memory` | `512Mi`                                      | Запрос памяти       |
| `resources.limits.memory`   | `1Gi`                                        | Лимит памяти        |

### Проверка через newman

После установки через Helm:

```bash
newman run hw/hw4/postman/otus-users.postman_collection.json
```

Ожидаемо: 4 запроса, 0 ошибок.

---

## Удаление

```bash
kubectl delete -f hw/hw4/k8s/
helm uninstall keycloak -n keycloak
helm uninstall postgres -n otus
kubectl delete namespace otus keycloak
```

Если разворачивали через Helm-чарт приложения:

```bash
helm uninstall otus-users -n otus
```

---

## Структура

```
hw/hw4/
├── doc/
│   ├── README.md          ← этот файл
│   └── REPORT.md          ← отчёт с результатами newman
├── helm/
│   ├── postgres-values.yaml
│   ├── keycloak-values.yaml
│   └── README.md          ← команды установки БД и Keycloak
├── charts/
│   └── otus-users/        ← ⭐ Helm-чарт приложения
│       ├── Chart.yaml
│       ├── values.yaml
│       └── templates/
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── job-migration.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── ingress.yaml
└── postman/
    └── otus-users.postman_collection.json
```

---

## Ссылки

- **Pull Request:** `https://github.com/Evryzh/otus/pull/N` (замените N)
- **Docker Hub:** `https://hub.docker.com/r/evryz/otus-users`
- **Репозиторий:** `https://github.com/Evryzh/otus`