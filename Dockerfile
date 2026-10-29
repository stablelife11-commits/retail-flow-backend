# Stage 1: Build the application JAR with Maven & JDK 21
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests

# Stage 2: Minimal JRE 21 Runtime Image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create a non-root user for security
RUN groupadd -r retailflow && useradd -r -g retailflow retailflow
COPY --from=builder /app/target/*.jar app.jar
RUN chown retailflow:retailflow app.jar
USER retailflow

EXPOSE 8080

# JVM memory flags tuned for cloud containers (512MB - 1GB RAM)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]