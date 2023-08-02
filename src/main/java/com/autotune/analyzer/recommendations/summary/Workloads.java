package com.autotune.analyzer.recommendations.summary;

import java.util.List;

public class Workloads {
    private int count;

    private List<String> names;

    public Workloads(int count, List<String> names) {
        this.count = count;
        this.names = names;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }
}