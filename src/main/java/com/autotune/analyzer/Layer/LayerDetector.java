package com.autotune.analyzer.Layer;

import com.google.gson.annotations.SerializedName;

public class LayerDetector {
    private String type;

    // For query-based detection
    private String datasource;
    private String query;
    private String key;
    @SerializedName("non_null_is_present")
    private Boolean nonNullIsPresent;

    // For label-based detection
    private String name;
    private String value;

    // Constructor for query-based detection
    public LayerDetector(String type, String datasource, String query, String key, Boolean nonNullIsPresent) {
        this.type = type;
        this.datasource = datasource;
        this.query = query;
        this.key = key;
        this.nonNullIsPresent = nonNullIsPresent;
    }

    // Constructor for label-based detection
    public LayerDetector(String type, String name, String value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public boolean isNonNullIsPresent() {
        return nonNullIsPresent;
    }

    public void setNonNullIsPresent(boolean nonNullIsPresent) {
        this.nonNullIsPresent = nonNullIsPresent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
