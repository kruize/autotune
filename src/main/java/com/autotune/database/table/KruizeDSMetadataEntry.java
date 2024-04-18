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

package com.autotune.database.table;

import jakarta.persistence.*;

/**
 * This is a Java class named KruizeDSMetadataEntry annotated with JPA annotations.
 * It represents a table named kruize_dsmetadata in a relational database.
 * <p>
 * The class has the following fields:
 * <p>
 * id: A unique identifier for each experiment detail.
 * version: A String representing the version of the experiment.
 * datasource_name: A String representing the datasource name.
 * cluster_name: A String representing the cluster name.
 * namespace: A String representing the namespace name.
 * workload_type: A String representing the type of workload.
 * workload_name: A String representing the workload name.
 * container_name: A String representing the container name.
 * container_image_name: A String representing the container image name.
 */
@Entity
@Table(name = "kruize_dsmetadata")
public class KruizeDSMetadataEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String version;
    private String datasource_name;
    private String cluster_name;
    private String namespace;
    private String workload_type;
    private String workload_name;
    private String container_name;
    private String container_image_name;
//    TODO: Relation mapping needs to be done later
//    @OneToMany(mappedBy = "metadata", cascade = CascadeType.ALL)
//    private List<KruizeExperimentEntry> experimentEntries;

    public KruizeDSMetadataEntry() {
    }

    public KruizeDSMetadataEntry(String version, String datasource_name, String cluster_name, String namespace, String workload_type, String workload_name, String container_name, String container_image_name) {
        this.version = version;
        this.datasource_name = datasource_name;
        this.cluster_name = cluster_name;
        this.namespace = namespace;
        this.workload_type = workload_type;
        this.workload_name = workload_name;
        this.container_name = container_name;
        this.container_image_name = container_image_name;
    }

    public Long getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDataSourceName() {
        return datasource_name;
    }

    public void setDataSourceName(String datasource_name) {
        this.datasource_name = datasource_name;
    }

    public String getClusterName() {
        return cluster_name;
    }

    public void setClusterName(String cluster_name) {
        this.cluster_name = cluster_name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getWorkloadType() {
        return workload_type;
    }

    public void setWorkloadType(String workload_type) {
        this.workload_type = workload_type;
    }

    public String getWorkloadName() {
        return workload_name;
    }

    public void setWorkloadName(String workload_name) {
        this.workload_name = workload_name;
    }

    public String getContainerName() {
        return container_name;
    }

    public void setContainerName(String container_name) {
        this.container_name = container_name;
    }

    public String getContainerImageName() {
        return container_image_name;
    }

    public void setContainerImageName(String container_image_name) {
        this.container_image_name = container_image_name;
    }

    @Override
    public String toString() {
        return "KruizeDSMetadataEntry{" +
                "id=" + id +
                ", version='" + version + '\'' +
                ", datasource_name='" + datasource_name + '\'' +
                ", cluster_name='" + cluster_name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", workload_type='" + workload_type + '\'' +
                ", workload_name='" + workload_name + '\'' +
                ", container_name='" + container_name + '\'' +
                ", container_image_name='" + container_image_name + '\'' +
                '}';
    }
}

