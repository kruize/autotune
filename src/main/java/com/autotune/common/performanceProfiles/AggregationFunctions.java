package com.autotune.common.performanceProfiles;

public class AggregationFunctions {

    private String function;
    private String query;
    private String version;

    public AggregationFunctions(String function, String query, String version) {
        this.function = function;
        this.query = query;
        this.version = version;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
