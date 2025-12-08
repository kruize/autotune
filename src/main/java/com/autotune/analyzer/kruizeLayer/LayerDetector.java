/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.kruizeLayer;

import com.google.gson.annotations.SerializedName;

public class LayerDetector {

   // For query-based detection
    private String datasource;
    private String query;
    private String key;

    // For label-based detection
    private String name;
    private String value;

    public LayerDetector() {
    }
    // Constructor for query-based detection
    public LayerDetector(String datasource, String query, String key) {
        this.datasource = datasource;
        this.query = query;
        this.key = key;
    }

    // Constructor for label-based detection
    public LayerDetector(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "LayerDetector{" +
                "datasource='" + datasource + '\'' +
                ", query='" + query + '\'' +
                ", key='" + key + '\'' +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}

