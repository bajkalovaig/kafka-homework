# Сборка приложения
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY ./eventTracker/ .
RUN mvn clean package

# Запуск приложения
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache postgresql-client bash openssl libgcc libstdc++
WORKDIR /app

COPY --from=builder /app/target/eventTracker-1.0-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]