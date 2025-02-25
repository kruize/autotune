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

import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataInfo;
import com.autotune.database.table.lm.BulkJob;
import com.autotune.utils.KruizeConstants;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static com.autotune.analyzer.serviceObjects.BulkJobStatus.Summary.parseUTCStringToTimestamp;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.*;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status.UNPROCESSED;

/**
 * Bulk API Response payload Object.
 */
@JsonFilter(JOB_FILTER)
public class BulkJobStatus {
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkJobStatus.class);
    private Summary summary;
    private Map<String, Experiment> experiments = Collections.synchronizedMap(new HashMap<>());
    @JsonIgnore
    private Map<String, Experiment> experimentMap = Collections.synchronizedMap(new HashMap<>());
    private Webhook webhook;
    private DataSourceMetadataInfo metadata;


    public BulkJobStatus(String jobID, String status, Instant startTime, BulkInput input) {
        this.summary = new Summary(jobID, status, startTime, input);
    }

    public BulkJobStatus(Summary summary, Map<String, Experiment> experimentMap, Webhook webhook, DataSourceMetadataInfo metadata) {
        this.summary = summary;
        this.experimentMap = experimentMap;
        this.webhook = webhook;
        this.metadata = metadata;
    }

    // Utility function to format Instant into the required UTC format
    private static String formatInstantAsUTCString(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC);  // Ensure it's in UTC

        return formatter.format(instant);
    }

    public Map<String, Experiment> getExperimentMap() {
        return experimentMap;
    }

    public void setExperimentMap(Map<String, Experiment> experimentMap) {
        this.experimentMap = experimentMap;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public void setWebhook(Webhook webhook) {
        this.webhook = webhook;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    // Method to add a new experiment with "unprocessed" status and null notification
    public synchronized Experiment addExperiment(String experimentName) {
        Experiment experiment = new Experiment(experimentName);
        experimentMap.put(experimentName, experiment);
        return experiment;
    }

    public DataSourceMetadataInfo getMetadata() {
        return metadata;
    }

    public void setMetadata(DataSourceMetadataInfo metadata) {
        this.metadata = metadata;
    }

    public Map<String, Experiment> getExperiments() {
        return experiments;
    }

    public void setExperiments(Map<String, Experiment> experiments) {
        this.experiments = experiments;
    }

    /**
     * Copies entries from {@code experimentMap} to {@code experiments} based on a specified pattern.
     * <p>
     * If the provided regex is {@code null}, all entries from {@code experimentMap} are copied
     * into {@code experiments}. If a regex is specified, only the entries whose keys match
     * the pattern are copied.
     * <p>
     * This method clears the current contents of {@code experiments} before performing the copy operation.
     * Access to {@code experimentMap} is synchronized to ensure thread safety.
     *
     * @param regex the regular expression used to filter entries by key, or {@code null} to copy all entries.
     *
     * <pre>
     * Example Usage:
     * Given a map containing:
     * {"test1" -> "value1", "example2" -> "value2", "sample3" -> "value3"}
     *
     * copyByPattern("test") will result in:
     * {"test1" -> "value1"} being copied to {@code experiments}.
     * </pre>
     *
     */
    public void copyByPattern(String regex) {

        experiments.clear();
        if (null != experimentMap) {
            synchronized (experimentMap) {
                if (regex == null) {
                    experiments.putAll(experimentMap);
                } else {
                    Pattern pattern = Pattern.compile(".*" + Pattern.quote(regex) + ".*");
                    experimentMap.forEach((key, value) -> {
                        if (pattern.matcher(key).matches()) {
                            experiments.put(key, value); // Direct reference copy
                        }
                    });
                }
            }
        }

    }

    public BulkJob getBulkJobForDB(String experimentsString) throws Exception {
        BulkJob bulkJob = null;
        try {
            bulkJob = new BulkJob(getSummary().getJobID(),
                    getSummary().getStatus(),
                    getSummary().getTotal_experiments(),
                    getSummary().getProcessed_experiments().get(),
                    parseUTCStringToTimestamp(getSummary().getStartTime()),
                    parseUTCStringToTimestamp(getSummary().getEndTime()),
                    new ObjectMapper().writeValueAsString(getWebhook()),
                    new ObjectMapper().writeValueAsString(getSummary().getNotifications()),
                    experimentsString,
                    new ObjectMapper().writeValueAsString(getMetadata()),
                    new ObjectMapper().writeValueAsString(getSummary().getInput())
            );
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return bulkJob;
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

    @JsonFilter(SUMMARY_FILTER)
    public static class Summary {
        @JsonProperty(JOB_ID)
        private String jobID;
        private String status;
        private int total_experiments;
        private AtomicInteger processed_experiments;
        @JsonProperty("job_start_time")
        private String startTime; // Change to String to store formatted time
        @JsonProperty("job_end_time")
        private String endTime;
        // Change to String to store formatted time
        private Map<String, Notification> notifications;
        private BulkInput input = new BulkInput();

        public Summary(String jobID, String status, Instant startTime, BulkInput input) {
            this.jobID = jobID;
            this.status = status;
            this.input = input;
            setStartTime(startTime);
            this.processed_experiments = new AtomicInteger(0);
        }

        public Summary(String jobID, String status, int total_experiments, int processed_experiments, Timestamp startTime, Timestamp endTime, Map<String, Notification> notifications, BulkInput input) {
            this.jobID = jobID;
            this.status = status;
            this.total_experiments = total_experiments;
            this.processed_experiments = new AtomicInteger(processed_experiments);
            this.startTime = formatInstantAsUTCString(startTime.toInstant());
            this.endTime = (endTime != null) ? formatInstantAsUTCString(endTime.toInstant()) : null;
            this.notifications = notifications;
            this.input = input;
        }

        // Utility function to parse string to java.sql.Timestamp in UTC
        static Timestamp parseUTCStringToTimestamp(String utcString) {
            if (null != utcString) {
                DateTimeFormatter formatter = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .withZone(ZoneOffset.UTC);

                // Parse the instant and create a timestamp
                Instant instant = Instant.from(formatter.parse(utcString));
                return Timestamp.from(instant);
            } else {
                return null;
            }
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
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

        public int getTotal_experiments() {
            return total_experiments;
        }

        public void setTotal_experiments(int total_experiments) {
            this.total_experiments = total_experiments;
        }

        // Utility function to format Instant into the required UTC format
        private String formatInstantAsUTCString(Instant instant) {
            DateTimeFormatter formatter = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .withZone(ZoneOffset.UTC);  // Ensure it's in UTC

            return formatter.format(instant);
        }

        // Method to set a notification in the map
        public void setNotification(String key, Notification notification) {
            if (this.notifications == null) {
                this.notifications = new HashMap<>(); // Initialize if null
            }
            this.notifications.put(key, notification);
        }

        public Map<String, Notification> getNotifications() {
            return notifications;
        }

        public void setNotifications(Map<String, Notification> notifications) {
            this.notifications = notifications;
        }

        public BulkInput getInput() {
            return input;
        }

        public void setInput(BulkInput input) {
            this.input = input;
        }
    }

    @JsonFilter(EXPERIMENTS_FILTER)
    public static class API_Response {
        private CreateExperimentAPIResponse create = new CreateExperimentAPIResponse();
        private GenerateRecommendationsAPIResponse recommendations = new GenerateRecommendationsAPIResponse();

        public API_Response() {
        }

        public CreateExperimentAPIResponse getCreate() {
            return create;
        }

        public void setCreate(CreateExperimentAPIResponse create) {
            this.create = create;
        }

        public GenerateRecommendationsAPIResponse getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(GenerateRecommendationsAPIResponse recommendations) {
            this.recommendations = recommendations;
        }
    }

    @JsonFilter(EXPERIMENTS_FILTER)
    public static class CreateExperimentAPIResponse {
        private KruizeResponse response;
        private CreateExperimentAPIObject request;

        public CreateExperimentAPIResponse() {
        }

        public KruizeResponse getResponse() {
            return response;
        }

        public void setResponse(KruizeResponse response) {
            this.response = response;
        }

        public CreateExperimentAPIObject getRequest() {
            return request;
        }

        public void setRequest(CreateExperimentAPIObject request) {
            this.request = request;
        }
    }

    @JsonFilter(EXPERIMENTS_FILTER)
    public static class GenerateRecommendationsAPIResponse {

        Object response = null;

        public GenerateRecommendationsAPIResponse() {
        }

        public GenerateRecommendationsAPIResponse(Object response) {
            if (response instanceof List) {
                this.response = Optional.of((List<ListRecommendationsAPIObject>) response);
            }
            if (response instanceof KruizeResponse) {
                this.response = Optional.of((KruizeResponse) response);
            }

        }

        public static Optional<List<ListRecommendationsAPIObject>> handleListResponse(Object response) {
            if (response instanceof List) {
                return Optional.of((List<ListRecommendationsAPIObject>) response);
            }
            return Optional.empty();
        }

        public static Optional<KruizeResponse> handleKruizeResponse(Object response) {
            if (response instanceof KruizeResponse) {
                return Optional.of((KruizeResponse) response);
            }
            return Optional.empty();
        }

        public Object getResponse() {
            return response;
        }

        public void setResponse(Object response) {
            this.response = response;
        }


    }

    @JsonFilter(EXPERIMENTS_FILTER)
    public static class Experiment {

        private String name;
        private KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status status;
        private API_Response apis = new API_Response();
        private List<StatusHistory> status_history = new ArrayList<>();

        public Experiment(String name) {
            this.name = name;
            this.status = UNPROCESSED; // Start with unprocessed status
            status_history.add(new StatusHistory(UNPROCESSED, Instant.now()));
        }

        public Experiment() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public API_Response getApis() {
            return apis;
        }

        public void setApis(API_Response apis) {
            this.apis = apis;
        }

        public KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status getStatus() {
            return status;
        }

        public void setStatus(KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status status) {
            status_history.add(new StatusHistory(status, Instant.now()));
            this.status = status;
        }

        public List<StatusHistory> getStatus_history() {
            return status_history;
        }

        public void setStatus_history(List<StatusHistory> status_history) {
            this.status_history = status_history;
        }
    }

    public static class StatusHistory {
        private KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status status;
        private String timestamp;

        public StatusHistory(KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status status, Instant timestamp) {
            this.status = status;
            this.timestamp = formatInstantAsUTCString(timestamp);
        }

        public StatusHistory() {
        }

        public KruizeConstants.KRUIZE_BULK_API.NotificationConstants.Status getStatus() {
            return status;
        }

        public String getTimestamp() {
            return timestamp;
        }

    }

    public static class Notification {
        private NotificationType type;
        private String message;
        private int code;

        // Constructor, getters, and setters

        public Notification() {
        }

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
