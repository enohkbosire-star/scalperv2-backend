# Stage 1: Build with Maven
# Build version: 1.0.1
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre
COPY --from=build /target/Fxausd-1.0-SNAPSHOT-shaded.jar app.jar
EXPOSE 4567 8888 8891
# Add memory limits to fit in Render's 512MB Free Tier
ENTRYPOINT ["java", "-Xmx300M", "-Xms128M", "-jar", "/app.jar", "server"]
