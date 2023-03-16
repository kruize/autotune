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
package com.autotune.dbactivites.model;

import com.autotune.utils.AnalyzerConstants;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * This is a Java class named ExperimentDetail annotated with JPA annotations.
 * It represents a table named experiment_detail in a relational database.
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
 * status: An enum representing the status of the experiment, defined in AnalyzerConstants.ExperimentStatus.
 * extended_data: A JSON object representing extended data for the experiment.
 * meta_data: A string representing metadata for the experiment.
 * The ExperimentDetail class also has getters and setters for all its fields.
 */
@Entity
@Table(name = "experiment_detail")
public class ExperimentDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String version;
    @Column(unique = true)
    private String experimentName;
    private String clusterName;
    private String mode;
    private String targetCluster;
    private String performance_profile;
    @Enumerated(EnumType.STRING)
    private AnalyzerConstants.ExperimentStatus status;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode extended_data;
    private String meta_data;


    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTargetCluster() {
        return targetCluster;
    }

    public void setTargetCluster(String targetCluster) {
        this.targetCluster = targetCluster;
    }

    public String getPerformance_profile() {
        return performance_profile;
    }

    public void setPerformance_profile(String performance_profile) {
        this.performance_profile = performance_profile;
    }

    public JsonNode getExtended_data() {
        return extended_data;
    }

    public void setExtended_data(JsonNode extended_data) {
        this.extended_data = extended_data;
    }

    public String getMeta_data() {
        return meta_data;
    }

    public void setMeta_data(String meta_data) {
        this.meta_data = meta_data;
    }

    public AnalyzerConstants.ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(AnalyzerConstants.ExperimentStatus status) {
        this.status = status;
    }

}
