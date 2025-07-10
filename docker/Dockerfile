FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . . 
RUN gradle clean bootJar --no-daemon

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
