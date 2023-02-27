package com.autotune.analyzer.serviceObjects;

import com.autotune.common.data.result.Results;

public class ContainerMetricsHelper {
    public String name;
    public Results results;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Results getResults() {
        return results;
    }

    public void setResults(Results results) {
        this.results = results;
    }
}
