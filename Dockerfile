# PROFESSIONAL INSTITUTIONAL DEPLOYMENT
# Stage 1: Build the JAR inside Docker to ensure consistency
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the built JAR
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/Fxausd-1.0-SNAPSHOT.jar app.jar
COPY data ./data
COPY *.bin ./

# Port for Spark API
EXPOSE 4567

# Start the Automatic Scalper
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar", "live"]
