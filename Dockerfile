FROM eclipse-temurin:17-jdk

# Install Git
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

VOLUME /tmp
COPY target/codevision-backend-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]