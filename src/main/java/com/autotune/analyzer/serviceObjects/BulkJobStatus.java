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

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.JOB_ID;

/**
 * Bulk API Response payload Object.
 */
@JsonFilter("jobFilter")
public class BulkJobStatus {
    @JsonProperty(JOB_ID)
    private String jobID;
    private String status;
    private int total_experiments;
    private int processed_experiments;
    private Data data;
    @JsonProperty("job_start_time")
    private String startTime; // Change to String to store formatted time
    @JsonProperty("job_end_time")
    private String endTime;   // Change to String to store formatted time
    private String message;

    public BulkJobStatus(String jobID, String status, Data data, Instant startTime) {
        this.jobID = jobID;
        this.status = status;
        this.data = data;
        setStartTime(startTime);
    }

    public String getJobID() {
        return jobID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = formatInstantAsUTCString(startTime);
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = formatInstantAsUTCString(endTime);
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
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

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    // Utility function to format Instant into the required UTC format
    private String formatInstantAsUTCString(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC);  // Ensure it's in UTC

        return formatter.format(instant);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Inner class for the data field
    public static class Data {
        private Experiments experiments;
        private Recommendations recommendations;

        public Data(Experiments experiments, Recommendations recommendations) {
            this.experiments = experiments;
            this.recommendations = recommendations;
        }

        public Experiments getExperiments() {
            return experiments;
        }

        public void setExperiments(Experiments experiments) {
            this.experiments = experiments;
        }

        public Recommendations getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(Recommendations recommendations) {
            this.recommendations = recommendations;
        }
    }

    // Inner class for experiments
    public static class Experiments {
        @JsonProperty("new")
        private List<String> newExperiments;
        @JsonProperty("updated")
        private List<String> updatedExperiments;
        @JsonProperty("failed")
        private List<String> failedExperiments;

        public Experiments(List<String> newExperiments, List<String> updatedExperiments, List<String> failedExperiments) {
            this.newExperiments = newExperiments;
            this.updatedExperiments = updatedExperiments;
            this.failedExperiments = failedExperiments;
        }

        public List<String> getNewExperiments() {
            return newExperiments;
        }

        public void setNewExperiments(List<String> newExperiments) {
            this.newExperiments = newExperiments;
        }

        public List<String> getUpdatedExperiments() {
            return updatedExperiments;
        }

        public void setUpdatedExperiments(List<String> updatedExperiments) {
            this.updatedExperiments = updatedExperiments;
        }

        public List<String> getFailedExperiments() {
            return failedExperiments;
        }

        public void setFailedExperiments(List<String> failedExperiments) {
            this.failedExperiments = failedExperiments;
        }
    }

    // Inner class for recommendations
    public static class Recommendations {
        private RecommendationData data;

        public Recommendations(RecommendationData data) {
            this.data = data;
        }

        public RecommendationData getData() {
            return data;
        }

        public void setData(RecommendationData data) {
            this.data = data;
        }
    }

    // Inner class for recommendation data
    public static class RecommendationData {
        private List<String> processed = Collections.synchronizedList(new ArrayList<>());
        private List<String> processing = Collections.synchronizedList(new ArrayList<>());
        private List<String> unprocessed = Collections.synchronizedList(new ArrayList<>());
        private List<String> failed = Collections.synchronizedList(new ArrayList<>());

        public RecommendationData(List<String> processed, List<String> processing, List<String> unprocessed, List<String> failed) {
            this.processed = processed;
            this.processing = processing;
            this.unprocessed = unprocessed;
            this.failed = failed;
        }

        public List<String> getProcessed() {
            return processed;
        }

        public synchronized void setProcessed(List<String> processed) {
            this.processed = processed;
        }

        public List<String> getProcessing() {
            return processing;
        }

        public synchronized void setProcessing(List<String> processing) {
            this.processing = processing;
        }

        public List<String> getUnprocessed() {
            return unprocessed;
        }

        public synchronized void setUnprocessed(List<String> unprocessed) {
            this.unprocessed = unprocessed;
        }

        public List<String> getFailed() {
            return failed;
        }

        public synchronized void setFailed(List<String> failed) {
            this.failed = failed;
        }

        // Move elements from inqueue to progress
        public synchronized void moveToProgress(String element) {
            if (unprocessed.contains(element)) {
                unprocessed.remove(element);
                if (!processing.contains(element)) {
                    processing.add(element);
                }
            }
        }

        // Move elements from progress to completed
        public synchronized void moveToCompleted(String element) {
            if (processing.contains(element)) {
                processing.remove(element);
                if (!processed.contains(element)) {
                    processed.add(element);
                }
            }
        }

        // Move elements from progress to failed
        public synchronized void moveToFailed(String element) {
            if (processing.contains(element)) {
                processing.remove(element);
                if (!failed.contains(element)) {
                    failed.add(element);
                }
            }
        }

        // Calculate the percentage of completion
        public int completionPercentage() {
            int totalTasks = processed.size() + processing.size() + unprocessed.size() + failed.size();
            if (totalTasks == 0) {
                return (int) 0.0;
            }
            return (int) ((processed.size() * 100.0) / totalTasks);
        }


    }
}
