package com.autotune.utils.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KafkaProducerTask implements Runnable {
    private String bootstrapServers;
    private String topic;
    private String jobId;
    private String message;
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducerTask.class);

    public KafkaProducerTask(String bootstrapServers, String topic, String jobId, String message) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.jobId = jobId;
        this.message = message;
    }

    @Override
    public void run() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(topic, jobId, message), (metadata, e) -> {
                if (e == null) {
                    LOGGER.info("Recommendation sent to Kafka topic: JobID={}, Partition={}, Offset={}",
                            jobId, metadata.partition(), metadata.offset());
                } else {
                    LOGGER.error("Error occurred while sending recommendation for JobID={} : {}", jobId, e.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}