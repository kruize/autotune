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
    private String cluster_group_name;
    private String cluster_name;
    private String namespace;
    private String workload_name;
    private String workload_type;
    private String container_name;
    private String container_image_name;
//    TODO: Relation mapping needs to be done later
//    @OneToMany(mappedBy = "metadata", cascade = CascadeType.ALL)
//    private List<KruizeExperimentEntry> experimentEntries;

    public KruizeMetadata() {
    }

    public KruizeMetadata(String version, String cluster_group_name, String cluster_name, String namespace, String workload_type, String workload_name, String container_name, String container_image_name) {
        this.version = version;
        this.cluster_group_name = cluster_group_name;
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

    public String getClusterGroupName() {
        return cluster_group_name;
    }

    public void setClusterGroupName(String cluster_group_name) {
        this.cluster_group_name = cluster_group_name;
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
        return "KruizeMetadata{" +
                "id=" + id +
                ", version='" + version + '\'' +
                ", clusterGroupName='" + cluster_group_name + '\'' +
                ", clusterName='" + cluster_name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", workloadType='" + workload_type + '\'' +
                ", workloadName='" + workload_name + '\'' +
                ", containerName='" + container_name + '\'' +
                ", containerImageName='" + container_image_name + '\'' +
                '}';
    }
}