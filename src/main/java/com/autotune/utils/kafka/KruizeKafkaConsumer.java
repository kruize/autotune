package com.autotune.utils.kafka;

import com.autotune.operator.KruizeDeploymentInfo;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Scanner;

//TODO: This class is not being used for now, will be updated later
public class KruizeKafkaConsumer implements Runnable {
    private static KafkaConsumer<String, String> consumer;
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeKafkaConsumer.class);

    @Override
    public void run() {

        // Flag to control the loop and terminate when needed
        boolean continueListening = true;

        try {
            consumer = getKafkaConsumerConfig();
            consumer.subscribe(java.util.Collections.singletonList(KruizeDeploymentInfo.kafka_topic_inbound));
            while (continueListening) {
                consumer.poll(java.time.Duration.ofMillis(100)).forEach(record -> {
                    LOGGER.info("Received Recommendation: JobID={}, Value={}, Partition={}, Offset={}",
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

    private KafkaConsumer<String, String> getKafkaConsumerConfig() {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KruizeDeploymentInfo.kafka_bootstrap_servers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, KruizeDeploymentInfo.kafka_group_id);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        return new KafkaConsumer<>(consumerProps);
    }

    // Shutdown hook for the consumer
    private static void addConsumerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (consumer != null) {
                consumer.close();
            }
        }));
    }

    private static boolean isTerminationSignalReceived() {
        Scanner scanner = new Scanner(System.in);
        return scanner.hasNext(); // This will return true if any input arrives
    }
}
