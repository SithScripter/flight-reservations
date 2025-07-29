# Java 21 on Alpine
FROM bellsoft/liberica-openjdk-alpine:21.0.6

# Install Maven and other tools
RUN apk add --no-cache curl jq maven

# Set working directory
WORKDIR /home/flight-reservations

# Copy POM
COPY pom.xml .

# Copy source and resources
COPY src ./src
COPY test-suites ./test-suites
COPY test-data ./test-data
COPY config ./config
COPY allure.properties .
COPY libs ./libs  # If any extra JARs are needed
