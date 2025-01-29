package com.autotune.utils.kafka;

import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class KruizeKafkaProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeKafkaProducer.class);

    // Singleton Kafka Producer Instance
    private static final KafkaProducer<String, String> producer = new KafkaProducer<>(getProducerProperties());

    // Get Kafka producer properties
    private static Properties getProducerProperties() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KruizeDeploymentInfo.kafka_bootstrap_servers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        return producerProps;
    }

    // Kafka message producer
    private static void sendMessage(String topic, String payload) {
        try {
            RecordMetadata metadata = producer.send(new ProducerRecord<>(topic, payload))
                    .get(5, TimeUnit.SECONDS); //todo : set the timeout value via ENV
            // todo: get the status of message whether its delivered or failed
            LOGGER.debug("Message sent successfully to topic {} at partition {} and offset {}",
                    metadata.topic(), metadata.partition(), metadata.offset());
        } catch (TimeoutException te) {
            LOGGER.error("Kafka timeout while sending message to topic {}: {}", topic, te.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error sending message to Kafka topic {}: {}", topic, e.getMessage(), e);
        }
    }

    // Send valid recommendation messages
    public static class ValidRecommendationMessageProducer implements Runnable {
        private final String payload;

        public ValidRecommendationMessageProducer(String payload) {
            this.payload = payload;
        }

        @Override
        public void run() {
            sendMessage(KruizeConstants.KAFKA_CONSTANTS.RECOMMENDATIONS_TOPIC, payload);
        }
    }

    // Send error messages
    public static class ErrorMessageProducer implements Runnable {
        private final String errorDetails;

        public ErrorMessageProducer(String errorDetails) {
            this.errorDetails = errorDetails;
        }

        @Override
        public void run() {
            sendMessage(KruizeConstants.KAFKA_CONSTANTS.ERROR_TOPIC, errorDetails);
        }
    }

    // Send summary messages
    public static class SummaryResponseMessageProducer implements Runnable {
        private final String payload;

        public SummaryResponseMessageProducer(String payload) {
            this.payload = payload;
        }

        @Override
        public void run() {
            sendMessage(KruizeConstants.KAFKA_CONSTANTS.SUMMARY_TOPIC, payload);
        }
    }

    // Close the Kafka producer
    public static void close() {
        if (producer != null) {
            producer.close();
            LOGGER.info("Kafka producer closed.");
        }
    }
}
