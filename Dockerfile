FROM eclipse-temurin:17-jdk

# Install Git for GitHub clone feature
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy JAR file
COPY target/codevision-backend-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]