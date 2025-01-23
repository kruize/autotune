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

# Kafka Configuration
BROKER="kruize-kafka-cluster-kafka-bootstrap.monitoring:9092"
TOPICS=("kruize-recommendations-topic" "kruize-error-topic" "kruize-summary-topic")
CONSUMER_GROUP="test-consumer-group"

# Check if Kafka CLI is available
if ! command -v kafka-console-consumer.sh &> /dev/null; then
  echo "Error: kafka-console-consumer.sh not found. Ensure Kafka CLI is installed and available in PATH."
  exit 1
fi

# Consume messages from each topic
for TOPIC in "${TOPICS[@]}"; do
  echo "Consuming messages from topic: $TOPIC"
  kafka-console-consumer.sh --bootstrap-server $BROKER \
                            --topic $TOPIC \
                            --from-beginning \
                            --consumer-property group.id=$CONSUMER_GROUP &
done

echo "Test consumers are now running for all topics. Press Ctrl+C to exit."

# Wait for all background processes to complete
wait
