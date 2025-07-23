FROM bellsoft/liberica-openjdk-alpine:21.0.6

# Install curl jq
RUN apk add curl jq

# workspace
WORKDIR /home/selenium-docker

# Add the required files to run the test
ADD target/docker-resources ./
#ADD https://raw.githubusercontent.com/vinsguru/selenium-docker/master/06-jenkins-ci-cd/selenium-docker/runner.sh runner.sh
#ADD runner.sh runner.sh
ADD runner.sh runner.sh

# Environment variables to be added
# BROWSER
# HUB_HOST
# TEST_SUITE
# THREAD_COUNT

# Run the tests (remember in linux, if the command is bit lengthy it can be broken in next line using \ which tells that
# it's just a single command-check below

#ENTRYPOINT java -cp 'libs/*' \
#                -Dselenium.grid.enabled=true \
#                -Dselenium.grid.hubHost=${HUB_HOST} \
#                -Dbrowser=${BROWSER} \
#                org.testng.TestNG \
#                -threadcount ${THREAD_COUNT} \
#                test-suites/${TEST_SUITE}

# to recitfy warning using sinle command-but din't work same thing-use any...well no need of this as well, as
#we will use runner.sh

#ENTRYPOINT java -cp 'libs/*' -Dselenium.grid.enabled=true -Dselenium.grid.hubHost=${HUB_HOST} -Dbrowser=${BROWSER} org.testng.TestNG -threadcount ${THREAD_COUNT} test-suites/${TEST_SUITE}

#if any issue with runner.sh in windows then use below
# Fix for windows
RUN dos2unix runner.sh

#start the runner
ENTRYPOINT sh runner.sh

# grok recommended solution for issues that i faced
# Fix for Windows line endings
#RUN dos2unix /home/selenium-docker/runner.sh

# Ensure the script is executable
#RUN chmod +x /home/selenium-docker/runner.sh

# Start the runner.sh
#ENTRYPOINT ["sh", "/home/selenium-docker/runner.sh"]