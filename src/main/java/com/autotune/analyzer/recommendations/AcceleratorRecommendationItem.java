package com.autotune.analyzer.recommendations;

public class AcceleratorRecommendationItem implements ResourceRecommendation {
    private String model;
    private String partition;
    private Integer count;
    private RecommendationConfigItem compute;
    private RecommendationConfigItem memory;

    public AcceleratorRecommendationItem(String model, String partition, Integer count, RecommendationConfigItem compute, RecommendationConfigItem memory) {
        this.model = model;
        this.partition = partition;
        this.count = count;
        this.compute = compute;
        this.memory = memory;
    }

    public String getModel() {
        return model;
    }

    public String getPartition() {
        return partition;
    }

    public Integer getCount() {
        return count;
    }

    public RecommendationConfigItem getCompute() {
        return compute;
    }

    public RecommendationConfigItem getMemory() {
        return memory;
    }
}
