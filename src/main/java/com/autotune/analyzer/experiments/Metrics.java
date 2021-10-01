package com.autotune.analyzer.experiments;

public class Metrics {
    String name;
    String query;
    String datasource;

    public Metrics(String name, String query, String datasource) {
        this.name = name;
        this.query = query;
        this.datasource = datasource;
    }

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
}
