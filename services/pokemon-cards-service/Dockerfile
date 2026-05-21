FROM gradle:9.3.1-jdk21-corretto-al2023 AS builder
WORKDIR /app
COPY . .
RUN gradle shadowJar --no-daemon

FROM amazoncorretto:21-al2023
WORKDIR /app
COPY --from=builder /app/build/libs/app.jar app.jar
EXPOSE 50051
CMD ["java", "-jar", "app.jar"]