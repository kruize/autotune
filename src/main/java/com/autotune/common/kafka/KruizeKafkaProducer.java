/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.common.kafka;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * KruizeKafkaProducer class is responsible for producing messages to Kafka topics.
 * It provides methods to send different types of messages, such as recommendations, errors, and summaries.
 */
public class KruizeKafkaProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeKafkaProducer.class);

    // Singleton Kafka Producer Instance
    private static final KafkaProducer<String, String> producer = new KafkaProducer<>(getProducerProperties());

    /**
     * Retrieves Kafka producer properties.     *
     * @return Properties object containing Kafka producer configuration.
     */
    private static Properties getProducerProperties() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KruizeDeploymentInfo.kafka_bootstrap_servers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, KruizeConstants.KAFKA_CONSTANTS.ALL);
        return producerProps;
    }

    /**
     * Sends a message to the specified Kafka topic.
     *
     * @param topic   The Kafka topic to send the message to.
     * @param payload The message payload.
     */
    private static void sendMessage(String topic, String payload) {
        try {
            LOGGER.debug("Sending message to client...");
            RecordMetadata metadata = producer.send(new ProducerRecord<>(topic, payload))
                    .get(5, TimeUnit.SECONDS); //todo : set the timeout value via ENV
            // todo: get the status of message whether its delivered or failed
            LOGGER.debug(KruizeConstants.KAFKA_CONSTANTS.MESSAGE_SENT_SUCCESSFULLY, metadata.topic(), metadata.partition(), metadata.offset());
        } catch (TimeoutException te) {
            LOGGER.error(KruizeConstants.KAFKA_CONSTANTS.KAFKA_MESSAGE_TIMEOUT_ERROR, topic, te.getMessage());
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.KAFKA_CONSTANTS.KAFKA_MESSAGE_FAILED, topic, e.getMessage(), e);
        }
    }

    /**
     * Produces valid recommendation messages.
     */
    public static class ValidRecommendationMessageProducer implements Runnable {
        private final String payload;

        /**
         * Constructs a new producer for valid recommendation messages.
         *
         * @param payload The recommendation message payload.
         */
        public ValidRecommendationMessageProducer(String payload) {
            this.payload = payload;
        }

        /**
         * Runs the producer to send the recommendation message.
         */
        @Override
        public void run() {
            sendMessage(KruizeConstants.KAFKA_CONSTANTS.RECOMMENDATIONS_TOPIC, payload);
        }
    }

    /**
     * Produces error messages.
     */
    public static class ErrorMessageProducer implements Runnable {
        private final String errorDetails;

        /**
         * Constructs a new producer for error messages.
         *
         * @param errorDetails The error message payload.
         */
        public ErrorMessageProducer(String errorDetails) {
            this.errorDetails = errorDetails;
        }

        /**
         * Runs the producer to send the error message.
         */
        @Override
        public void run() {
            sendMessage(KruizeConstants.KAFKA_CONSTANTS.ERROR_TOPIC, errorDetails);
        }
    }

    /**
     * Produces summary response messages.
     */
    public static class SummaryResponseMessageProducer implements Runnable {
        private final String payload;

        /**
         * Constructs a new producer for summary response messages.
         *
         * @param payload The summary message payload.
         */
        public SummaryResponseMessageProducer(String payload) {
            this.payload = payload;
        }

        /**
         * Runs the producer to send the summary message.
         */
        @Override
        public void run() {
            sendMessage(KruizeConstants.KAFKA_CONSTANTS.SUMMARY_TOPIC, payload);
        }
    }

    /**
     * Closes the Kafka producer instance.
     */
    public static void close() {
        if (producer != null) {
            producer.close();
            LOGGER.debug(KruizeConstants.KAFKA_CONSTANTS.KAFKA_PRODUCER_CLOSED);
        }
    }
}
