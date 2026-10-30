# Base manifests are pinned; refresh deliberately and verify in CI.
FROM node:22.23.4-bookworm-slim@sha256:4b150b48b4116e2a22f2c39cc27025fbf4f153e2078fd3b14d159fdb5c7279da AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/public ./public
COPY frontend/src ./src
# Preserve the existing CRA warning policy; tests run independently in CI.
RUN CI=false npm run build

FROM eclipse-temurin:21-jdk-jammy@sha256:4b1a74fbffbe6b4e7b6f0171a472478282fb463790a51edf10e465d578f22b69 AS backend
WORKDIR /build
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
COPY src/main ./src/main
COPY --from=frontend /build/frontend/build ./frontend/build
# Tests are mandatory in CI before this separate packaging build.
RUN sh ./mvnw --batch-mode --no-transfer-progress -Dmaven.test.skip=true -Pproduction-package package

FROM eclipse-temurin:21-jre-jammy@sha256:1eac8b8fee11ca7d6725a48bc5d41992ed1561504ebae25490d2d062847e8269 AS runtime
RUN groupadd --system --gid 10001 dixit \
    && useradd --system --uid 10001 --gid dixit --no-create-home --home-dir /app --shell /usr/sbin/nologin dixit
WORKDIR /app
COPY --from=backend --chown=root:root /build/target/*.jar ./app.jar
USER 10001:10001
EXPOSE 8080
STOPSIGNAL SIGTERM
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50.0", "-XX:InitialRAMPercentage=10.0", "-jar", "/app/app.jar"]
