import subprocess
import pytest
import os
from kafka import KafkaConsumer

def consume_messages_from_kafka(topic):
    server = "http://localhost:9092"
    print(f"Starting consumer with server {server} to consume messages from topic {topic}...")
  
    consumer = KafkaConsumer(
        topic,
        bootstrap_servers=server,
        group_id='test-consumer-group',
        auto_offset_reset='earliest'
    )
    print("Created consumer")
    for message in consumer:
        print(f"Consumed: {message.value.decode('utf-8')}")

def run_command(command, log_file="kafka_setup.log"):
    with open(log_file, "a") as log:
        log.write(f"\nRunning command: {' '.join(command)}\n")
        try:
            subprocess.run(command, check=True, stdout=log, stderr=log)
        except subprocess.CalledProcessError as ex:
            log.write(f"Command failed with exit code {ex.returncode}: {ex}\n")
            raise

def apache_kafka_setup():
    kafka_url = "https://dlcdn.apache.org/kafka/3.9.0/kafka_2.13-3.9.0.tgz"
    kafka_archive = "kafka_2.13-3.9.0.tgz"
    kafka_dir = "kafka_2.13-3.9.0"

    try:
        print("Downloading Kafka...")
        subprocess.run(["wget", "-q", kafka_url], check=True)

        print("Extracting Kafka...")
        subprocess.run(["tar", "zxf", kafka_archive], check=True)

        # Start Zookeeper
        print("Starting Zookeeper...")
        log_file = "zookeper.log"
        with open(log_file, "w") as log:
            zookeeper_cmd = ["bin/zookeeper-server-start.sh", "config/zookeeper.properties"]
            zookeeper_process = subprocess.Popen(zookeeper_cmd, cwd=kafka_dir, stdout = log, stderr = log)

        # Configure heap options for Kafka
        print("Configuring Kafka heap options...")
        os.environ["KAFKA_HEAP_OPTS"] = "-Xmx256M -Xms128M"

        print("Editing Kafka server.properties...")
        server_properties_path = os.path.join(kafka_dir, "config/server.properties")
        with open(server_properties_path, "r") as file:
            lines = file.readlines()

        with open(server_properties_path, "w") as file:
            for line in lines:
                if line.strip().startswith("#advertised.listeners=PLAINTEXT://"):  
                    file.write("advertised.listeners=PLAINTEXT://localhost:9092\n")
                else:
                    file.write(line)

        # Start Kafka server
        print("Starting Kafka server...")
        log_file = "kafka_server.log"
        with open(log_file, "w") as log:
            kafka_server_cmd = ["bin/kafka-server-start.sh", "config/server.properties"]
            kafka_server_process = subprocess.Popen(kafka_server_cmd, cwd=kafka_dir, stdout = log, stderr = log)

        # Create Kafka topics
        topics = ["recommendations", "summary"]
        for topic in topics:
            create_topic_cmd = [
                "bin/kafka-topics.sh", "--create", "--topic", topic,
                "--bootstrap-server", "localhost:9092", "--replication-factor", "1", "--partitions", "1"
            ]
            subprocess.run(create_topic_cmd, cwd=kafka_dir, check=True)

        print("Kafka setup complete")

    except subprocess.CalledProcessError as e:
        print(f"Error during setup: {e}")

def apache_kafka_cleanup():
    kafka_dir = "kafka_2.13-3.9.0"
    kafka_archive = "kafka_2.13-3.9.0.tgz"

    try:
        # Delete Kafka topics
        topics = ["recommendations", "summary"]
        for topic in topics:
            delete_topic_cmd = [
                "bin/kafka-topics.sh", "--delete", "--topic", topic,
                "--bootstrap-server", "localhost:9092"
            ]
            subprocess.run(delete_topic_cmd, cwd=kafka_dir, check=True)

        print("Stopping Kafka server...")
        kafka_stop_cmd = ["bin/kafka-server-stop.sh"]
        subprocess.run(kafka_stop_cmd, cwd=kafka_dir, check=True)

        print("Stopping Zookeeper...")
        zookeeper_stop_cmd = ["bin/zookeeper-server-stop.sh"]
        subprocess.run(zookeeper_stop_cmd, cwd=kafka_dir, check=True)

        print("Cleaning up extracted files...")
        subprocess.run(["rm", "-rf", kafka_dir], check=True)

        print("Cleaning up downloaded tar file...")
        subprocess.run(["rm", "-f", kafka_archive], check=True)

        print("Kafka cleanup complete")

    except subprocess.CalledProcessError as e:
        print(f"Error during cleanup: {e}")

