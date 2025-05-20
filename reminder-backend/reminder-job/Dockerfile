# Use an official OpenJDK runtime as a parent image (adjust version as needed)
FROM docker-0.unsee.tech/openjdk:8-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Argument to specify the JAR file name
ARG JAR_FILE=target/*.jar

# Copy the fat JAR file
COPY ${JAR_FILE} app.jar

# Expose port (if reminder-job runs on a specific port, e.g., if it has web endpoints for management)
# If it's a pure background job runner without HTTP endpoints, this EXPOSE might not be strictly necessary
# but doesn't hurt. Assuming 8081 if it needs a separate port from core, or remove if not needed.
# EXPOSE 8081 

# Run the JAR file
ENTRYPOINT ["java", "-jar", "/app/app.jar"] 