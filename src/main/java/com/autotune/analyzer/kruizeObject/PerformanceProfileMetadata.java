package com.autotune.analyzer.kruizeObject;

public class PerformanceProfileMetadata {
    private String name;

    public PerformanceProfileMetadata(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "PerformanceProfileMetadata{" +
                "name='" + name + '\'' +
                '}';
    }
}
