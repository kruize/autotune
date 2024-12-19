/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.serviceObjects;

import com.autotune.utils.KruizeConstants;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.JOB_ID;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status.UNPROCESSED;

/**
 * Bulk API Response payload Object.
 */
@JsonFilter("jobFilter")
public class BulkJobStatus {
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkJobStatus.class);
    @JsonProperty(JOB_ID)
    private String jobID;
    private String status;
    private int total_experiments;
    private AtomicInteger processed_experiments;  //todo : If the primary operations are increments or simple atomic updates, use AtomicInteger. It is designed for lock-free thread-safe access
    @JsonProperty("job_start_time")
    private String startTime; // Change to String to store formatted time
    @JsonProperty("job_end_time")
    private String endTime;   // Change to String to store formatted time
    private Map<String, Notification> notifications;
    private Map<String, Experiment> experiments = Collections.synchronizedMap(new HashMap<>());
    private Webhook webhook;

    public BulkJobStatus(String jobID, String status, Instant startTime) {
        this.jobID = jobID;
        this.status = status;
        setStartTime(startTime);
        this.processed_experiments = new AtomicInteger(0);
    }


    // Method to set a notification in the map
    public void setNotification(String key, Notification notification) {
        if (this.notifications == null) {
            this.notifications = new HashMap<>(); // Initialize if null
        }
        this.notifications.put(key, notification);
    }

    public String getJobID() {
        return jobID;
    }

    public void setJobID(String jobID) {
        this.jobID = jobID;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = formatInstantAsUTCString(startTime);
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = formatInstantAsUTCString(endTime);
    }

    public Map<String, Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(Map<String, Notification> notifications) {
        this.notifications = notifications;
    }

    public Map<String, Experiment> getExperiments() {
        return experiments;
    }

    public void setExperiments(Map<String, Experiment> experiments) {
        this.experiments = experiments;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public void setWebhook(Webhook webhook) {
        this.webhook = webhook;
    }

    // Method to add a new experiment with "unprocessed" status and null notification
    public synchronized Experiment addExperiment(String experimentName) {
        Experiment experiment = new Experiment(experimentName);
        experiments.put(experimentName, experiment);
        return experiment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotal_experiments() {
        return total_experiments;
    }

    public void setTotal_experiments(int total_experiments) {
        this.total_experiments = total_experiments;
    }


    public void incrementProcessed_experiments() {
        this.processed_experiments.incrementAndGet();
    }

    public AtomicInteger getProcessed_experiments() {
        return processed_experiments;
    }

    public void setProcessed_experiments(int count) {
        this.processed_experiments.set(count);
    }

    // Utility function to format Instant into the required UTC format
    private String formatInstantAsUTCString(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC);  // Ensure it's in UTC

        return formatter.format(instant);
    }


    public static enum NotificationType {
        ERROR("error"),
        WARNING("warning"),
        INFO("info");

        private final String type;

        NotificationType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static class Experiment {
        private String name;
        private Notification notification; // Empty by default
        private Recommendation recommendations;

        public Experiment(String name) {
            this.name = name;
            this.notification = null; // Start with null notification
            this.recommendations = new Recommendation(UNPROCESSED); // Start with unprocessed status
        }

        // Getters and setters
        public Recommendation getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(Recommendation recommendations) {
            this.recommendations = recommendations;
        }

        public Notification getNotification() {
            return notification;
        }

        public void setNotification(Notification notification) {
            this.notification = notification;
        }
    }

    public static class Recommendation {
        private KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status status;
        private Notification notifications; // Notifications can hold multiple entries

        public Recommendation(KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status status) {
            this.status = status;
        }

        // Getters and setters

        public KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status getStatus() {
            return status;
        }

        public void setStatus(KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status status) {
            this.status = status;
        }

        public Notification getNotifications() {
            return notifications;
        }

        public void setNotifications(Notification notifications) {
            this.notifications = notifications;
        }
    }

    public static class Notification {
        private NotificationType type;
        private String message;
        private int code;

        // Constructor, getters, and setters

        public Notification(NotificationType type, String message, int code) {
            this.type = type;
            this.message = message;
            this.code = code;
        }

        public NotificationType getType() {
            return type;
        }

        public void setType(NotificationType type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }
    }

    public static class Webhook {
        private KruizeConstants.KRUIZE_BULK_API.NotificationConstants.WebHookStatus status;
        private Notification notifications; // Notifications can hold multiple entries

        public Webhook(KruizeConstants.KRUIZE_BULK_API.NotificationConstants.WebHookStatus status) {
            this.status = status;
        }

        public KruizeConstants.KRUIZE_BULK_API.NotificationConstants.WebHookStatus getStatus() {
            return status;
        }

        public void setStatus(KruizeConstants.KRUIZE_BULK_API.NotificationConstants.WebHookStatus status) {
            this.status = status;
        }

        public Notification getNotifications() {
            return notifications;
        }

        public void setNotifications(Notification notifications) {
            this.notifications = notifications;
        }
    }

}
