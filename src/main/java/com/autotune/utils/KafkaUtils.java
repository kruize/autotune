package com.autotune.utils;

import com.autotune.operator.KruizeDeploymentInfo;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Scanner;


public class KafkaUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaUtils.class);

    private static Consumer<String, String> consumer;
    private static Producer<String, String> producer;


    // Static initializer block to register shutdown hooks
    static {
        addConsumerShutdownHook();
        addProducerShutdownHook();
    }

    // Kafka Consumer Method
    public static String consumeMessages() {
        Properties props = new Properties();
        LOGGER.error("This is from env {}", KruizeDeploymentInfo.getKafkaBootstrapServers());
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KruizeDeploymentInfo.getKafkaBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KruizeDeploymentInfo.getKafkaGroupID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Flag to control the loop and terminate when needed
        boolean continueListening = true;

        try {
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(KruizeDeploymentInfo.getInboundKafkaTopic()));

            while (continueListening) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(Long.MAX_VALUE));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("Received message: " + record.value());
                }
                if (isTerminationSignalReceived()) {
                    continueListening = false;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to consume msg: ", e);
        }
        return null;
    }

    // Kafka Producer Method
    public static void produceMessage(String message) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KruizeDeploymentInfo.getKafkaBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try {
            producer = new KafkaProducer<>(props);
            ProducerRecord<String, String> record = new ProducerRecord<>(KruizeDeploymentInfo.getOutboundKafkaTopic(), message);
            producer.send(record);
        } catch (Exception e) {
            LOGGER.error("Unable to produce msg: ", e);
        }
    }

    private static boolean isTerminationSignalReceived() {
        Scanner scanner = new Scanner(System.in);
        return scanner.hasNext(); // This will return true if any input arrives
    }

    // Shutdown hook for the consumer
    private static void addConsumerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (consumer != null) {
                consumer.close();
            }
        }));
    }

    // Shutdown hook for the producer
    private static void addProducerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (producer != null) {
                producer.close();
            }
        }));
    }
}
