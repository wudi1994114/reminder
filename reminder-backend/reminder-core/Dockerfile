# Use an official OpenJDK runtime as a parent image (adjust version as needed)
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Argument to specify the JAR file name (can be overridden at build time)
ARG JAR_FILE=target/*.jar

# Copy the fat JAR file from the target directory to the container
COPY ${JAR_FILE} app.jar

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the JAR file 
ENTRYPOINT ["java", "-jar", "/app/app.jar"] 