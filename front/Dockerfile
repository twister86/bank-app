FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/
COPY config-server/pom.xml config-server/
COPY gateway/pom.xml gateway/
COPY accounts-service/pom.xml accounts-service/
COPY cash-service/pom.xml cash-service/
COPY transfer-service/pom.xml transfer-service/
COPY notifications-service/pom.xml notifications-service/
COPY front/pom.xml front/
COPY front/src front/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl front -am -Dmaven.test.skip=true clean package -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/front/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
