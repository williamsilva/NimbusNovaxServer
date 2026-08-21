# syntax=docker/dockerfile:1
# Nota: se um deploy no Railway falhar na inicialização com algo como
# "ls: cannot access '*/build/libs/*jar'" mesmo com o build acima concluído com sucesso, é cache
# de build corrompido/parcial no Railway (não um problema deste Dockerfile - ENTRYPOINT abaixo
# nunca chama ls) - geralmente após uma tentativa anterior falhar no meio do processo. Um commit
# novo (mesmo pequeno, tocando este arquivo) força invalidar o cache e resolve.
#
# Nota 2 (2026-08-18): também já vimos o Build > Build image falhar direto, sem log nenhum na aba
# Build Logs e sem botão de redeploy/retry na UI, num commit que builda limpo local (Gradle +
# docker compose build) - mesmo diagnóstico (infra do Railway, não este Dockerfile/código). Fix
# idêntico: um commit novo pra forçar uma tentativa de build do zero.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --version
COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
