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
 * This is a Java class named KruizeDataSourceEntry annotated with JPA annotations.
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
public class KruizeDataSourceEntry {
    private String version;
    @Id
    private String name;
    private String provider;
    private String serviceName;
    private String namespace;
    private String url;
    @ManyToOne // Assuming many datasources can reference the same auth credentials
    @JoinColumn(name = "authentication_id") // Foreign key column in the datasource table
    private KruizeAuthenticationEntry kruizeAuthenticationEntry;

    public KruizeAuthenticationEntry getKruizeAuthenticationEntry() {
        return kruizeAuthenticationEntry;
    }

    public void setKruizeAuthenticationEntry(KruizeAuthenticationEntry kruizeAuthenticationEntry) {
        this.kruizeAuthenticationEntry = kruizeAuthenticationEntry;
    }

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
