# syntax=docker/dockerfile:1.7

FROM gradle:8.8-jdk21 AS build
WORKDIR /workspace
COPY BE/LinguageFlowPOC/gradlew BE/LinguageFlowPOC/gradlew
COPY BE/LinguageFlowPOC/gradle BE/LinguageFlowPOC/gradle
COPY BE/LinguageFlowPOC/build.gradle BE/LinguageFlowPOC/settings.gradle BE/LinguageFlowPOC/
COPY BE/LinguageFlowPOC/src BE/LinguageFlowPOC/src
WORKDIR /workspace/BE/LinguageFlowPOC
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/BE/LinguageFlowPOC/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
