# Use a Java runtime image.
FROM bellsoft/liberica-openjdk-alpine:21.0.6

# Set the working directory
WORKDIR /home/flight-reservations

# Copy the application JAR from the build stage (assuming it's named flight-reservations-1.0-SNAPSHOT.jar)
# Your Jenkinsfile's `mvn package` command produces this.
COPY target/flight-reservations-1.0-SNAPSHOT.jar .

# Define the command to run your application.
# If your application is a Spring Boot app, it might be something like this.
# ENTRYPOINT ["java", "-jar", "flight-reservations-1.0-SNAPSHOT.jar"]