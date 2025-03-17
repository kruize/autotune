/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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
package com.autotune.database.table.lm;

import com.autotune.analyzer.serviceObjects.BulkInput;
import com.autotune.analyzer.serviceObjects.BulkJobStatus;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * DB entity to store Kruize bulk job details
 * like processed and total count , Start and End time , Experiment etc.
 */
@Entity
@Table(name = "kruize_bulkjobs")
public class BulkJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkJob.class);
    @Id
    @Column(name = "job_id", columnDefinition = "VARCHAR(36)")
    private String jobId;
    private String status;
    @Column(name = "total_count")
    private int totalExperiments;
    @Column(name = "processed_count")
    private int processedExperiments;
    @Column(name = "start_time")
    private Timestamp jobStartTime;
    @Column(name = "end_time")
    private Timestamp jobEndTime;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode webhook;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode notifications; // Stored as JSON string
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode experiments; // JSONB field for experiments data
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode metadata; // JSONB field for experiments data
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload; // JSONB field for experiments data

    public BulkJob(String jobId, String status, int totalExperiments, int processedExperiments, Timestamp jobStartTime, Timestamp jobEndTime, String webhook, String notifications, String experiments, String metadata, String payload) {
        this.jobId = jobId;
        this.status = status;
        this.totalExperiments = totalExperiments;
        this.processedExperiments = processedExperiments;
        this.jobStartTime = jobStartTime;
        this.jobEndTime = jobEndTime;
        try {
            this.webhook = new ObjectMapper().readTree(webhook);
            this.notifications = new ObjectMapper().readTree(notifications);
            this.experiments = new ObjectMapper().readTree(experiments);
            this.metadata = new ObjectMapper().readTree(metadata);
            this.payload = new ObjectMapper().readTree(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    public BulkJob() {

    }

    // Getters and Setters

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalExperiments() {
        return totalExperiments;
    }

    public void setTotalExperiments(int totalExperiments) {
        this.totalExperiments = totalExperiments;
    }

    public int getProcessedExperiments() {
        return processedExperiments;
    }

    public void setProcessedExperiments(int processedExperiments) {
        this.processedExperiments = processedExperiments;
    }

    public Timestamp getJobStartTime() {
        return jobStartTime;
    }

    public void setJobStartTime(Timestamp jobStartTime) {
        this.jobStartTime = jobStartTime;
    }

    public Timestamp getJobEndTime() {
        return jobEndTime;
    }

    public void setJobEndTime(Timestamp jobEndTime) {
        this.jobEndTime = jobEndTime;
    }

    public JsonNode getWebhook() {
        return webhook;
    }

    public void setWebhook(JsonNode webhook) {
        this.webhook = webhook;
    }

    public JsonNode getNotifications() {
        return notifications;
    }

    public void setNotifications(JsonNode notifications) {
        this.notifications = notifications;
    }

    public JsonNode getExperiments() {
        return experiments;
    }

    public void setExperiments(JsonNode experiments) {
        this.experiments = experiments;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "BulkJob{" +
                "jobId=" + jobId +
                ", status='" + status + '\'' +
                ", totalExperiments=" + totalExperiments +
                ", processedExperiments=" + processedExperiments +
                ", jobStartTime=" + jobStartTime +
                ", jobEndTime=" + jobEndTime +
                ", webhook='" + webhook + '\'' +
                ", notifications='" + notifications + '\'' +
                ", experiments='" + experiments + '\'' +
                ", metadata='" + metadata + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }

    public BulkJobStatus getBulkJobStatus() {
        BulkJobStatus jobStatus = new BulkJobStatus(
                new BulkJobStatus.Summary(
                        jobId,
                        status,
                        totalExperiments,
                        processedExperiments,
                        jobStartTime,
                        jobEndTime,
                        convertJsonNodeToMap(notifications),
                        convertJsonNodeToBulkInput(payload)),
                convertJsonNodeToExperimentsMap(experiments.get("experiments")),
                null,
                convertJsonNodeToMetaData(metadata)
        );
        return jobStatus;

    }


    public Map<String, BulkJobStatus.Notification> convertJsonNodeToMap(JsonNode jsonNode) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (jsonNode == null || jsonNode.isNull()) {
            return new HashMap<>(); // Return an empty map if null
        }

        try {
            return objectMapper.convertValue(jsonNode, objectMapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, BulkJobStatus.Notification.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JsonNode to Map<String, Notification>", e);
        }
    }

    public BulkInput convertJsonNodeToBulkInput(JsonNode jsonNode) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (jsonNode == null || jsonNode.isNull()) {
            return null; // Return null if the JsonNode is empty
        }

        try {
            return objectMapper.treeToValue(jsonNode, BulkInput.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JsonNode to BulkInput", e);
        }
    }

    public Map<String, BulkJobStatus.Experiment> convertJsonNodeToExperimentsMap(JsonNode jsonNode) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (jsonNode == null || jsonNode.isNull()) {
            return null; // Return null if the JsonNode is empty
        }
        try {
            return objectMapper.convertValue(jsonNode, objectMapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, BulkJobStatus.Experiment.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JsonNode to BulkJobStatus.Experiment", e);
        }
    }

    public DataSourceMetadataInfo convertJsonNodeToMetaData(JsonNode jsonNode) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (jsonNode == null || jsonNode.isNull()) {
            return null; // Return null if the JsonNode is empty
        }

        try {
            return objectMapper.treeToValue(jsonNode, DataSourceMetadataInfo.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JsonNode to MetaData", e);
        }
    }

}
