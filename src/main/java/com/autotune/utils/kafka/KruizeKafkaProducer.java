package com.autotune.utils.kafka;

import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KruizeKafkaProducer {
    private String bootstrapServers;
    private String topic;
    private String jobId;
    private String message;
    private KafkaProducer<String, String> producer;
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeKafkaProducer.class);



    // Kafka producer properties
    public static Properties getProducerProperties() {

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KruizeDeploymentInfo.kafka_bootstrap_servers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        return producerProps;
    }

    // Thread to send valid recommendation messages
    public static class ValidRecommendationMessageProducer implements Runnable {
        private final KafkaProducer<String, String> producer;
        private final String messageId;
        private final String payload;

        public ValidRecommendationMessageProducer(KafkaProducer<String, String> producer, String messageId, String payload) {
            this.producer = producer;
            this.messageId = messageId;
            this.payload = payload;
        }

        @Override
        public void run() {
            String successMessage = String.format("{\"messageId\": \"%s\", \"payload\": %s}", messageId, payload);
            sendMessage(KruizeConstants.KAFKA_CONSTANTS.RECOMMENDATIONS_TOPIC, messageId, successMessage);
        }

        private void sendMessage(String topic, String key, String message) {
            try {
                RecordMetadata metadata = producer.send(new ProducerRecord<>(topic, key, message)).get();
                LOGGER.info("Success message sent to topic {} | partition {} | offset {}\n",
                        metadata.topic(), metadata.partition(), metadata.offset());
            } catch (Exception e) {
                LOGGER.error("Error sending success message: {}\n", e.getMessage());
            }
        }
    }

    // Thread to send error messages
    public static class ErrorMessageProducer implements Runnable {
        private final KafkaProducer<String, String> producer;
        private final String messageId;
        private final String errorDetails;

        public ErrorMessageProducer(KafkaProducer<String, String> producer, String messageId, String errorDetails) {
            this.producer = producer;
            this.messageId = messageId;
            this.errorDetails = errorDetails;
        }

        @Override
        public void run() {
            String errorMessage = String.format(
                    "{\"messageId\": \"%s\", \"timestamp\": \"%s\", \"status\": \"FAILURE\", \"error\": {\"details\": \"%s\"}}",
                    messageId, System.currentTimeMillis(), errorDetails);
            sendMessage(KruizeConstants.KAFKA_CONSTANTS.ERROR_TOPIC, messageId, errorMessage);
        }

        private void sendMessage(String topic, String key, String message) {
            try {
                RecordMetadata metadata = producer.send(new ProducerRecord<>(topic, key, message)).get();
                LOGGER.info("Error message sent to topic {} | partition {} | offset {}\n",
                        metadata.topic(), metadata.partition(), metadata.offset());
            } catch (Exception e) {
                LOGGER.error("Error sending error message: {}\n", e.getMessage());
            }
        }
    }

    // Thread to send bulk response messages
    public static class SummaryResponseMessageProducer implements Runnable {
        private final KafkaProducer<String, String> producer;
        private final String messageId;
        private final String status;
        private final String payload;

        public SummaryResponseMessageProducer(KafkaProducer<String, String> producer, String messageId, String status, String payload) {
            this.producer = producer;
            this.messageId = messageId;
            this.status = status;
            this.payload = payload;
        }

        @Override
        public void run() {
            String responseMessage = String.format(
                    "{\"messageId\": \"%s\", \"timestamp\": \"%s\", \"status\": \"%s\", \"payload\": %s}",
                    messageId, System.currentTimeMillis(), status, payload);
            sendMessage(KruizeConstants.KAFKA_CONSTANTS.SUMMARY_TOPIC, messageId, responseMessage);
        }

        private void sendMessage(String topic, String key, String message) {
            try {
                RecordMetadata metadata = producer.send(new ProducerRecord<>(topic, key, message)).get();
                LOGGER.info("Response message sent to topic {} | partition {} | offset {}\n",
                        metadata.topic(), metadata.partition(), metadata.offset());
            } catch (Exception e) {
                LOGGER.error("Error sending response message: {}\n", e.getMessage());
            }
        }
    }


    // Close the Kafka producer
    public void close() {
        if (producer != null) {
            producer.close();
        }
    }
}
