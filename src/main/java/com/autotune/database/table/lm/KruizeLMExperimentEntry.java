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

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.database.helper.GenerateExperimentID;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;
import java.util.Date;


/**
 * This is a Java class named KruizeLMExperimentEntry annotated with JPA annotations.
 * It represents a table named kruize_lm_experiments in a relational database.
 * <p>
 * The class has the following fields:
 * <p>
 * id: A unique identifier for each experiment detail.
 * version: A string representing the version of the experiment.
 * experimentName: A string representing the name of the experiment.
 * clusterName: A string representing the name of the cluster.
 * mode: A string representing the mode of the experiment.
 * targetCluster: A string representing the target cluster for the experiment.
 * performance_profile: A string representing the performance profile for the experiment.
 * metadata_profile: A string representing the metadata profile for the experiment.
 * status: An enum representing the status of the experiment, defined in AnalyzerConstants.ExperimentStatus.
 * extended_data: A JSON object representing extended data for the experiment.
 * meta_data: A string representing metadata for the experiment.
 * experiment_type : Recommendation generation at container, namespace level etc.
 * The ExperimentDetail class also has getters and setters for all its fields.
 */
@Entity
@Table(name = "kruize_lm_experiments")
@IdClass(GenerateExperimentID.class)
public class KruizeLMExperimentEntry {
    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    private String experiment_id;
    private String version;
    @Column(unique = true)
    private String experiment_name;
    private String cluster_name;
    private String mode;
    private String target_cluster;
    private String performance_profile;
    private String metadata_profile;
    @Enumerated(EnumType.STRING)
    private AnalyzerConstants.ExperimentStatus status;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode datasource;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode extended_data;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode meta_data;
    @Enumerated(EnumType.STRING)
    private AnalyzerConstants.ExperimentType experiment_type;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_date", updatable = false)
    private Timestamp creation_date;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    private Timestamp updated_date;

//    TODO: update KruizeDSMetadataEntry


    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getExperiment_name() {
        return experiment_name;
    }

    public void setExperiment_name(String experiment_name) {
        this.experiment_name = experiment_name;
    }

    public String getCluster_name() {
        return cluster_name;
    }

    public void setCluster_name(String cluster_name) {
        this.cluster_name = cluster_name;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTarget_cluster() {
        return target_cluster;
    }

    public void setTarget_cluster(String target_cluster) {
        this.target_cluster = target_cluster;
    }

    public String getPerformance_profile() {
        return performance_profile;
    }

    public void setPerformance_profile(String performance_profile) {
        this.performance_profile = performance_profile;
    }

    public String getMetadata_profile() { return metadata_profile; }

    public void setMetadata_profile(String metadata_profile) { this.metadata_profile = metadata_profile; }

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

    public AnalyzerConstants.ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(AnalyzerConstants.ExperimentStatus status) {
        this.status = status;
    }

    public String getExperiment_id() {
        return experiment_id;
    }

    public void setExperiment_id(String experiment_id) {
        this.experiment_id = experiment_id;
    }

    public JsonNode getDatasource() {
        return datasource;
    }

    public void setDatasource(JsonNode datasource) {
        this.datasource = datasource;
    }

    public AnalyzerConstants.ExperimentType getExperiment_type() {
        return experiment_type;
    }

    public void setExperiment_type(AnalyzerConstants.ExperimentType experiment_type) {
        this.experiment_type = experiment_type;
    }

    public Date getCreation_date() {
        return creation_date;
    }

    public void setCreation_date(Timestamp creation_date) {
        this.creation_date = creation_date;
    }

    public Date getUpdated_date() {
        return updated_date;
    }

    public void setUpdated_date(Timestamp updated_date) {
        this.updated_date = updated_date;
    }

    @Override
    public String toString() {
        return "KruizeLMExperimentEntry{" +
                "experiment_id='" + experiment_id + '\'' +
                ", version='" + version + '\'' +
                ", experiment_name='" + experiment_name + '\'' +
                ", cluster_name='" + cluster_name + '\'' +
                ", mode='" + mode + '\'' +
                ", target_cluster='" + target_cluster + '\'' +
                ", performance_profile='" + performance_profile + '\'' +
                ", metadata_profile='" + metadata_profile + '\'' +
                ", status=" + status +
                ", datasource=" + datasource +
                ", extended_data=" + extended_data +
                ", meta_data=" + meta_data +
                ", experiment_type=" + experiment_type +
                ", creation_date=" + creation_date +
                ", updated_date=" + updated_date +
                '}';
    }
}
