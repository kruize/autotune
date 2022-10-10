package com.autotune.analyzer.model;

public class QueryData {
    private String name;
    private String query;
    private String datasource;
    private String value_type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getValue_type() {
        return value_type;
    }

    public void setValue_type(String value_type) {
        this.value_type = value_type;
    }

    public QueryData(String name, String query, String datasource, String value_type) {
        this.name = name;
        this.query = query;
        this.datasource = datasource;
        this.value_type = value_type;
    }
}

