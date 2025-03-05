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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Scanner;

//TODO: This class is not being used for now, will be updated later
/**
 * KruizeKafkaConsumer class is responsible for consuming messages from a Kafka topic.
 * It implements the Runnable interface to allow execution in a separate thread.
 * The consumer listens for messages and logs the received recommendations.
 */
public class KruizeKafkaConsumer implements Runnable {
    private static KafkaConsumer<String, String> consumer;
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeKafkaConsumer.class);

    /**
     * The main execution method of the Kafka consumer.
     * The consumer subscribes to a Kafka topic and continuously polls for new input.
     */
    @Override
    public void run() {

        // Flag to control the loop and terminate when needed
        boolean continueListening = true;

        try {
            consumer = getKafkaConsumerConfig();
            consumer.subscribe(java.util.Collections.singletonList(KruizeDeploymentInfo.bulk_input_topic));
            while (continueListening) {
                consumer.poll(java.time.Duration.ofMillis(100)).forEach(record -> {
                    LOGGER.debug("Received Input: JobID={}, Value={}, Partition={}, Offset={}",
                            record.key(), record.value(), record.partition(), record.offset());
                });
                if (isTerminationSignalReceived()) {
                    continueListening = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures and returns a Kafka consumer instance.
     *
     * @return a configured {@code KafkaConsumer} instance.
     */
    private KafkaConsumer<String, String> getKafkaConsumerConfig() {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KruizeDeploymentInfo.kafka_bootstrap_servers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, KruizeDeploymentInfo.kafka_group_id);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        return new KafkaConsumer<>(consumerProps);
    }

    /**
     * Adds a shutdown hook to close the Kafka consumer gracefully.
     */
    private static void addConsumerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (consumer != null) {
                consumer.close();
            }
        }));
    }

    /**
     * Checks if a termination signal has been received.
     *
     * @return {@code true} if termination input is detected; {@code false} otherwise.
     */
    private static boolean isTerminationSignalReceived() {
        Scanner scanner = new Scanner(System.in);
        return scanner.hasNext(); // This will return true if any input arrives
    }
}
