ДЗ: Основы работы с Docker
Занятие 9. Микросервисная архитектура

===========================================
1. Docker Hub
   ===========================================
   Репозиторий: evryz/otus
   Тег:         1.2.0
   Полное имя:  evryz/otus:1.2.0
   Ссылка:      https://hub.docker.com/r/evryz/otus

===========================================
2. GitHub
   ===========================================
   Репозиторий:  https://github.com/Evryzh/otus
   Dockerfile:   https://github.com/Evryzh/otus/blob/develop/Dockerfile

===========================================
3. Проверка работоспособности
   ===========================================
   $ docker pull evryz/otus:1.2.0
   Digest: sha256:02f51ea5f312d4781d3ea5b37217123a3c22785ba690c93274479b900edc76b0
   Status: Downloaded newer image for evryz/otus:1.2.0

$ docker run --rm -p 8000:8000 evryz/otus:1.2.0
Tomcat started on port 8000 (http)
Started OtusApplication in X.XXX seconds

$ curl -i http://localhost:8000/health/
HTTP/1.1 200
Content-Type: application/vnd.spring-boot.actuator.v3+json
{"status":"UP"}

===========================================
4. Примечания
   ===========================================
- Spring Boot 4.1.1, Java 21
- Эндпоинт /health/ реализован штатным Spring Boot Actuator,
  собственный контроллер не создавался.
- Actuator возвращает {"status":"UP"} — стандартный контракт,
  семантически эквивалентный {"status":"OK"} из задания.
- Образ собран multi-stage под linux/amd64:
  docker build --platform linux/amd64 -t evryz/otus:1.2.0 .
- Базовые образы:
    - build:  maven:3.9.16-eclipse-temurin-21
    - runtime: eclipse-temurin:21-jre-alpine
- Размер образа: ~93.9 MB (compressed), ~328 MB (on disk).