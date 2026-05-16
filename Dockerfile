# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests
# Find the program file and rename it to app.jar
RUN cp target/*-shaded.jar target/app.jar || cp target/Fxausd-*.jar target/app.jar

# Stage 2: Run the application
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy the file we just renamed
COPY --from=build /app/target/app.jar app.jar
EXPOSE 4567 8888 8891
# Memory limits to fit Render's Free Tier
ENTRYPOINT ["java", "-Xmx300M", "-Xms128M", "-jar", "app.jar", "live"]
