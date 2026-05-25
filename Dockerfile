# INSTITUTIONAL CLOUD DEPLOYMENT FOR FXAUSD
FROM openjdk:21-slim

# Copy the built JAR from NetBeans dist folder
# Note: Ensure you run "Clean and Build" in NetBeans
COPY target/*.jar app.jar

# Dynamic port assignment for cloud environments
EXPOSE 4567

# Run the Institutional Scalper
ENTRYPOINT ["java", "-jar", "/app.jar", "live"]
