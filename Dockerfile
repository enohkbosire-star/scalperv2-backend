# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests
# Find any shaded or main jar and rename it to target/app.jar
RUN find target -name "*-shaded.jar" -exec mv {} target/app.jar \; || mv target/*.jar target/app.jar

# Stage 2: Run the application
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
EXPOSE 4567 8888 8891
# Add memory limits to fit in Render's 512MB Free Tier
ENTRYPOINT ["java", "-Xmx300M", "-Xms128M", "-jar", "app.jar", "server"]
