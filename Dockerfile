# Use the small and efficient Alpine base image with Java 21
FROM bellsoft/liberica-openjdk-alpine:21.0.6

# ✅ ADD THESE TOOLS for the health check
RUN apk add --no-cache curl jq

# Set the working directory inside the container
WORKDIR /home/flight-reservations

# Copy the pom.xml file
COPY pom.xml .

# Copy the pre-built JARs, dependencies, and test resources
ADD target/docker-resources ./