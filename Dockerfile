# Use the small and efficient Alpine base image with Java 21
FROM bellsoft/liberica-openjdk-alpine:21.0.6

# Install curl (needed for downloading) and Maven
RUN apk add --no-cache curl
ENV MAVEN_VERSION=3.9.6
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | \
    tar -xz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven
ENV MAVEN_HOME=/opt/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

# Set the working directory inside the container
WORKDIR /home/flight-reservations

# Copy the pre-built JARs, dependencies, and test resources
# This uses the output from your 'mvn package' step
ADD target/docker-resources ./