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
package com.autotune.analyzer.workerimpl;

import com.autotune.analyzer.serviceObjects.*;
import com.autotune.analyzer.services.BulkService;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.common.kafka.KruizeKafka;
import com.autotune.common.kafka.KruizeKafkaProducer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages Kafka message publishing using an executor service.
 * Ensures messages are published to valid Kafka topics and processed asynchronously.
 */
public class KruizeKafkaManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeKafkaManager.class);
    private static KruizeKafkaManager instance;
    private final ExecutorService kafkaExecutorService;
    private final Set<String> validTopics;

    /**
     * Constructs a KruizeKafkaManager instance.
     * Initializes an executor service and loads valid Kafka topics from the configuration.
     */
    public KruizeKafkaManager() {
        this.kafkaExecutorService = Executors.newFixedThreadPool(3);
        // validate the Kafka Connection
        validateKafkaConnection();
        // Load valid topics from config
        validTopics = KruizeDeploymentInfo.loadKafkaTopicsFromConfig();
    }

    /**
     * Returns a singleton instance of the KruizeKafkaManager.
     *
     * @return the singleton instance
     */
    public static synchronized KruizeKafkaManager getInstance() {
        if (instance == null) {
            instance = new KruizeKafkaManager();
        }
        return instance;
    }

    /**
     * Publishes a Kafka message after filtering and validation.
     *
     * @param topic              the Kafka topic
     * @param jobData            the bulkJobStatus object containing the response data
     * @param experimentName     the experiment name
     * @param experiment         the experiment object
     * @param kafkaIncludeFilter filters to include in the Kafka message
     * @param kafkaExcludeFilter filters to exclude from the Kafka message
     */
    void publishKafkaMessage(String topic, BulkJobStatus jobData, String experimentName, BulkJobStatus.Experiment experiment, Set<String> kafkaIncludeFilter, Set<String> kafkaExcludeFilter) {
        try {
            if (!validTopics.contains(topic)) {
                throw new Exception(String.format(KruizeConstants.KAFKA_CONSTANTS.MISSING_KAFKA_TOPIC, topic));
            }
            String kafkaMessage = BulkService.filterJson(jobData, kafkaIncludeFilter, kafkaExcludeFilter, experimentName);
            publish(new KruizeKafka(topic, kafkaMessage));
            experiment.setStatus(KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status.PUBLISHED);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            experiment.setStatus(KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status.PUBLISH_FAILED);
        }
    }

    /**
     * Publishes a Kafka message asynchronously using the executor service.
     *
     * @param kruizeKafka the Kafka message containing the topic and message content
     */
    public void publish(KruizeKafka kruizeKafka) {
        kafkaExecutorService.submit(() -> {
            try {
                // Call Kafka producer based on topic
                switch (kruizeKafka.getTopic()) {
                    case KruizeConstants.KAFKA_CONSTANTS.RECOMMENDATIONS_TOPIC:
                        new KruizeKafkaProducer.ValidRecommendationMessageProducer(kruizeKafka.getMessage());
                        break;
                    case KruizeConstants.KAFKA_CONSTANTS.ERROR_TOPIC:
                        new KruizeKafkaProducer.ErrorMessageProducer(kruizeKafka.getMessage());
                        break;
                    case KruizeConstants.KAFKA_CONSTANTS.SUMMARY_TOPIC:
                        new KruizeKafkaProducer.SummaryResponseMessageProducer(kruizeKafka.getMessage());
                        break;
                    default:
                        throw new IllegalArgumentException(String.format(KruizeConstants.KAFKA_CONSTANTS.UNKNOWN_TOPIC, kruizeKafka.getTopic()));
                }
            } catch (Exception e) {
                LOGGER.error(KruizeConstants.KAFKA_CONSTANTS.KAFKA_PUBLISH_FAILED, e.getMessage());
            }
        });
    }

    /**
     * Validates the Kafka connection by attempting to connect to the configured Kafka bootstrap servers.
     * Creates an AdminClient with the bootstrap server properties.
     * Attempts to describe the Kafka cluster to check if nodes are reachable.
     * Logs a debug message if the connection is successful.
     * Throws a RuntimeException if the connection fails.
     *
     * @throws RuntimeException if the Kafka connection cannot be established.
     */
    private void validateKafkaConnection() {

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KruizeDeploymentInfo.kafka_bootstrap_servers);

        try (AdminClient adminClient = AdminClient.create(props)) {
            // Check if cluster nodes are reachable
            adminClient.describeCluster().nodes().get();
            LOGGER.debug(KruizeConstants.KAFKA_CONSTANTS.KAFKA_CONNECTION_SUCCESS, KruizeDeploymentInfo.kafka_bootstrap_servers);
        } catch (Exception e) {
            throw new RuntimeException(String.format(KruizeConstants.KAFKA_CONSTANTS.KAFKA_CONNECTION_FAILURE, KruizeDeploymentInfo.kafka_bootstrap_servers).concat(e.getMessage()));
        }
    }
}
