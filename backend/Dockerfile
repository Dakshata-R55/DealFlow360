# Dealflow360 API image (Spring Boot JDBC + MySQL driver)
FROM gradle:8.12.1-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="dealflow360-backend"
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 18080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
