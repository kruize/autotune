#!/bin/bash
#
# Copyright (c) 2025 Red Hat, IBM Corporation and others.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Default values
CLUSTER_TYPE="ephemeral"
KRUIZE_IMAGE="quay.io/khansaad/autotune_operator"
KRUIZE_IMAGE_TAG="kafka3"
CLOWDAPP_FILE="./ros-ocp-backend/kruize-clowdapp.yaml"
BONFIRE_CONFIG_FILE="$HOME/.config/bonfire/config.yaml"
current_dir="$(dirname "$0")"
LOG_FILE="${current_dir}/kafka-demo.log"
# Define Kafka version and file URL
KAFKA_VERSION="3.9.0"
SCALA_VERSION="2.13"
KAFKA_TGZ="kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz"
KAFKA_DIR="${KAFKA_ROOT_DIR}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}"
KAFKA_URL="https://dlcdn.apache.org/kafka/${KAFKA_VERSION}/${KAFKA_TGZ}"

start_demo=1
skip_namespace_reservation=0
repo_name=ros-ocp-backend

set -euo pipefail  # Enable strict error handling

#source ${common_dir}/common_helper.sh
#source ${current_dir}/../common.sh

function usage() {
	echo "Usage: $0 [-s|-t] [-i kruize-image] [-u datasource-url] [-d datasource-name]"
	echo "s = start (default), t = terminate"
	echo "i = Kruize image (default: $KRUIZE_IMAGE)"
	echo "c = Cluster type (default: ephemeral)"
	echo "u = Prometheus/Thanos datasource URL (default: $DATASOURCE_URL)"
	echo "d = Name of the datasource (default: $DATASOURCE_NAME)"
	exit 1
}

function check_err() {
	err=$?
	if [ ${err} -ne 0 ]; then
		echo "❌ $*"
		if [[ -f "${LOG_FILE}" ]]; then
			echo "For detailed logs, look in ${LOG_FILE}"
		fi
		exit 1
	fi
}

# Function to handle errors
error_exit() {
    echo "❌ Error: $1"
    exit 1
}

# Check if bonfire is installed
function check_bonfire() {
if ! command -v bonfire &>/dev/null; then
    error_exit "Bonfire tool is not installed. Please install it before running this script."
fi
}

# get date in format
function get_date() {
	date "+%Y-%m-%d %H:%M:%S"
}

# Setup local kafka for message consumption
function setup_kafka_local() {
  KAFKA_ROOT_DIR=${PWD}

  if java -version &> /dev/null; then
      echo "Java is already installed."
      java -version
  else
      echo "Install java"
      exit 1
  fi

  ZOOKEEPER_LOG="${KAFKA_ROOT_DIR}/zookeeper.log"
  KAFKA_SERVER_LOG="${KAFKA_ROOT_DIR}/kafka_server.log"

  # Check if the file exists
  if [ ! -f "$KAFKA_TGZ" ]; then
      echo "${KAFKA_TGZ} does not exist. Downloading..."
      wget -q ${KAFKA_URL}

      if [ $? -ne 0 ]; then
          echo "Failed to download $KAFKA_TGZ. Exiting."
          exit 1
      fi
  else
      echo "$KAFKA_TGZ already exists. Skipping download."
  fi

  echo "Extracting Kafka tgz..."
  tar zxf ${KAFKA_TGZ} -C ${KAFKA_ROOT_DIR}

  echo "Kafka setup completed!"
  echo
}

# Check if the cluster_type is one of icp or openshift
function check_cluster_type() {
	case "${cluster_type}" in ephemeral) ;;
	*)
		echo "Error: unsupported cluster type: ${cluster_type}"
		echo "Currently only ephemeral cluster is supported"
		exit -1
		;;
	esac
}

# replace the kruize-clowder file in the repo with the local one
function clowder_file_replace() {
	echo -n "🔄 Replacing 'kruize-clowdapp' in the cloned repo..."
	cp "./kruize-clowdapp.yaml" "$repo_name/kruize-clowdapp.yaml"  || { echo "❌ Failed to replace the file."; exit 1; }
	echo "✅ Done!"
}

function kafka_demo_setup() {
	# Start all the installs
	start_time=$(get_date)
	check_bonfire
	echo | tee -a "${LOG_FILE}"
	echo "#######################################" | tee -a "${LOG_FILE}"
	echo "# Kafka Demo Setup on ${CLUSTER_TYPE} " | tee -a "${LOG_FILE}"
	echo "#######################################" | tee -a "${LOG_FILE}"
	echo

	if [ ${skip_namespace_reservation} -eq 0 ]; then
		echo -n "🔄 Reserving a namespace... "
		{
			bonfire namespace reserve -d 24h
		} >>"${LOG_FILE}" 2>&1
	else
		echo -n "🔄 Skipping namespace reservation... "
	fi
	echo "✅ Done!"
	echo -n "🔄 Updating Bonfire config with the Kruize image..."
	{
		if [[ -f "$BONFIRE_CONFIG_FILE" ]]; then
			echo "Updating Bonfire config file: $BONFIRE_CONFIG_FILE..."

			# Update ROS repo location
			sed -i "s|repo: .*ros-ocp-backend|repo: $(pwd)/${repo_name}|g" "$BONFIRE_CONFIG_FILE"

			# Update KRUIZE_IMAGE
			if grep -q "KRUIZE_IMAGE:" "$BONFIRE_CONFIG_FILE"; then
				sed -i "s|KRUIZE_IMAGE:.*|KRUIZE_IMAGE: $KRUIZE_IMAGE|" "$BONFIRE_CONFIG_FILE"
			fi

			# Update KRUIZE_IMAGE_TAG
			if grep -q "KRUIZE_IMAGE_TAG:" "$BONFIRE_CONFIG_FILE"; then
				sed -i "s|KRUIZE_IMAGE_TAG:.*|KRUIZE_IMAGE_TAG: $KRUIZE_IMAGE_TAG|" "$BONFIRE_CONFIG_FILE"
			fi
		else
			echo "Error: Bonfire config.yaml file not found. Skipping update."
		fi
	} >>"${LOG_FILE}" 2>&1
	echo "✅ Done!"
	echo -n "🔄 Pulling required repositories..."
	if [ ! -d ${repo_name} ]; then
		{
			clone_repo
		} >>"${LOG_FILE}" 2>&1
	fi
	echo "✅ Done!"
	# below step is temporarily added, will be removed once the kruize-clowdapp changes are merged
	clowder_file_replace
	EPHEMERAL_NAMESPACE=$(bonfire namespace list --mine | awk 'NR==3 {print $1}')
	# Check if no namespace is found
	if [[ -z "$EPHEMERAL_NAMESPACE" ]]; then
			echo "❌ No namespace is reserved under your name. Kindly re-run the demo script without the '-r' flag."
			exit 1
	fi
	echo "EPHEMERAL_NAMESPACE = ${EPHEMERAL_NAMESPACE}"
	########################
	# Get Kafka svc
	########################
	KAFKA_SVC_NAME=$(oc get svc | grep kafka-bootstrap | awk '{print $1}')

	#######################################################################################
	# Modify kruize-clowdapp file to update the namespace and kafka bootstrap value
	#######################################################################################

	if [[ -f "$CLOWDAPP_FILE" ]]; then
		echo -n "🔄 Modifying $CLOWDAPP_FILE..."

		# Update the namespace value in the YAML file
		sed -i "s/\(http:\/\/kruize-recommendations\.\)ephemeral-[a-z0-9]\+\(.*\)/\1${EPHEMERAL_NAMESPACE}\2/" "$CLOWDAPP_FILE"

		# Update KAFKA_BOOTSTRAP_SERVERS value
		sed -i "s/\(value: \"\)env-ephemeral-[a-z0-9]\+-[a-z0-9]\+-kafka-bootstrap\(\..*\)/\1${KAFKA_SVC_NAME}\2/" "$CLOWDAPP_FILE"
		sed -i "s/\(value: \".*\)ephemeral-[a-z0-9]\+\(.*\)/\1${EPHEMERAL_NAMESPACE}\2/" "$CLOWDAPP_FILE"
		echo "✅ Done!"
	else
		echo "Error: $CLOWDAPP_FILE not found. Skipping modification."
	fi

	echo -n "🔄 Deploying the application.Please wait..."
	{
		bonfire deploy ros-ocp-backend -C kruize-test || error_exit "Failed to deploy the application."
	} >>"${LOG_FILE}" 2>&1
	echo "✅ Installation complete!"

	echo "🔄 Waiting for Kruize Pods to come up..."
	wait_for_pod

	############################
	# Expose Kruize svc
	############################
	echo -n "🔄 Exposing kruize-recommendations service..."
	if ! oc expose svc/kruize-recommendations 2>&1 | grep -q "AlreadyExists"; then
    echo "✅ Route created successfully!"
	else
			echo "⚠️ Route already exists, continuing..."
	fi
	echo "✅ Done!"


	########################
	# Get the route
	########################
	KRUIZE_ROUTE=$(oc get route | grep kruize-recommendations-ephemeral | awk '{print $2}')
	echo "KRUIZE_ROUTE = ${KRUIZE_ROUTE}"

	################################
	# Create Metric Profile
	################################
	echo -n "🔄 Creating Metric Profile..."
	api_call "${KRUIZE_ROUTE}/createMetricProfile" "@resource_optimization_local_monitoring.json" >>"${LOG_FILE}" 2>&1
	echo "✅ Created Successfully!"

	####################################################
	# Invoke the Bulk Service and get the jobID
	####################################################
	echo "🔄 Invoking Bulk Service..."
	echo "curl -s -X POST "${KRUIZE_ROUTE}/bulk" -H Content-Type: application/json -d '{\"datasource\":\"${DATASOURCE_NAME}\"}'"
	api_call "${KRUIZE_ROUTE}/bulk" "{\"datasource\":\"${DATASOURCE_NAME}\"}" >>"${LOG_FILE}" 2>&1
	echo "✅ Job_id generated!"

	##########################################################################
	# Start consuming the recommendations using recommendations-topic
	##########################################################################
	echo -n "🔄 Consuming recommendations from recommendations-topic..."
	echo
	KAFKA_POD_NAME=$(oc get pods | grep kafka | awk '{print $1}')
	echo "oc exec "$KAFKA_POD_NAME" -- bin/kafka-console-consumer.sh --topic recommendations-topic --bootstrap-server "${KAFKA_SVC_NAME}"."${EPHEMERAL_NAMESPACE}".svc.cluster.local:9092 --from-beginning"
	oc exec $KAFKA_POD_NAME -- bin/kafka-console-consumer.sh --topic recommendations-topic --bootstrap-server ${KAFKA_SVC_NAME}.${EPHEMERAL_NAMESPACE}.svc.cluster.local:9092 --from-beginning >>"${LOG_FILE}" 2>&1

#  setup_kafka_local
#
#  # get the certificate from the cluster
#  oc get secret -n ${EPHEMERAL_NAMESPACE} my-cluster-cluster-ca-cert -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt
#
#  keytool -import -trustcacerts -alias root -file ca.crt -keystore truststore.jks -storepass password -noprompt
#
#  # Grab Kafka Endpoint
#  KAFKA_ENDPOINT=$(oc -n kafka get kafka kruize-kafka-cluster -o=jsonpath='{.status.listeners[?(@.name=="route")].bootstrapServers}')
#
#  # Connect from CLI
#  ./${KAFKA_DIR}/bin/kafka-console-consumer.sh --bootstrap-server $KAFKA_ENDPOINT --topic recommendations-topic --from-beginning  --consumer-property security.protocol=SSL --consumer-property ssl.truststore.password=password --consumer-property ssl.truststore.location=truststore.jks

	end_time=$(get_date)
	elapsed_time=$(time_diff "${start_time}" "${end_time}")
	echo "🕒 Success! Kafka demo setup took ${elapsed_time} seconds"
	echo
}

function kafka_demo_setup_terminate() {
	start_time=$(get_date)
	echo | tee -a "${LOG_FILE}"
	echo "#######################################" | tee -a "${LOG_FILE}"
	echo "#  Kafka Demo Terminate on ${CLUSTER_TYPE} #" | tee -a "${LOG_FILE}"
	echo "#######################################" | tee -a "${LOG_FILE}"
	echo | tee -a "${LOG_FILE}"
	echo "Clean up in progress..."
	echo

	if [ "${CLUSTER_TYPE}" == "ephemeral" ]; then
		bonfire namespace release -f || echo "⚠️ Warning: Failed to release namespace, continuing..."
	fi
	echo
	echo -n "🔄 Removing git repos..."
	rm -rf ${repo_name}
	echo "✅"
	end_time=$(get_date)
	elapsed_time=$(time_diff "${start_time}" "${end_time}")
	echo
	echo "🕒 Success! Kafka demo cleanup took ${elapsed_time} seconds"
	echo
}

############################
#  Clone git Repos
############################
function clone_repo() {
	echo "1. Cloning ${repo_name} git repo..."
	git clone git@github.com:RedHatInsights/"${repo_name}".git >/dev/null 2>/dev/null
	check_err "ERROR: git clone of ros-ocp-backend failed."
	echo "done"
}

# Function to make API call and check response status
function api_call() {
    local url="$1"
    local data="$2"

    # Make API call and capture the HTTP response code
    response_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$url" \
        -H "Content-Type: application/json" \
        -d "$data")

    # Check if response code is 200 or 201
    if [[ "$response_code" == "200" || "$response_code" == "201" ]]; then
        echo "✅ API call succeeded with response code: $response_code"
    elif [ "$response_code" == "409" ]; then
        echo "❌ API call failed with response code: $response_code"
        echo "Continuing to the next step..."
    else
        echo "❌ API call failed with response code: $response_code"
        exit 1
    fi
}

# Wait for any pod containing "kruize-recommendations" to be Running
function wait_for_pod() {
    local pod_prefix="kruize-recommendations"
    local timeout=60
    local elapsed=0

    echo "⏳ Waiting for pod with prefix '$pod_prefix' to be in 'Running' state (Timeout: ${timeout}s)..."

    while [[ $elapsed -lt $timeout ]]; do
        pod_name=$(oc get pods --no-headers | grep "$pod_prefix" | awk '{print $1}')
        pod_status=$(oc get pod "$pod_name" -o jsonpath='{.status.phase}' 2>/dev/null || echo "")

        if [[ "$pod_status" == "Running" ]]; then
            echo "✅ Pod '$pod_name' is Running!"
            return 0
        fi

        echo "⏳ Kruize Pod is not ready yet... retrying in 5s"
        sleep 5
        ((elapsed+=5))
    done

    echo "❌ Timeout! No pod with prefix '$pod_prefix' reached 'Running' state within ${timeout}s."
    exit 1
}

# Parse command-line options
while getopts "sti:u:d:r" opt; do
	case "${opt}" in
	s)
		start_demo=1
		;;
	t)
		start_demo=0
		;;
	c)
		CLUSTER_TYPE="${OPTARG}"
		check_cluster_type
		;;
	i)
		IFS=":" read -r KRUIZE_IMAGE KRUIZE_IMAGE_TAG <<<"${OPTARG}"
		;;
	u)
		DATASOURCE_URL="${OPTARG}"
		;;
	d)
		DATASOURCE_NAME="${OPTARG}"
		;;
	r)
		skip_namespace_reservation=1
		;;
	*)
		usage
		;;
	esac
done

# Perform action based on selection
if [ ${start_demo} -eq 1 ]; then
	echo
	echo "Starting the demo using: "
	echo "Kruize Image: $KRUIZE_IMAGE"
	echo "Kruize Image Tag: $KRUIZE_IMAGE_TAG"
	echo "DATASOURCE_URL: $DATASOURCE_URL"
	echo "DATASOURCE_NAME: $DATASOURCE_NAME"
	kafka_demo_setup
else
	echo
	echo "🔄 Terminating the demo setup..."
	kafka_demo_setup_terminate
fi

# If the user passes '-h' or '--help', show usage and exit
if [[ $1 == "-h" || $1 == "--help" ]]; then
	usage
fi
