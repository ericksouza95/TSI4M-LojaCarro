# API da Loja de Carros (Spring Boot)
# Etapa 1: compila o projeto com Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagem final só com o Java (JRE) e o .jar
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 loja && mkdir -p /app/img /app/logs && chown -R loja /app
COPY --from=build /src/target/LojaCarro-0.0.1-SNAPSHOT.jar app.jar
USER loja
ENV SPRING_PROFILES_ACTIVE=prod \
    IMAGENS_DIR=/app/img \
    JAVA_TOOL_OPTIONS="-Xms128m -Xmx320m"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
