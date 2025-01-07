package com.autotune.utils.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KafkaConsumerTask implements Runnable {
    private String bootstrapServers;
    private String topic;
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerTask.class);


    public KafkaConsumerTask(String bootstrapServers, String topic) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
    }

    @Override
    public void run() {
        KafkaConsumer<String, String> consumer = getStringStringKafkaConsumer();

        try (consumer) {
            consumer.subscribe(java.util.Collections.singletonList(topic));
            while (true) {
                consumer.poll(java.time.Duration.ofMillis(100)).forEach(record -> {
                    LOGGER.info("Received Recommendation: JobID={}, Value={}, Partition={}, Offset={}",
                            record.key(), record.value(), record.partition(), record.offset());
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private KafkaConsumer<String, String> getStringStringKafkaConsumer() {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "recommendation-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new KafkaConsumer<>(consumerProps);
    }
}