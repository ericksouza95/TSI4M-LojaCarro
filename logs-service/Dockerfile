# Microserviço de logs de auditoria
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 logs
COPY --from=build /src/target/logs-service-0.0.1-SNAPSHOT.jar app.jar
USER logs
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-Xms96m -Xmx192m"
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
