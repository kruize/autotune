package com.autotune.experimentManager.data.result;

public class AutoTuneWorkFlow {
    private boolean preValidation;
    private boolean deployConfig;
    private boolean postValidation;
    private boolean loadValidation;
    private boolean metricCollection;
    private boolean summarizer;

    public AutoTuneWorkFlow(boolean preValidation, boolean deployConfig, boolean postValidation, boolean loadValidation, boolean metricCollection, boolean summarizer) {
        this.preValidation = preValidation;
        this.deployConfig = deployConfig;
        this.postValidation = postValidation;
        this.loadValidation = loadValidation;
        this.metricCollection = metricCollection;
        this.summarizer = summarizer;
    }

    public boolean isPreValidation() {
        return preValidation;
    }

    public boolean isDeployConfig() {
        return deployConfig;
    }

    public boolean isPostValidation() {
        return postValidation;
    }

    public boolean isLoadValidation() {
        return loadValidation;
    }

    public boolean isMetricCollection() {
        return metricCollection;
    }

    public boolean isSummarizer() {
        return summarizer;
    }
}
