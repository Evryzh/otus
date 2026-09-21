# otus

Репозиторий домашних заданий курса OTUS «Архитектура и шаблоны проектирования».

**Студент:** Евгений Рыжков (Evryzh)

---

## Содержание

| ДЗ  | Тема                                                    | Директория | Статус      |
|-----|---------------------------------------------------------|------------|-------------|
| HW1 | Паттерны декомпозиции микросервисов                     | hw/hw1/    | ✅          |
| HW2 | Простейший REST-сервис + health                         | hw/hw2/    | ✅          |
| HW3 | Основы работы с Kubernetes (Service, Ingress)           | hw/hw3/    | ✅          |
| HW4 | Инфраструктурные паттерны (K8s + Keycloak + PostgreSQL) | hw/hw4/    | 🚧 в работе |

---

## HW1 — Паттерны декомпозиции микросервисов

Кейс: интернет-магазин.

- Метод декомпозиции: бизнес-возможности (Business Capabilities) с уточнением через DDD.
- Сервисы: UserService, ProductService, OrderService, PaymentService, NotificationService.
- Артефакты: пользовательские сценарии (US-1…US-7), ER-модель предметной области, контейнерная диаграмма C4, описание сервисов и контрактов взаимодействия.
- Архитектурные требования: брокер сообщений (Kafka/RabbitMQ), паттерн Saga (хореография), идемпотентность платежей, Keycloak + API Gateway, ELK/Prometheus/Jaeger.

Решение: hw/hw1/

---

## HW2 — Простейший REST-сервис + health

Spring Boot-приложение с эндпоинтом GET /health/.

- Образ: evryz/otus:1.2.0
- Порт: 8000
- Ответ: {"status":"UP"} (Actuator, семантически эквивалентно {"status":"OK"})

---

## HW3 — Основы работы с Kubernetes (Service, Ingress)

Минимальный сервис на порту 8000 с GET /health/, задеплоенный в Minikube.

- Манифесты: Deployment (2 реплики), Service (ClusterIP 80→8000), Ingress на arch.homework.
- Задание со звёздочкой: rewrite /otusapp/evryzh/* → /* (выполнено через отдельный Ingress).
- Проверка: curl + newman (3 запроса, 0 ошибок).

Решение: hw/hw3/

---

## HW4 — Инфраструктурные паттерны

RESTful CRUD сервис профиля пользователя на Spring Boot с интеграцией Keycloak и PostgreSQL.

Стек:
- Java 21, Spring Boot 4.1.1
- PostgreSQL 16 (Flyway-миграции)
- Keycloak 26 (OAuth2 Resource Server, Admin API)
- Docker, Kubernetes (Minikube), Helm, nginx-ingress
- MapStruct, Lombok, springdoc-openapi

Документация:
- Инструкция по развёртыванию: hw/hw4/doc/README.md
- Установка PostgreSQL и Keycloak: hw/hw4/helm/README.md
- Отчёт с результатами: hw/hw4/doc/REPORT.md

Быстрые ссылки:
- Docker Hub: evryz/otus-users
- Ветка: feature/hw4

---

## Структура репозитория

otus/
├── src/                    # Java-код приложения (HW2/HW4)
├── hw/
│   ├── hw1/                # ДЗ №1: декомпозиция микросервисов
│   ├── hw2/                # ДЗ №2: Простейший REST-сервис + health
│   ├── hw3/                # ДЗ №3: Kubernetes (Service, Ingress)
│   └── hw4/                # ДЗ №4: K8s + Keycloak + PostgreSQL
├── config/                 # .env.example
├── pom.xml
└── release-notes.md

---

## Лицензия

MIT