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
 * It represents a table named kruize_metadata in a relational database.
 * <p>
 * The class has the following fields:
 * <p>
 * id: A unique identifier for each experiment detail.
 * version: A String representing the version of the experiment.
 * clusterGroupName: A String representing the datasource name.
 * clusterName: A String representing the cluster name.
 * namespace: A String representing the namespace name.
 * workloadType: A String representing the type of workload.
 * workloadName: A String representing the workload name.
 * containerName: A String representing the container name.
 * containerImageName: A String representing the container image name.
 */
@Entity
@Table(name = "kruize_dsmetadata")
public class KruizeDSMetadataEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String version;
    private String clusterGroupName;
    private String clusterName;
    private String namespace;
    private String workloadType;
    private String workloadName;
    private String containerName;
    private String containerImageName;
//    TODO: Relation mapping needs to be done later
//    @OneToMany(mappedBy = "metadata", cascade = CascadeType.ALL)
//    private List<KruizeExperimentEntry> experimentEntries;

    public KruizeDSMetadataEntry() {
    }

    public KruizeDSMetadataEntry(String version, String clusterGroupName, String clusterName, String namespace, String workloadType, String workloadName, String containerName, String containerImageName) {
        this.version = version;
        this.clusterGroupName = clusterGroupName;
        this.clusterName = clusterName;
        this.namespace = namespace;
        this.workloadType = workloadType;
        this.workloadName = workloadName;
        this.containerName = containerName;
        this.containerImageName = containerImageName;
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

    public String getClusterGroupName() {
        return clusterGroupName;
    }

    public void setClusterGroupName(String clusterGroupName) {
        this.clusterGroupName = clusterGroupName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getWorkloadType() {
        return workloadType;
    }

    public void setWorkloadType(String workloadType) {
        this.workloadType = workloadType;
    }

    public String getWorkloadName() {
        return workloadName;
    }

    public void setWorkloadName(String workloadName) {
        this.workloadName = workloadName;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getContainerImageName() {
        return containerImageName;
    }

    public void setContainerImageName(String containerImageName) {
        this.containerImageName = containerImageName;
    }

    @Override
    public String toString() {
        return "KruizeDSMetadataEntry{" +
                "id=" + id +
                ", version='" + version + '\'' +
                ", clusterGroupName='" + clusterGroupName + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", namespace='" + namespace + '\'' +
                ", workloadType='" + workloadType + '\'' +
                ", workloadName='" + workloadName + '\'' +
                ", containerName='" + containerName + '\'' +
                ", containerImageName='" + containerImageName + '\'' +
                '}';
    }
}

