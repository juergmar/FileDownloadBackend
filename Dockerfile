# Build stage: compile and package the Spring Boot app using JDK 21
FROM maven:3.9.4-eclipse-temurin-21-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src/ src/
RUN mvn clean package -DskipTests

# Runtime stage: run the packaged jar using JDK 21
FROM openjdk:21-slim
ENV TZ=Europe/Berlin
RUN apt-get update && apt-get install -y tzdata && \
    ln -fs /usr/share/zoneinfo/Europe/Berlin /etc/localtime && \
    dpkg-reconfigure --frontend noninteractive tzdata && \
    apt-get clean && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
