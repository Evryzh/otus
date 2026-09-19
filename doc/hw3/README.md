# ДЗ №3: Основы работы с Kubernetes (Service, Ingress)

## Задание

- Создать минимальный сервис на порту 8000 с методом `GET /health/`
- Собрать Docker-образ, запушить в Docker Hub
- Написать манифесты для Deployment, Service, Ingress
- Реплик — не меньше 2
- Хост в Ingress — `arch.homework`
- `GET http://arch.homework/health` → `{"status": "OK"}`

**Задание со звёздочкой:**
- Правило в Ingress: `/otusapp/{student name}/*` → rewrite → `/*`
- Пример: `curl arch.homework/otusapp/evryzh/health` → `arch.homework/health`

---

## Что сделано

### Приложение

- Spring Boot сервис `evryz/otus:1.2.0` (уже собран в предыдущем ДЗ)
- Слушает порт `8000`
- Отдаёт `{"status":"UP"}` на `GET /health` и `GET /health/` (Spring Boot Actuator, семантически эквивалентно `{"status":"OK"}`)

### Манифесты (`k8s/`)

| Файл                | Что описывает                                                     |
|---------------------|-------------------------------------------------------------------|
| `deployment.yaml`   | Deployment `otus-app`, 2 реплики, liveness/readiness пробы        |
| `service.yaml`      | Service `otus-service`, ClusterIP, 80 → 8000                      |
| `ingress.yaml`      | Ingress `otus-ingress`, `/health` → сервис (без rewrite)          |
| `ingress-star.yaml` | Ingress `otus-ingress-star`, `/otusapp/evryzh/*` → rewrite → `/*` |

### Postman

- `postman/otus-k8s.postman_collection.json` — проверка трёх эндпоинтов через newman

### Почему два Ingress

Nginx Ingress Controller применяет `rewrite-target` **глобально ко всем путям одного Ingress-объекта**. Если оставить один Ingress с `rewrite-target: /$2`:

- Для `/otusapp/evryzh/health` rewrite сработает правильно → `/health`.
- Для `/health` regex-групп нет, `$2` пустой → путь станет `/` → Spring вернёт `404`.

Поэтому два отдельных Ingress: один без rewrite, второй с rewrite.

---

## Как развернуть в моём окружении

### Окружение

- **Хост:** Windows (там проект, git, GitHub)
- **Виртуалка:** Ubuntu (там Docker, minikube, kubectl, helm)
- **Кластер:** Minikube внутри Ubuntu, драйвер `docker`
- **Ingress:** nginx-ingress, установленный через Helm
- **Сеть:** `arch.homework` прописан в `/etc/hosts` Ubuntu на IP minikube; `minikube tunnel` пробрасывает порт 80

### 1. Запустить виртуалку Ubuntu

Открыть VirtualBox/VMware/Hyper-V → запустить Ubuntu → залогиниться.

### 2. Проверить, что minikube жив

```bash
minikube status
```

Если не запущен:

```bash
minikube start --driver=docker
```

Проверить ноды:

```bash
kubectl get nodes
```

### 3. Проверить, что nginx-ingress установлен

```bash
kubectl get pods -n m
```

Должен быть `nginx-ingress-nginx-controller-...` в статусе `Running`.

Если namespace `m` пустой — переустановить:

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx/
helm repo update
kubectl create namespace m
helm install nginx ingress-nginx/ingress-nginx --namespace m
```

### 4. Проверить `/etc/hosts`

```bash
cat /etc/hosts | grep arch.homework
```

Должно быть что-то вроде:

```
10.97.249.195 arch.homework
```

Если пусто — узнать EXTERNAL-IP ingress-контроллера:

```bash
kubectl get svc -n m nginx-ingress-nginx-controller
```

И прописать:

```bash
echo "<EXTERNAL-IP> arch.homework" | sudo tee -a /etc/hosts
```

### 5. Запустить `minikube tunnel` (в отдельном терминале)

```bash
minikube tunnel
```

Ввести пароль sudo. Оставить терминал открытым — туннель должен работать всё время.

Проверить в **первом** терминале:

```bash
kubectl get svc -n m
```

`EXTERNAL-IP` у `nginx-ingress-nginx-controller` должен быть `10.97.249.195` (не `<pending>`).

### 6. Склонировать ветку `feature/hw3` из GitHub

```bash
cd ~
rm -rf otus-check
git clone -b feature/hw3 https://github.com/Evryzh/otus.git otus-check
cd otus-check
```

### 7. Применить манифесты

```bash
kubectl apply -f k8s/
```

Проверить:

```bash
kubectl get pods
kubectl get svc otus-service
kubectl get ingress
```

Дождаться `Running 1/1` для обоих `otus-app-*`.

### 8. Проверить через curl

```bash
curl http://arch.homework/health
curl http://arch.homework/health/
curl http://arch.homework/otusapp/evryzh/health
curl http://arch.homework/otusapp/evryzh/health/
```

Все четыре должны вернуть `{"status":"UP"}`.

### 9. Проверить через newman

Установить newman (если ещё нет):

```bash
sudo apt update
sudo apt install -y nodejs npm
sudo npm install -g newman
```

Запустить:

```bash
newman run postman/otus-k8s.postman_collection.json
```

Ожидаемо: 3 запроса, 0 ошибок.

### 10. Удалить манифесты (когда не нужны)

```bash
kubectl delete -f k8s/
```

---

## Полезные команды

```bash
# Проверить резолвинг
getent hosts arch.homework

# Посмотреть endpoints сервиса
kubectl get endpoints otus-service

# Логи ingress-контроллера
kubectl logs -n m -l app.kubernetes.io/component=controller --tail=50

# Логи приложения
kubectl logs -l app=otus-app --tail=50
```