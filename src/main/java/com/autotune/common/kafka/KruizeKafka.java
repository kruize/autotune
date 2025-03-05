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

/**
 * Represents a Kafka Object with a topic and message content.
 */
public class KruizeKafka {
    private final String topic;
    private final String message;

    /**
     * Constructs a KruizeKafka instance with the specified topic and message.
     *
     * @param topic   the Kafka topic to which the message belongs
     * @param message the message content
     */
    public KruizeKafka(String topic, String message) {
        this.topic = topic;
        this.message = message;
    }

    /**
     * Retrieves the Kafka topic of this message.
     *
     * @return the topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Retrieves the content of the message.
     *
     * @return the message content
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns a string representation of this Kafka message.
     *
     * @return a string containing the topic and message content
     */
    @Override
    public String toString() {
        return "KruizeKafka{" +
                "topic='" + topic + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
