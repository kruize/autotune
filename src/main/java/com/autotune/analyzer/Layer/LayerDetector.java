package com.autotune.analyzer.Layer;

import com.google.gson.annotations.SerializedName;

public class LayerDetector {
    private String type;

    private String datasource;

    private String query;

    private String key;

    @SerializedName("non_null_is_present")
    private boolean nonNullIsPresent;

    public LayerDetector(String type, String datasource, String query, String key, boolean nonNullIsPresent) {
        this.type = type;
        this.datasource = datasource;
        this.query = query;
        this.key = key;
        this.nonNullIsPresent = nonNullIsPresent;
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
}
