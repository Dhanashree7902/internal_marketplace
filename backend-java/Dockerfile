# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN useradd --system --create-home appuser
COPY --from=build /workspace/target/internal-marketplace-api.jar app.jar
USER appuser

# Cloud Run injects PORT; server.port in application.yml already binds to it.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
