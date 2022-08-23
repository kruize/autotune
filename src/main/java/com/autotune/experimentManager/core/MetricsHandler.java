package com.autotune.experimentManager.core;

import com.autotune.common.data.metrics.EMMetricResult;
import com.autotune.common.data.metrics.iteration.IterationResult;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.PodContainer;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.TrialDataStore;
import com.autotune.experimentManager.utils.EMConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MetricsHandler {
    public static void collectMetrics(ExperimentTrial experimentTrial, int currentIteration) {
        HashMap<String, Metric> podMetrics = experimentTrial.getTrialDetails().get(EMConstants.EMConfigDeployments.DeploymentTypes.TRAINING).getPodMetrics();
        HashMap<String, PodContainer> containerList = experimentTrial.getTrialDetails().get(EMConstants.EMConfigDeployments.DeploymentTypes.TRAINING).getPodContainers();
        int warmupCycles = Integer.parseInt(experimentTrial.getExperimentSettings().getTrialSettings().getTrialWarmupCycles());
        int measurementCycles = Integer.parseInt(experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementCycles());
        int totalCycles = warmupCycles + measurementCycles;
        for (int i = 0; i < totalCycles; i++) {
            for (Map.Entry<String,Metric> metric : podMetrics.entrySet()) {
                if (i == 0) {
                    IterationResult iterationResult = new IterationResult();
                    metric.getValue().getIterationResults().add(iterationResult);
                }
                IterationResult iterationResult = metric.getValue().getIterationResults().get(currentIteration - 1);
                EMMetricResult emMetricResult = new EMMetricResult();
                if (i < warmupCycles) {
                    iterationResult.addToWarmUpList(emMetricResult);
                } else {
                    iterationResult.addToMeasurementList(emMetricResult);
                }
            }
            for (Map.Entry<String, PodContainer> pod : containerList.entrySet()) {
                HashMap<String, Metric> containerMetrics = pod.getValue().getContainerMetrics();
                for (Map.Entry<String, Metric> metric : containerMetrics.entrySet()) {
                    if (i == 0) {
                        IterationResult iterationResult = new IterationResult();
                        metric.getValue().getIterationResults().add(iterationResult);
                    }
                    IterationResult iterationResult = metric.getValue().getIterationResults().get(currentIteration - 1);
                    EMMetricResult emMetricResult = new EMMetricResult();
                    if (i < warmupCycles) {
                        iterationResult.addToWarmUpList(emMetricResult);
                    } else {
                        iterationResult.addToMeasurementList(emMetricResult);
                    }
                }
            }
        }

    }
}
