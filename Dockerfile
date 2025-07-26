# Use the small and efficient Alpine base image with Java 21
FROM bellsoft/lib-erica-openjdk-alpine:21.0.6

# Set the working directory inside the container
WORKDIR /home/flight-reservations

# Copy the pom.xml file (needed for classpath resolution)
COPY pom.xml .

# Copy the pre-built JARs, dependencies, and test resources
ADD target/docker-resources ./