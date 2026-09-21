# ============================================================
# Этап 1: Сборка JAR
# ============================================================
FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /app

# Сначала копируем только pom.xml, чтобы закешировать зависимости Maven.
# Если pom.xml не менялся — слой с зависимостями берётся из кеша.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Теперь копируем исходники и собираем JAR.
COPY src ./src
RUN mvn -B clean package -DskipTests

# ============================================================
# Этап 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Копируем только готовый JAR из этапа сборки.
COPY --from=build /app/target/otus-1.4.0.jar app.jar

# Открываем порт 8000 (документация, реально порт задаётся в application.yaml).
EXPOSE 8000

# Запуск приложения.
ENTRYPOINT ["java", "-jar", "app.jar"]