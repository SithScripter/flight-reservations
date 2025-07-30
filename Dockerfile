# Use the small and efficient Alpine base image with Java 21
FROM bellsoft/liberica-openjdk-alpine:21.0.6

# Set the working directory inside the container
WORKDIR /home/flight-reservations

# Copy the application JAR from the build stage
COPY target/docker-resources/libs/flight-reservations.jar .

# Define the command to run your application.
# ENTRYPOINT ["java", "-jar", "flight-reservations.jar"]