#!/bin/bash

#-------------------------------------------------------------------
#  This script expects the following environment variables
#     HUB_HOST
#     BROWSER
#     THREAD_COUNT
#     TEST_SUITE
#-------------------------------------------------------------------

# Let's print what we have received
echo "-------------------------------------------"
echo "HUB_HOST      : ${HUB_HOST:-hub}"
echo "BROWSER       : ${BROWSER:-chrome}"
echo "THREAD_COUNT  : ${THREAD_COUNT:-1}"
echo "TEST_SUITE    : ${TEST_SUITE}"
echo "-------------------------------------------"

# Do not start the tests immediately. Hub has to be ready with browser nodes
echo "Checking if hub is ready..!"
count=0
while [ "$( curl -s http://${HUB_HOST:-hub}:4444/status | jq -r .value.ready )" != "true" ]
do
  count=$((count+1))
  echo "Attempt: ${count}"
  if [ "$count" -ge 30 ]
  then
      echo "**** HUB IS NOT READY WITHIN 30 SECONDS ****"
      exit 1
  fi
  sleep 1
done

## Check if enough nodes of the specified browser are available
#echo "Checking if ${BROWSER:-chrome} nodes are available..!"
#count=0
#while true
#do
#  # Debug: Print the full curl command and output
#  echo "Running: curl -s http://${HUB_HOST:-hub}:4444/status"
#  curl -s http://${HUB_HOST:-hub}:4444/status > /tmp/status.json
#  echo "Raw status output:"
#  cat /tmp/status.json | jq .
#
#  # Ensure BROWSER has no trailing spaces
#  BROWSER_CLEAN=$(echo "${BROWSER:-chrome}" | tr -d '[:space:]')
#  jq_filter=".value.nodes[] | select(.stereotype.browserName == \"$BROWSER_CLEAN\") | .slots[] | select(.session == null) | .id"
#  echo "JQ filter: $jq_filter"
#  available_nodes=$(cat /tmp/status.json | jq -r "$jq_filter" | wc -l)
#  echo "Available ${BROWSER_CLEAN} nodes: $available_nodes"
#
#  if [ "$available_nodes" -ge "${THREAD_COUNT:-1}" ]
#  then
#    echo "Found $available_nodes ${BROWSER_CLEAN} nodes, proceeding with tests..."
#    break
#  fi
#  count=$((count+1))
#  echo "Attempt: ${count}"
#  if [ "$count" -ge 30 ]
#  then
#      echo "**** NOT ENOUGH ${BROWSER_CLEAN} NODES AVAILABLE WITHIN 30 SECONDS ****"
#      exit 1
#  fi
#  sleep 1
#done

# At this point, selenium grid should be up!
echo "Selenium Grid is up and running. Running the test...."

# Start the java command
java -cp 'libs/*' \
     -Dselenium.grid.enabled=true \
     -Dselenium.grid.hubHost="${HUB_HOST:-hub}" \
     -Dbrowser="${BROWSER:-chrome}" \
     org.testng.TestNG \
     -threadcount "${THREAD_COUNT:-1}" \
     test-suites/"${TEST_SUITE}"

#below is optional-used it for debugging test results as output directory in local was empty
# Debug - Check if results are generated in the expected directory
echo "Checking Allure results in /home/flight-reservations/target/allure-results:"
ls -l /home/flight-reservations/target/allure-results