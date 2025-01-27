/*******************************************************************************
 * Copyright (c) 2024 Red Hat, IBM Corporation and others.
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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * This is a Java class named KruizeLMMetadataProfileEntry annotated with JPA annotations.
 * It represents a table named kruize_lm_metadata_profiles in a relational database.
 * <p>
 * The class has the following fields:
 * <p>
 * id: A unique identifier for each metadata profile detail.
 * apiVersion: A string representing version of the Kubernetes API to create this object
 * kind: A string representing type of kubernetes object
 * metadata: A JSON object containing the metadata of the CRD, including name field
 * name: A string representing the name of the metadata profile.
 * profile_version: A string representing the version of the metadata profile.
 * k8s_type: A string representing kubernetes type.
 * query_variables: A JSON object containing metadata queries
 */
@Entity
@Table(name = "kruize_lm_metadata_profiles")
public class KruizeLMMetadataProfileEntry {
    private String api_version;
    private String kind;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode metadata;
    @Id
    private String name;
    private double profile_version;
    private String k8s_type;
    private String datasource;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode query_variables;

    public String getApi_version() {
        return api_version;
    }

    public void setApi_version(String api_version) {
        this.api_version = api_version;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getProfile_version() {
        return profile_version;
    }

    public void setProfile_version(double profile_version) {
        this.profile_version = profile_version;
    }

    public String getK8s_type() {
        return k8s_type;
    }

    public void setK8s_type(String k8s_type) {
        this.k8s_type = k8s_type;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public JsonNode getQuery_variables() {
        return query_variables;
    }

    public void setQuery_variables(JsonNode query_variables) {
        this.query_variables = query_variables;
    }
}
