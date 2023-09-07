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
package com.autotune.database.table;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;
import java.util.List;

/**
 * This is a Java class named KruizeResultsEntry annotated with JPA annotations.
 * It represents a table named kruize_results in a relational database.
 */
@Entity
@Table(name = "kruize_results",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"experiment_name", "interval_start_time", "interval_end_time"})},
        indexes = @Index(
                name = "idx_result_experiment_name",
                columnList = "experiment_name",
                unique = false))
public class KruizeResultsEntry {
    private String version;
    @Id
    private String experiment_name;
    private Timestamp interval_start_time;
    @Id
    private Timestamp interval_end_time;
    private String cluster_name;
    private double duration_minutes;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode extended_data;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode meta_data;

    @Transient
    private List<String> errorReasons;

    public String getExperiment_name() {
        return experiment_name;
    }

    public void setExperiment_name(String experiment_name) {
        this.experiment_name = experiment_name;
    }

    public Timestamp getInterval_start_time() {
        return interval_start_time;
    }

    public void setInterval_start_time(Timestamp interval_start_time) {
        this.interval_start_time = interval_start_time;
    }

    public Timestamp getInterval_end_time() {
        return interval_end_time;
    }

    public void setInterval_end_time(Timestamp interval_end_time) {
        this.interval_end_time = interval_end_time;
    }

    public double getDuration_minutes() {
        return duration_minutes;
    }

    public void setDuration_minutes(double duration_minutes) {
        this.duration_minutes = duration_minutes;
    }

    public JsonNode getExtended_data() {
        return extended_data;
    }

    public void setExtended_data(JsonNode extended_data) {
        this.extended_data = extended_data;
    }

    public JsonNode getMeta_data() {
        return meta_data;
    }

    public void setMeta_data(JsonNode meta_data) {
        this.meta_data = meta_data;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getErrorReasons() {
        return errorReasons;
    }

    public void setErrorReasons(List<String> errorReasons) {
        this.errorReasons = errorReasons;
    }

    public String getCluster_name() {
        return cluster_name;
    }

    public void setCluster_name(String cluster_name) {
        this.cluster_name = cluster_name;
    }
}
