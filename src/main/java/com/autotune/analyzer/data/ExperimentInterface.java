package com.autotune.analyzer.data;

import java.util.List;

public interface ExperimentInterface {
    public List<ExperimentData> getALLExperiments();
    public List<ExperimentData> getALLExperiments(String status);
    public ExperimentData getExperimentBy(String clusterID,String namespace,String deployment);
    public boolean saveExperiment(ExperimentData experimentData);
    public boolean updateExperiment(ExperimentData experimentData);
}
