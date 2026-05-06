# ---------- build stage ----------
# Maven-образ с OpenJDK 21 — и компилятор, и инструмент сборки в одном.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# В Docker Compose build.context: . (корень репозитория).
# Parent pom ссылается на все модули → чтобы реактор не падал,
# копируем ВСЕ pom'ы модулей перед сборкой одного.
COPY pom.xml .
COPY accounts-service/pom.xml accounts-service/
COPY cash-service/pom.xml cash-service/
COPY transfer-service/pom.xml transfer-service/
COPY notifications-service/pom.xml notifications-service/
COPY front/pom.xml front/
# Теперь копируем исходники только нужного модуля и собираем.
COPY accounts-service/src accounts-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl accounts-service -am -Dmaven.test.skip=true clean package -B

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/accounts-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
