#!/bin/bash

KAFKA_ROOT_DIR=$1
if [ -z "$KAFKA_ROOT_DIR" ]; then
    KAFKA_ROOT_DIR="${PWD}"
fi

if java -version &> /dev/null; then
    echo "Java is already installed."
    java -version
else
    echo "Install java"
    exit 1
fi

# Define Kafka version and file URL
KAFKA_VERSION="3.9.0"
SCALA_VERSION="2.13"
KAFKA_TGZ="kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz"
KAFKA_DIR="${KAFKA_ROOT_DIR}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}"
KAFKA_URL="https://dlcdn.apache.org/kafka/${KAFKA_VERSION}/${KAFKA_TGZ}"
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

cd ${KAFKA_DIR}

# Start Zookeeper server in the background
echo "Starting Zookeeper server..."
nohup bin/zookeeper-server-start.sh config/zookeeper.properties > "${ZOOKEEPER_LOG}" 2>&1 &

sleep 20
if grep -i "binding to port" "${ZOOKEEPER_LOG}" > /dev/null 2>&1 ; then
	echo "Zookeeper server started"
else
	echo "Zookeeper server failed to start!"
	exit 1
fi

# Set Kafka heap options
export KAFKA_HEAP_OPTS="-Xmx1G -Xms256M"

# Edit server.properties to configure Host in advertised listeners
echo "Configuring Kafka server properties..."
sed -i 's/^#advertised.listeners=PLAINTEXT:\/\/\${HOSTNAME}:9093/advertised.listeners=PLAINTEXT:\/\/localhost:9092/' config/server.properties

# Start Kafka server
echo "Starting Kafka server..."
nohup bin/kafka-server-start.sh config/server.properties > "${KAFKA_SERVER_LOG}" 2>&1 &

sleep 20
if grep -i "KafkaServer id=0\] started" "${KAFKA_SERVER_LOG}" > /dev/null 2>&1 ; then
	echo "Kafka server started"
else
	echo "Kafka server failed to start!"
	exit 1
fi

# Create Kafka topics
topics=("recommendations" "summary")

echo "Creating Kafka topics..."
for topic in "${topics[@]}"; do
    echo "Creating topic: $topic"
    bin/kafka-topics.sh --create --topic "$topic" --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1
done

echo "Checking Kafka topics are listed..."
for topic in "${topics[@]}"; do
    echo "Checking topic: $topic"
    if bin/kafka-topics.sh --list --bootstrap-server "localhost:9092" | grep -w "$topic" > /dev/null 2>&1; then
	echo "$topic is listed"
    else
	echo "$topic is not listed"
	exit 1
    fi
done

cd ..
exit 0
echo "Kafka setup complete!"

