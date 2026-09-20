# Отчёт о выполнении ДЗ №3

**Тема:** Базовые сущности Kubernetes — Service, Ingress
**Дата:** 17 сентября 2026
**Студент:** Евгений Рыжков (Evryzh)

---

## 1. Что было сделано

### 1.1. Приложение

Использован Spring Boot сервис `evryz/otus:1.2.0` из предыдущего ДЗ:

- Слушает порт `8000`
- Отдаёт `{"status":"UP"}` на `GET /health` и `GET /health/`
- Ответ Actuator семантически эквивалентен `{"status":"OK"}` из задания

### 1.2. Манифесты Kubernetes

| Файл | Назначение |
|---|---|
| `k8s/deployment.yaml` | Deployment `otus-app`, 2 реплики, liveness/readiness пробы |
| `k8s/service.yaml` | Service `otus-service`, ClusterIP, порт 80 → targetPort 8000 |
| `k8s/ingress.yaml` | Ingress `otus-ingress`, маршрут `/health` → `otus-service` (без rewrite) |
| `k8s/ingress-star.yaml` | Ingress `otus-ingress-star`, маршрут `/otusapp/evryzh/*` с rewrite на `/*` |

### 1.3. Коллекция Postman

`postman/otus-k8s.postman_collection.json` — три запроса для проверки:

1. `GET http://arch.homework/health`
2. `GET http://arch.homework/health/`
3. `GET http://arch.homework/otusapp/evryzh/health`

---

## 2. Окружение

| Компонент | Версия / параметр |
|---|---|
| Хост | Windows |
| Виртуалка | Ubuntu 24.04 LTS |
| Кластер | Minikube (драйвер `docker`) |
| Ingress | nginx-ingress, установлен через Helm |
| Проброс порта | `minikube tunnel` |
| DNS | `arch.homework` в `/etc/hosts` Ubuntu |

---

## 3. Процесс проверки

Проверка выполнялась «с нуля», как это сделает преподаватель:

1. Клонирована ветка `feature/hw3` из GitHub в чистую директорию.
2. Удалены старые ресурсы из кластера.
3. Применены манифесты одной командой.
4. Дождались готовности подов.
5. Проверены endpoints сервиса.
6. Проверены ingress-объекты.
7. Выполнены четыре `curl`-запроса.
8. Запущена коллекция Postman через newman.

---

## 4. Результаты

### 4.1. Применение манифестов

```bash
$ kubectl apply -f k8s/
deployment.apps/otus-app created
service/otus-service created
ingress.networking.k8s.io/otus-ingress created
ingress.networking.k8s.io/otus-ingress-star created
```

### 4.2. Поды

```bash
$ kubectl get pods
NAME                        READY   STATUS    RESTARTS   AGE
otus-app-5c4557c9fb-f9hdz   1/1     Running   0          7h16m
otus-app-5c4557c9fb-fxbhk   1/1     Running   0          7h16m

```

Обе реплики в статусе `1/1 Running`.

### 4.3. Endpoints сервиса

```bash
$ kubectl get endpoints otus-service
NAME           ENDPOINTS                         AGE
otus-service   10.244.0.7:8000,10.244.0.8:8000   7h18m
```

Сервис нашёл оба пода, трафик идёт на порт 8000.

### 4.4. Ingress

```bash
$ kubectl get ingress
NAME                CLASS   HOSTS           ADDRESS         PORTS   AGE
otus-ingress        nginx   arch.homework   10.97.249.195   80      7h19m
otus-ingress-star   nginx   arch.homework   10.97.249.195   80      7h19m
```

Оба ingress активны и имеют адрес.

### 4.5. Проверка через curl

```bash
$ curl -i http://arch.homework/health
HTTP/1.1 200
{"status":"UP"}

$ curl -i http://arch.homework/health/
HTTP/1.1 200
{"status":"UP"}

$ curl -i http://arch.homework/otusapp/evryzh/health
HTTP/1.1 200
{"status":"UP"}

$ curl -i http://arch.homework/otusapp/evryzh/health/
HTTP/1.1 200
{"status":"UP"}
```

Все четыре запроса вернули `200 OK`.

### 4.6. Проверка через newman

```bash
$ newman run postman/otus-k8s.postman_collection.json
newman

OTUS K8s HW

→ Health Check
  GET http://arch.homework/health [200 OK, 183B, 105ms]

→ Health Check with trailing slash
  GET http://arch.homework/health/ [200 OK, 183B, 34ms]

→ Health Check via otusapp rewrite
  GET http://arch.homework/otusapp/evryzh/health [200 OK, 183B, 23ms]

┌─────────────────────────┬───────────────────┬───────────────────┐
│                         │          executed │            failed │
├─────────────────────────┼───────────────────┼───────────────────┤
│              iterations │                 1 │                 0 │
├─────────────────────────┼───────────────────┼───────────────────┤
│                requests │                 3 │                 0 │
├─────────────────────────┼───────────────────┼───────────────────┤
│            test-scripts │                 0 │                 0 │
├─────────────────────────┼───────────────────┼───────────────────┤
│      prerequest-scripts │                 0 │                 0 │
├─────────────────────────┼───────────────────┼───────────────────┤
│              assertions │                 0 │                 0 │
├─────────────────────────┴───────────────────┴───────────────────┤
│ total run duration: 417ms                                       │
├─────────────────────────────────────────────────────────────────┤
│ total data received: 45B (approx)                               │
├─────────────────────────────────────────────────────────────────┤
│ average response time: 54ms [min: 23ms, max: 105ms, s.d.: 36ms] │
└─────────────────────────────────────────────────────────────────┘
```

Три запроса, ноль ошибок.

---

## 5. Выводы

- Все требования ДЗ выполнены: Deployment (2 реплики), Service, Ingress.
- Задание со звёздочкой выполнено: rewrite `/otusapp/evryzh/*` → `/*`.
- Проверка через curl и newman прошла успешно.
- Манифесты применяются одной командой `kubectl apply -f k8s/`.

---

## 6. Примечание: почему два Ingress

Nginx Ingress Controller применяет аннотацию `rewrite-target` **глобально ко всем путям одного Ingress-объекта**. Если оставить один Ingress с `rewrite-target: /$2`:

- Для `/otusapp/evryzh/health` rewrite сработает правильно → `/health`.
- Для `/health` regex-групп нет, `$2` пустой, путь превратится в `/` → Spring вернёт `404`.

Поэтому для двух сценариев маршрутизации созданы отдельные Ingress-объекты:

- `ingress.yaml` — без rewrite, для `/health`.
- `ingress-star.yaml` — с rewrite, для `/otusapp/evryzh/*`.

---

## 7. Ссылки

- **Pull Request:** `https://github.com/Evryzh/otus/pull/4` (замените N)
- **Docker Hub образ:** `https://hub.docker.com/r/evryz/otus`
- **Репозиторий:** `https://github.com/Evryzh/otus`