# Multi-stage build para backend Spring Boot
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copiamos pom.xml primero para aprovechar cache de dependencias
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:resolve dependency:resolve-plugins
# Copiamos el código fuente
COPY src ./src
# Empaquetamos (salta tests en imagen de build)
RUN mvn -q -DskipTests package

# Imagen ligera para runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copiamos el JAR (usa nombre artefacto + versión)
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
# Puerto de la aplicación
EXPOSE 8080
# Variables opcionales pueden sobreescribirse en docker-compose
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
