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
current_dir=$1
export CLUSTER_TYPE="openshift"
export KRUIZE_DOCKER_IMAGE="quay.io/kruize/autotune_operator:0.5"
# Define Kafka version and file URL
KAFKA_VERSION="3.9.0"
SCALA_VERSION="2.13"
KAFKA_TGZ="kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz"
KAFKA_URL="https://dlcdn.apache.org/kafka/${KAFKA_VERSION}/${KAFKA_TGZ}"
KAFKA_CLUSTER_NAME="kruize-kafka-cluster"

KAFKA_NAMESPACE="kafka"
kafka_server_setup=1

set -euo pipefail  # Enable strict error handling

LOG_FILE="${current_dir}/kafka-setup.log"


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

function setup_kafka_server() {
  {
  # Create namespace for Kafka if it doesn't exist
  echo "Creating namespace for Kafka..."
  oc create namespace $KAFKA_NAMESPACE || echo "Namespace $KAFKA_NAMESPACE already exists."

  # Install Kafka using Strimzi Operator (if not already installed)
  echo "Installing Strimzi Operator..."
  oc apply -f https://strimzi.io/install/latest?namespace=$KAFKA_NAMESPACE -n $KAFKA_NAMESPACE

  # Wait for the Strimzi Operator to be ready
  echo "Waiting for Strimzi Operator to be ready..."
  oc rollout status deployment/strimzi-cluster-operator -n $KAFKA_NAMESPACE

  # Create Kafka cluster YAML
  cat <<EOF | oc apply -n $KAFKA_NAMESPACE -f -
  apiVersion: kafka.strimzi.io/v1beta2
  kind: Kafka
  metadata:
    name: $KAFKA_CLUSTER_NAME
  spec:
    kafka:
      version: 3.8.0
      replicas: 3
      listeners:
        - name: plain
          port: 9092
          type: internal
          tls: false
        - name: tls
          port: 9093
          type: internal
          tls: true
        - name: external
          port: 9094
          type: route
          tls: true
      config:
        offsets.topic.replication.factor: 3
        transaction.state.log.replication.factor: 3
        log.message.format.version: "3.4"
      storage:
        type: ephemeral
    zookeeper:
      replicas: 3
      storage:
        type: ephemeral
    entityOperator:
      topicOperator: {}
      userOperator: {}
EOF

  # Wait for Kafka to be ready
  echo "Waiting for Kafka cluster to be ready..."
  oc wait kafka/$KAFKA_CLUSTER_NAME --for=condition=Ready --timeout=300s -n $KAFKA_NAMESPACE

# Create Kafka topics
echo "Creating Kafka topics..."
cat <<EOF | oc apply -n $KAFKA_NAMESPACE -f -
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: recommendations-topic
  labels:
    strimzi.io/cluster: $KAFKA_CLUSTER_NAME
spec:
  partitions: 3
  replicas: 3
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: error-topic
  labels:
    strimzi.io/cluster: $KAFKA_CLUSTER_NAME
spec:
  partitions: 3
  replicas: 3
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: summary-topic
  labels:
    strimzi.io/cluster: $KAFKA_CLUSTER_NAME
spec:
  partitions: 3
  replicas: 3
EOF

  # Get Kafka bootstrap server URL
  BOOTSTRAP_SERVER="$KAFKA_CLUSTER_NAME-kafka-bootstrap.$KAFKA_NAMESPACE.svc.cluster.local:9092"

  echo "✅ Kafka Bootstrap Server: $BOOTSTRAP_SERVER"
  export BOOTSTRAP_SERVER
  } >> "${LOG_FILE}" 2>&1
}

# Check if the cluster_type is openshift
function check_cluster_type() {
	case "${CLUSTER_TYPE}" in openshift) ;;
	*)
		echo "Error: unsupported cluster type: ${CLUSTER_TYPE}"
		echo "Currently only openshift cluster is supported"
		exit -1
		;;
	esac
}


# Start all the installs
start_time=$(get_date)
# Clear the log file at the start of the script
> "${LOG_FILE}"

check_cluster_type

{
  echo
  echo "#######################################"
  echo "# Kafka Setup on ${CLUSTER_TYPE} "
  echo "#######################################"
  echo
} | tee -a "${LOG_FILE}"

rm -rf ca.crt truststore.jks >> "${LOG_FILE}" 2>&1

echo -n "🔄 Setting up Kafka server on $CLUSTER_TYPE. Please wait..."
kafka_start_time=$(get_date)
setup_kafka_server &
install_pid=$!
while kill -0 $install_pid 2>/dev/null;
do
  echo -n "."
  sleep 5
done
wait $install_pid
status=$?
if [ ${status} -ne 0 ]; then
  exit 1
fi
kafka_end_time=$(get_date)

echo "✅ Kafka server setup completed"
echo
export  BOOTSTRAP_SERVER="$KAFKA_CLUSTER_NAME-kafka-bootstrap.$KAFKA_NAMESPACE.svc.cluster.local:9092"

echo -n "⏳ Setting up Kafka client locally to consume recommendations..."
setup_kafka_local >> "${LOG_FILE}" 2>&1
echo "✅ Kafka client setup completed"
echo "✅ Done"
echo

end_time=$(get_date)
if [ ${kafka_server_setup} -eq 1 ]; then
  kafka_elapsed_time=$(time_diff "${kafka_start_time}" "${kafka_end_time}")
  echo "🕒 Success! Kafka Server setup took ${kafka_elapsed_time} seconds"
fi
elapsed_time=$(time_diff "${start_time}" "${end_time}")
echo "🕒 Success! Kafka setup took ${elapsed_time} seconds"
echo
