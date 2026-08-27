# syntax=docker/dockerfile:1.7

FROM maven:3-eclipse-temurin-26-alpine AS build
WORKDIR /workspace

# Cache de dependências separado do código para builds reproduzíveis e rápidos.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress verify

# Distroless não inclui shell, package manager ou utilitários exploráveis.
FROM gcr.io/distroless/java21-debian13:nonroot AS runtime
WORKDIR /app

LABEL org.opencontainers.image.title="itau-teste" \
      org.opencontainers.image.description="Secure transaction statistics API" \
      org.opencontainers.image.source="https://github.com/YggorMartins/itau-teste"

COPY --from=build --chown=65532:65532 /workspace/target/itau-teste-*.jar /app/app.jar

USER 65532:65532
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-Djava.io.tmpdir=/tmp", "-jar", "/app/app.jar"]
