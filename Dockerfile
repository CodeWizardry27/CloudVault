# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-22 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the application, skipping tests to speed up deployment
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:22-jre-jammy
WORKDIR /app
# Copy the built jar file from the build stage
COPY --from=build /app/target/*.jar app.jar
# Expose the port (Render automatically maps port 10000 or reads the PORT env variable)
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=${PORT:8080}"]
