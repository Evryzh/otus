# ДЗ №3: Основы работы с Kubernetes (Service, Ingress)

## Задание

- Создать минимальный сервис на порту 8000 с методом `GET /health/`
- Собрать Docker-образ и запушить в Docker Hub
- Написать манифесты для Deployment, Service, Ingress
- Количество реплик — не меньше 2
- Хост в Ingress — `arch.homework`
- `GET http://arch.homework/health` → `{"status": "OK"}` (в нашем случае `{"status": "UP"}` от Spring Boot Actuator — семантически эквивалентно)

**Задание со звёздочкой:**
- В Ingress должно быть правило, которое форвардит все запросы с `/otusapp/{student name}/*` на сервис с rewrite-ом пути.
- Пример: `curl arch.homework/otusapp/evryzh/health` → rewrite → `arch.homework/health`

---

## Pull Request

Изменения по этому ДЗ оформлены через Pull Request в ветку `develop`:

- **PR:** `https://github.com/Evryzh/otus/pull/4`
- **Base:** `develop`
- **Compare:** `feature/hw3`

Все манифесты лежат в директории `k8s/`:

```
k8s/
├── deployment.yaml
├── service.yaml
├── ingress.yaml
├── ingress-star.yaml
└── README.md
```

---

## Предварительные требования

Для развёртывания и проверки необходимо:

- Работающий кластер Kubernetes
- Установленный **kubectl**, настроенный на этот кластер
- Установленный **Helm**
- Установленный **nginx-ingress controller**
- DNS-имя `arch.homework` резолвится в адрес, по которому доступен ingress-контроллер
- Ingress-контроллер принимает трафик на порту **80**

## Применение манифестов

```bash
cd k8s
kubectl apply -f .
```

### Проверка

```bash
kubectl get pods
kubectl get svc
kubectl get ingress
```

Дождитесь, пока оба пода `otus-app-*` перейдут в статус `Running 1/1`.

```bash
curl -i http://arch.homework/health
# HTTP/1.1 200 OK
# {"status":"UP"}

curl -i http://arch.homework/health/
# HTTP/1.1 200 OK
# {"status":"UP"}

curl -i http://arch.homework/otusapp/evryzh/health
# HTTP/1.1 200 OK
# {"status":"UP"}

curl -i http://arch.homework/otusapp/evryzh/health/
# HTTP/1.1 200 OK
# {"status":"UP"}
```

### Удаление

```bash
kubectl delete -f .
```

---

## Структура манифестов

| Файл                | Что описывает                                                              |
|---------------------|----------------------------------------------------------------------------|
| `deployment.yaml`   | Deployment `otus-app`, 2 реплики, liveness/readiness пробы                 |
| `service.yaml`      | Service `otus-service` типа ClusterIP, порт 80 → 8000                      |
| `ingress.yaml`      | Ingress `otus-ingress` — маршрут `/health` → `otus-service`                |
| `ingress-star.yaml` | Ingress `otus-ingress-star` — маршрут `/otusapp/evryzh/*` с rewrite на `/` |

### Почему два Ingress?

Nginx Ingress Controller применяет аннотацию `rewrite-target` **глобально ко всем путям одного
Ingress-объекта**. Если оставить один Ingress с `rewrite-target: /$2`:

- Для `/otusapp/evryzh/health` rewrite сработает правильно → `/health`.
- Для `/health` regex-групп нет, `$2` пустой, путь превратится в `/` → Spring вернёт `404`.

Поэтому для двух разных сценариев маршрутизации созданы **два отдельных Ingress-объекта**:
один без rewrite (`/health`), второй с rewrite (`/otusapp/...`).

---

## Проверка через Postman

В репозитории есть коллекция Postman:

```
postman/otus-k8s.postman_collection.json
```

Запуск через newman:

```bash
newman run postman/otus-k8s.postman_collection.json
```