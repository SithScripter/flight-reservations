# Use the small and efficient Alpine base image with Java 21 [cite: 24]
FROM bellsoft/liberica-openjdk-alpine:21.0.6

# Install only the tools needed for the health check [cite: 24]
RUN apk add --no-cache curl jq

# Set the working directory inside the container [cite: 24]
WORKDIR /home/flight-reservations

# Copy the pre-packaged test resources, dependencies, and test JAR.
# This 'docker-resources' directory is created by Maven during the build.
COPY target/docker-resources/ .