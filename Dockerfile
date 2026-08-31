# syntax=docker/dockerfile:1.7
FROM gradle:9.7.0-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle gradle.properties ./
COPY src ./src
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
