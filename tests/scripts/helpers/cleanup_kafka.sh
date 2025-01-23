#!/bin/bash

KAFKA_ROOT_DIR=$1
if [ -z "$KAFKA_ROOT_DIR" ]; then
        KAFKA_ROOT_DIR="${PWD}"
fi

# Define Kafka version and directory
KAFKA_VERSION="3.9.0"
SCALA_VERSION="2.13"
KAFKA_DIR="${KAFKA_ROOT_DIR}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}"
ZOOKEEPER_LOG="${KAFKA_ROOT_DIR}/zookeeper.log"
KAFKA_SERVER_LOG="${KAFKA_ROOT_DIR}/kafka_server.log"

# List of topics to delete
topics=("recommendations" "summary")
echo "Deleting Kafka topics..."
for topic in "${topics[@]}"; do
  echo "Deleting topic: $topic"
  $KAFKA_DIR/bin/kafka-topics.sh --delete --topic "$topic" --bootstrap-server localhost:9092
done

# Stop Kafka and Zookeeper servers
echo "Stopping Kafka and Zookeeper servers..."

$KAFKA_DIR/bin/zookeeper-server-stop.sh
$KAFKA_DIR/bin/kafka-server-stop.sh
sleep 5

#KAFKA_PID=$(ps aux | grep '[k]afka-server-start.sh' | awk '{print $2}')
KAFKA_PID=$(ps aux | grep 'kafka.Kafka' | grep -v 'grep' | awk '{print $2}')
if [ ! -z "$KAFKA_PID" ]; then
  echo "Kafka server not stopped"
else
  echo "Kafka server stopped"
fi

# Stop Zookeeper server
#ZOOKEEPER_PID=$(ps aux | grep '[z]ookeeper-server-start.sh' | awk '{print $2}')
ZOOKEEPER_PID=$(ps aux | grep 'org.apache.zookeeper.server.quorum.QuorumPeerMain' | grep -v 'grep' | awk '{print $2}')
if [ ! -z "$ZOOKEEPER_PID" ]; then
  echo "zookeeper server not stopped"
else
  echo "zookeeper server stopped"
fi

echo "Cleaning up Kafka dir..."
rm -rf "${KAFKA_DIR}"
rm "${ZOOKEEPER_LOG}"
rm "${KAFKA_SERVER_LOG}"
