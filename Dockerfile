# Use the small and efficient Alpine base image with Java 21
FROM bellsoft/liberica-openjdk-alpine:21.0.6

# Install necessary tools: curl, jq for health check, and Maven for running tests
RUN apk add --no-cache curl jq maven

# Set the working directory inside the container
WORKDIR /home/flight-reservations

# Copy the pom.xml file - Maven needs this to understand the project
COPY pom.xml .

# Copy the entire src directory. Maven will find main/java, main/resources,
# test/java, and test/resources within this, including your allure.properties.
COPY src ./src

# IMPORTANT: Ensure ALL 'COPY' lines for allure.properties, config, test-data,
# test-suites, and libs (if it's not at the root) are REMOVED from your Dockerfile
# if they were previously there. The 'COPY src ./src' now handles them.
# Example of lines to ENSURE ARE REMOVED:
# # COPY test-suites ./test-suites
# # COPY test-data ./test-data
# # COPY config ./config
# # COPY allure.properties .
# # COPY libs ./libs # Unless you have external JARs here at the project root