package com.autotune.database.table;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * This is a Java class named KruizeDataSource annotated with JPA annotations.
 * It represents a table named kruize_data_source in a relational database.
 * <p>
 * The class has the following fields:
 * <p>
 * id: A unique identifier for each experiment detail.
 * version: A String representing the version of the experiment.
 * name: A string representing the name of the datasource.
 * provider: A string representing the datasource provider name.
 * serviceName: A string representing the name of the service.
 * namespace: A string representing the namespace name.
 * url: A string representing the URL of the service. */

@Entity
@Table(name = "kruize_datasources")
public class KruizeDataSource {
    @Id
    private String version;
    private String name;
    private String provider;
    private String serviceName;
    private String namespace;
    private String url;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
