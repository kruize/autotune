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
set -e

NAMESPACE="monitoring"
CLUSTER_NAME="kruize-kafka-cluster"
TOPICS=("kruize-recommendations-topic" "kruize-error-topic" "kruize-summary-topic")

echo "Creating namespace: $NAMESPACE..."
kubectl create namespace $NAMESPACE || echo "Namespace $NAMESPACE already exists."

echo "Applying Strimzi Operator..."
kubectl apply -f https://strimzi.io/install/latest?namespace=$NAMESPACE -n $NAMESPACE

echo "Waiting for Strimzi operator to be ready..."
kubectl rollout status deployment/strimzi-cluster-operator -n $NAMESPACE --timeout=120s

echo "Deploying Kafka cluster..."
cat <<EOF | kubectl apply -n $NAMESPACE -f -
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: $CLUSTER_NAME
spec:
  kafka:
    version: 3.9.0
    replicas: 1
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
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

echo "Waiting for Kafka cluster to be ready..."
kubectl wait kafka/$CLUSTER_NAME --for=condition=Ready --timeout=300s -n $NAMESPACE

for TOPIC_NAME in "${TOPICS[@]}"; do
    echo "Creating Kafka topic: $TOPIC_NAME..."
    cat <<EOF | kubectl apply -n $NAMESPACE -f -
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: $TOPIC_NAME
  labels:
    strimzi.io/cluster: $CLUSTER_NAME
spec:
  partitions: 1
  replicas: 1
  config:
    retention.ms: 7200000
EOF
done

echo "Kafka setup complete!"
