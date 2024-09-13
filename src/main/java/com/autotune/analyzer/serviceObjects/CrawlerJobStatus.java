package com.autotune.analyzer.serviceObjects;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CrawlerJobStatus {
    private String jobID;
    private String status;
    private int progress;
    private Data data;
    @JsonProperty("start_time")
    private String startTime; // Change to String to store formatted time
    @JsonProperty("end_time")
    private String endTime;   // Change to String to store formatted time

    public CrawlerJobStatus(String jobID, String status, int progress, Data data, Instant startTime) {
        this.jobID = jobID;
        this.status = status;
        this.progress = progress;
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

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = formatInstantAsUTCString(startTime);
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = formatInstantAsUTCString(endTime);
    }

    // Utility function to format Instant into the required UTC format
    private String formatInstantAsUTCString(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC);  // Ensure it's in UTC

        return formatter.format(instant);
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

        public Experiments(List<String> newExperiments, List<String> updatedExperiments) {
            this.newExperiments = newExperiments;
            this.updatedExperiments = updatedExperiments;
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
    }

    // Inner class for recommendations
    public static class Recommendations {
        @JsonProperty("count")
        private int totalCount;
        @JsonProperty("completed")
        private int completedCount;
        private RecommendationData data;

        public Recommendations(int totalCount, int completedCount, RecommendationData data) {
            this.totalCount = totalCount;
            this.completedCount = completedCount;
            this.data = data;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public int getCompletedCount() {
            return this.data.getCompleted().size();
        }

        public void setCompletedCount(int completedCount) {
            this.completedCount = completedCount;
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
        private List<String> completed;
        private List<String> progress;
        private List<String> inqueue;
        private List<String> failed;

        public RecommendationData(List<String> completed, List<String> progress, List<String> inqueue, List<String> failed) {
            this.completed = completed;
            this.progress = progress;
            this.inqueue = inqueue;
            this.failed = failed;
        }

        public List<String> getCompleted() {
            return completed;
        }

        public void setCompleted(List<String> completed) {
            this.completed = completed;
        }

        public List<String> getProgress() {
            return progress;
        }

        public void setProgress(List<String> progress) {
            this.progress = progress;
        }

        public List<String> getInqueue() {
            return inqueue;
        }

        public void setInqueue(List<String> inqueue) {
            this.inqueue = inqueue;
        }

        public List<String> getFailed() {
            return failed;
        }

        public void setFailed(List<String> failed) {
            this.failed = failed;
        }

        // Move elements from inqueue to progress
        public void moveToProgress(String element) {
            if (inqueue.contains(element)) {
                inqueue.remove(element);
                if (!progress.contains(element)) {
                    progress.add(element);
                }
            }
        }

        // Move elements from progress to completed
        public void moveToCompleted(String element) {
            if (progress.contains(element)) {
                progress.remove(element);
                if (!completed.contains(element)) {
                    completed.add(element);
                }
            }
        }

        // Move elements from progress to failed
        public void moveToFailed(String element) {
            if (progress.contains(element)) {
                progress.remove(element);
                if (!failed.contains(element)) {
                    failed.add(element);
                }
            }
        }

        // Calculate the percentage of completion
        public int completionPercentage() {
            int totalTasks = completed.size() + progress.size() + inqueue.size() + failed.size();
            if (totalTasks == 0) {
                return (int) 0.0;
            }
            return (int) ((completed.size() * 100.0) / totalTasks);
        }
    }


}
