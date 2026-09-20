# Установка PostgreSQL и Keycloak

## 1. PostgreSQL

### 1.1. Создать namespace

```bash
kubectl create namespace otus
```

### 1.2. Установить PostgreSQL

```bash
helm install postgres bitnami/postgresql \
  -n otus \
  -f hw/hw4/helm/postgres-values.yaml
```

### 1.3. Создать базу и пользователя для Keycloak

```bash
kubectl exec -it -n otus postgres-postgresql-0 -- psql -U postgres -c "CREATE DATABASE keycloak;"
kubectl exec -it -n otus postgres-postgresql-0 -- psql -U postgres -c "CREATE USER keycloak WITH PASSWORD 'keycloak';"
kubectl exec -it -n otus postgres-postgresql-0 -- psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;"
```

### 1.4. Выдать права на схему public

Это обязательный шаг для PostgreSQL 15+. Без него Keycloak упадёт с `permission denied for schema public`.

```bash
kubectl exec -it -n otus postgres-postgresql-0 -- psql -U postgres -d keycloak -c "GRANT ALL ON SCHEMA public TO keycloak;"
kubectl exec -it -n otus postgres-postgresql-0 -- psql -U postgres -d keycloak -c "ALTER SCHEMA public OWNER TO keycloak;"
```

### 1.5. Проверить базы

```bash
kubectl exec -it -n otus postgres-postgresql-0 -- psql -U postgres -c "\l"
```

Должны быть базы `otus` и `keycloak`.

## 2. Keycloak

### 2.1. Установить Keycloak

```bash
helm repo add codecentric https://codecentric.github.io/helm-charts
helm repo update
helm install keycloak codecentric/keycloakx \
  -n keycloak \
  -f hw/hw4/helm/keycloak-values.yaml
```

### 2.2. Исправить хост Ingress

Чарт `codecentric/keycloakx` создаёт Ingress с хостом по умолчанию — `keycloak.keycloak.example.com`, игнорируя параметр `ingress.hostname`. Нужно вручную заменить хост на `keycloak.homework`.

Проверить текущий хост:

```bash
kubectl get ingress -n keycloak
```

Если хост не `keycloak.homework` — исправить:

```bash
kubectl patch ingress keycloak-keycloakx -n keycloak --type=json \
  -p='[{"op": "replace", "path": "/spec/rules/0/host", "value": "keycloak.homework"}]'
```

Проверить:

```bash
kubectl get ingress -n keycloak
```

Ожидаемо:

```
NAME                 CLASS   HOSTS               ADDRESS         PORTS   AGE
keycloak-keycloakx   nginx   keycloak.homework   10.97.249.195   80      2m
```

Также добавить хост в `/etc/hosts`, если ещё не добавлен:

```bash
grep keycloak.homework /etc/hosts || echo "10.97.249.195 keycloak.homework" | sudo tee -a /etc/hosts
```

### 2.3. Дождаться готовности

```bash
kubectl get pods -n keycloak
```

Под `keycloak-keycloakx-0` должен быть `1/1 Running`. Первый старт занимает 2–3 минуты.

### 2.4. Проверить доступ

```bash
curl -I http://keycloak.homework/auth/
```

Ожидаемо: `HTTP/1.1 302 Found`.

Обратите внимание: Keycloak живёт под путём `/auth/` — это стандартное поведение чарта `keycloakx`. Все запросы идут на `/auth/...`.

## 3. Настройка Keycloak

### 3.1. Получить admin-токен

```bash
ADMIN_TOKEN=$(curl -s -X POST \
  http://keycloak.homework/auth/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" -d "password=admin" -d "grant_type=password" -d "client_id=admin-cli" \
  | jq -r '.access_token')
```

Токен живёт 5 минут. Если истёк — получить заново.

### 3.2. Создать Realm `otus`

```bash
curl -s -X POST http://keycloak.homework/auth/admin/realms \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"realm": "otus", "enabled": true}'
```

Проверить:

```bash
curl -s http://keycloak.homework/auth/admin/realms \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.[].realm'
```

Ожидаемо: `"master"`, `"otus"`.

### 3.3. Создать Client `otus-users`

```bash
curl -s -X POST http://keycloak.homework/auth/admin/realms/otus/clients \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "otus-users",
    "enabled": true,
    "publicClient": true,
    "directAccessGrantsEnabled": true,
    "standardFlowEnabled": true,
    "redirectUris": ["http://arch.homework/*"],
    "webOrigins": ["http://arch.homework"]
  }'
```

Проверить:

```bash
curl -s http://keycloak.homework/auth/admin/realms/otus/clients \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.[].clientId'
```

Ожидаемо: `"otus-users"` и стандартные клиенты.

### 3.4. Создать роль `user`

```bash
curl -s -X POST http://keycloak.homework/auth/admin/realms/otus/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "user", "description": "Regular user"}'
```

Проверить:

```bash
curl -s http://keycloak.homework/auth/admin/realms/otus/roles \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.[].name'
```

Ожидаемо: `"user"`, `"default-roles-otus"`, `"offline_access"`, `"uma_authorization"`.

### 3.5. Создать тестового пользователя `testuser`

```bash
curl -s -X POST http://keycloak.homework/auth/admin/realms/otus/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "enabled": true,
    "email": "testuser@example.com",
    "firstName": "Test",
    "lastName": "User",
    "credentials": [{
      "type": "password",
      "value": "testpass",
      "temporary": false
    }]
  }'
```

Проверить:

```bash
curl -s http://keycloak.homework/auth/admin/realms/otus/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.[].username'
```

Ожидаемо: `"testuser"`.

### 3.6. Назначить роль `user` пользователю `testuser`

```bash
USER_ID=$(curl -s http://keycloak.homework/auth/admin/realms/otus/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[] | select(.username=="testuser") | .id')

ROLE=$(curl -s http://keycloak.homework/auth/admin/realms/otus/roles/user \
  -H "Authorization: Bearer $ADMIN_TOKEN")

curl -s -X POST http://keycloak.homework/auth/admin/realms/otus/users/$USER_ID/role-mappings/realm \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "[$ROLE]"
```

Проверить:

```bash
curl -s http://keycloak.homework/auth/admin/realms/otus/users/$USER_ID/role-mappings/realm \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.[].name'
```

Ожидаемо: `"default-roles-otus"`, `"user"`.

### 3.7. Проверить токен

```bash
USER_TOKEN=$(curl -s -X POST \
  http://keycloak.homework/auth/realms/otus/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" -d "password=testpass" -d "grant_type=password" -d "client_id=otus-users" \
  | jq -r '.access_token')

echo $USER_TOKEN | cut -d. -f2 | base64 -d 2>/dev/null | jq '.realm_access.roles'
```

Ожидаемо:

```json
[
  "default-roles-otus",
  "offline_access",
  "uma_authorization",
  "user"
]
```

## 4. Примечания

### 4.1. Путь `/auth`

Чарт `codecentric/keycloakx` настраивает Keycloak с `KC_HTTP_RELATIVE_PATH=/auth`. Поэтому все URL'ы идут с префиксом `/auth`:

- `http://keycloak.homework/auth/` — базовый URL
- `http://keycloak.homework/auth/admin/` — админка
- `http://keycloak.homework/auth/realms/otus/...` — OIDC endpoints

### 4.2. Проблема с PostgreSQL 15+

PostgreSQL 15+ отзывает права на схему `public` у всех, кроме владельца. Поэтому после создания базы `keycloak` нужно явно выдать права пользователю `keycloak` на схему `public` (шаг 1.4). Иначе Keycloak упадёт с ошибкой:

```
ERROR: permission denied for schema public
Failed SQL: CREATE TABLE public.databasechangelog ...
```

### 4.3. Проблема с хостом Ingress

Чарт `codecentric/keycloakx` игнорирует параметр `ingress.hostname` и создаёт Ingress с хостом по умолчанию — `keycloak.keycloak.example.com`. Поэтому после установки нужно вручную патчить Ingress (шаг 2.2).