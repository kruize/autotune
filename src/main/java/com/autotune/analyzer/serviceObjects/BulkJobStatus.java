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
    private int processed_experiments;
    @JsonProperty("job_start_time")
    private String startTime; // Change to String to store formatted time
    @JsonProperty("job_end_time")
    private String endTime;   // Change to String to store formatted time
    private Map<String, KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Notification> notifications;
    private Map<String, Experiment> experiments = Collections.synchronizedMap(new HashMap<>());

    public BulkJobStatus(String jobID, String status, Instant startTime) {
        this.jobID = jobID;
        this.status = status;
        setStartTime(startTime);
    }


    // Method to set a notification in the map
    public void setNotification(String key, KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Notification notification) {
        if (notifications == null) {
            notifications = new HashMap<>(); // Initialize if null
        }
        notifications.put(key, notification);
    }

    public Map<String, Experiment> getExperiments() {
        return experiments;
    }

    public void setExperiments(Map<String, Experiment> experiments) {
        this.experiments = experiments;
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

    public void setStartTime(Instant startTime) {
        this.startTime = formatInstantAsUTCString(startTime);
    }

    public void setEndTime(Instant endTime) {
        this.endTime = formatInstantAsUTCString(endTime);
    }

    public int getTotal_experiments() {
        return total_experiments;
    }

    public void setTotal_experiments(int total_experiments) {
        this.total_experiments = total_experiments;
    }

    public int getProcessed_experiments() {
        return processed_experiments;
    }

    public void setProcessed_experiments(int processed_experiments) {
        this.processed_experiments = processed_experiments;
    }

    // Utility function to format Instant into the required UTC format
    private String formatInstantAsUTCString(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC);  // Ensure it's in UTC

        return formatter.format(instant);
    }


    public static class Experiment {
        private String name;
        private Notification notification; // Empty by default
        private Recommendation recommendation;

        public Experiment(String name) {
            this.name = name;
            this.notification = null; // Start with null notification
            this.recommendation = new Recommendation(UNPROCESSED); // Start with unprocessed status
        }

        // Getters and setters
        public Recommendation getRecommendation() {
            return recommendation;
        }

        public void setNotification(Notification notification) {
            this.notification = notification;
        }
    }

    public static class Recommendation {
        private KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status status;
        private Notification notification; // Notifications can hold multiple entries

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

        public Notification getNotification() {
            return notification;
        }

        public void setNotification(Notification notification) {
            this.notification = notification;
        }
    }

    public static class Notification {
        private String type;
        private String message;
        private int code;

        // Constructor, getters, and setters

        public Notification(String type, String message, int code) {
            this.type = type;
            this.message = message;
            this.code = code;
        }
    }


}
