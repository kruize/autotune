package com.autotune.analyzer.data;

import java.util.List;

public class ExperimentInterfaceImpl implements ExperimentInterface{
    private List<ExperimentData> allExperimentsList;

    public ExperimentInterfaceImpl(List<ExperimentData> allExperimentsList) {
        this.allExperimentsList = allExperimentsList;
    }

    @Override
    public List<ExperimentData> getALLExperiments() {
        return  this.allExperimentsList;
    }

    @Override
    public List<ExperimentData> getALLExperiments(String status) {
        return null;
    }

    @Override
    public ExperimentData getExperimentBy(String clusterID, String namespace, String deployment) {
        return null;
    }

    @Override
    public boolean saveExperiment(ExperimentData experimentData) {
        return false;
    }

    @Override
    public boolean updateExperiment(ExperimentData experimentData) {
        return false;
    }
}
