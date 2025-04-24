import base64
import subprocess

from kafka import KafkaConsumer

KAFKA_NAMESPACE = "kafka"
LOG_FILE = "kafka_setup.log"
CA_CERT_FILE = "ca.crt"
TRUSTSTORE_FILE = "truststore.jks"
TRUSTSTORE_PASSWORD = "password"

def run_command(command_list, capture_output=True, log_file=None):
    """Utility to run a subprocess command."""
    result = subprocess.run(
        command_list,
        stdout=subprocess.PIPE if capture_output else None,
        stderr=subprocess.PIPE,
        text=True
    )
    if result.returncode != 0:
        error_msg = f"Command failed: {' '.join(command_list)}\n{result.stderr}"
        if log_file:
            with open(log_file, "a") as f:
                f.write(error_msg)
        raise RuntimeError(error_msg)
    return result.stdout.strip() if capture_output else None


def fetch_and_save_ca_cert():
    print("Fetching CA certificate from OpenShift...")
    ca_cert_b64 = run_command([
        "oc", "get", "secret", "-n", KAFKA_NAMESPACE,
        "kruize-kafka-cluster-cluster-ca-cert",
        "-o", "jsonpath={.data.ca\\.crt}"
    ], log_file=LOG_FILE)

    with open(CA_CERT_FILE, "wb") as f:
        f.write(base64.b64decode(ca_cert_b64))


def get_kafka_endpoint():
    print("Fetching Kafka endpoint from cluster...")
    return run_command([
        "oc", "-n", KAFKA_NAMESPACE,
        "get", "kafka", "kruize-kafka-cluster",
        "-o", "jsonpath={.status.listeners[?(@.name==\"external\")].bootstrapServers}"
    ], log_file=LOG_FILE)


def consume_kafka_message(endpoint, topic):
    print(f"Connecting to Kafka at: {endpoint}")

    consumer = KafkaConsumer('my-topic', group_id='my-group', bootstrap_servers=endpoint, ssl_cafile='ca.crt',
                             security_protocol="SSL", consumer_timeout_ms=10000, enable_auto_commit=True)
    print("Waiting for a message...")
    try:
        for message in consumer:
            print(f"{message.topic}:{message.partition}:{message.offset}: value={message.value.decode('utf-8')}")
            break  # Consume only one message and break the loop
    except Exception as e:
        print(f"Error consuming message: {e}")
    finally:
        consumer.close()



    # conf = {
    #     'bootstrap.servers': endpoint,
    #     'security.protocol': 'SSL',
    #     'ssl.ca.location': 'ca.crt',
    #     'group.id': 'python-consumer',
    #     'auto.offset.reset': 'earliest',
    # }
    #
    # consumer = Consumer(conf)
    # consumer.subscribe([topic])
    #
    # try:
    #     msg = consumer.poll(timeout=10.0)
    #     if msg is None:
    #         print("No message received.")
    #     elif msg.error():
    #         print("Consumer error:", msg.error())
    #     else:
    #         print(f"Received message: {msg.value().decode('utf-8')}")
    # finally:
    #     consumer.close()


def consume_messages_from_kafka(topic):

    print("Consuming messages from Kafka topic ...")

    try:
        fetch_and_save_ca_cert()
        endpoint = get_kafka_endpoint()
        consume_kafka_message(endpoint, topic)
    except Exception as e:
        print(f"ERROR: {e}")
