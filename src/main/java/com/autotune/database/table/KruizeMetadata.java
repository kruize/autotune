package com.autotune.database.table;

import jakarta.persistence.*;

import java.util.List;

/**
 * This is a Java class named KruizeMetadata annotated with JPA annotations.
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
@Table(name = "kruize_metadata")
public class KruizeMetadata {
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
    @OneToMany(mappedBy = "metadata", cascade = CascadeType.ALL)
    private List<KruizeExperimentEntry> experimentEntries;

    public KruizeMetadata() {
    }

    public KruizeMetadata(String version, String clusterGroupName, String clusterName, String namespace, String workloadType, String workloadName, String containerName, String containerImageName) {
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

    public List<KruizeExperimentEntry> getExperimentEntries() {
        return experimentEntries;
    }

    public void setExperimentEntries(List<KruizeExperimentEntry> experimentEntries) {
        this.experimentEntries = experimentEntries;
    }

    @Override
    public String toString() {
        return "KruizeMetadata{" +
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