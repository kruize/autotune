package com.autotune.experimentManager.core;

import com.autotune.common.data.metrics.EMMetricResult;
import com.autotune.common.data.metrics.iteration.IterationResult;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.PodContainer;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.experimentManager.data.EMMapper;
import com.autotune.experimentManager.data.TrialDataStore;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import com.autotune.utils.GenericRestApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MetricsHandler {

    public static void collectMetrics(ExperimentTrial experimentTrial, int currentIteration) {
        HashMap<String, Metric> podMetrics = experimentTrial.getTrialDetails().get(EMConstants.EMConfigDeployments.DeploymentTypes.TRAINING).getPodMetrics();
        HashMap<String, PodContainer> containerList = experimentTrial.getTrialDetails().get(EMConstants.EMConfigDeployments.DeploymentTypes.TRAINING).getPodContainers();
        int warmupCycles = Integer.parseInt(experimentTrial.getExperimentSettings().getTrialSettings().getTrialWarmupCycles());
        int measurementCycles = Integer.parseInt(experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementCycles());
        int totalCycles = warmupCycles + measurementCycles;
        int warmupDurationValue = EMUtil.getTimeValue(experimentTrial.getExperimentSettings().getTrialSettings().getTrialWarmupDuration());
        int measurementDurationValue = EMUtil.getTimeValue(experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementDuration());
        TimeUnit warmupUnit = EMUtil.getTimeUnit(experimentTrial.getExperimentSettings().getTrialSettings().getTrialWarmupDuration());
        TimeUnit measurementUnit = EMUtil.getTimeUnit(experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementDuration());
        String warmUpTimeUnitForCycle = EMUtil.getShortRepOfTimeUnit(warmupUnit);
        String measurementTimeUnitForCycle = EMUtil.getShortRepOfTimeUnit(measurementUnit);
        GenericRestApiClient apiClient = new GenericRestApiClient(
                EMUtil.getBaseDataSourceUrl(
                        experimentTrial.getDatasourceInfo().getUrl().toString(),
                        experimentTrial.getDatasourceInfo().getName()
                )
        );
        if (null != apiClient) {
            for (int i = 0; i < totalCycles; i++) {
                for (Map.Entry<String,Metric> metric : podMetrics.entrySet()) {
                    if (i == 0) {
                        IterationResult iterationResult = new IterationResult();
                        metric.getValue().getIterationResults().add(iterationResult);
                    }
                    IterationResult iterationResult = metric.getValue().getIterationResults().get(currentIteration - 1);
                    if (i < warmupCycles) {
                        EMMetricResult emMetricResult = EMUtil.getPodMetricResult(apiClient,
                                metric.getValue().getDatasource(),
                                metric.getValue().getQuery(),
                                experimentTrial.getNamespace(),
                                warmUpTimeUnitForCycle,
                                String.valueOf(warmupDurationValue));
                        if (null != emMetricResult)
                            iterationResult.addToWarmUpList(emMetricResult);
                    } else {
                        EMMetricResult emMetricResult = EMUtil.getPodMetricResult(apiClient,
                                metric.getValue().getDatasource(),
                                metric.getValue().getQuery(),
                                experimentTrial.getNamespace(),
                                measurementTimeUnitForCycle,
                                String.valueOf(measurementDurationValue));
                        if (null != emMetricResult)
                            iterationResult.addToMeasurementList(emMetricResult);
                    }
                }
                for (Map.Entry<String, PodContainer> pod : containerList.entrySet()) {
                    HashMap<String, Metric> containerMetrics = pod.getValue().getContainerMetrics();
                    String containerName = pod.getValue().getContainerName();
                    for (Map.Entry<String, Metric> metric : containerMetrics.entrySet()) {
                        if (i == 0) {
                            IterationResult iterationResult = new IterationResult();
                            metric.getValue().getIterationResults().add(iterationResult);
                        }
                        IterationResult iterationResult = metric.getValue().getIterationResults().get(currentIteration - 1);
                        if (i < warmupCycles) {
                            EMMetricResult emMetricResult = EMUtil.getContainerMetricResult(apiClient,
                                    containerName,
                                    metric.getValue().getDatasource(),
                                    metric.getValue().getQuery(),
                                    experimentTrial.getNamespace(),
                                    warmUpTimeUnitForCycle,
                                    String.valueOf(warmupDurationValue));
                            if (null != emMetricResult)
                                iterationResult.addToWarmUpList(emMetricResult);
                        } else {
                            EMMetricResult emMetricResult = EMUtil.getContainerMetricResult(apiClient,
                                    containerName,
                                    metric.getValue().getDatasource(),
                                    metric.getValue().getQuery(),
                                    experimentTrial.getNamespace(),
                                    measurementTimeUnitForCycle,
                                    String.valueOf(measurementDurationValue));
                            if (null != emMetricResult)
                                iterationResult.addToMeasurementList(emMetricResult);
                        }
                    }
                }
                int sleepSecs = 0;
                if (i < warmupCycles){
                    sleepSecs = warmupDurationValue * EMUtil.getTimeUnitInSeconds(warmupUnit);
                } else {
                    sleepSecs = measurementDurationValue * EMUtil.getTimeUnitInSeconds(measurementUnit);
                }
                try {
                    System.out.println("Sleeping for " + sleepSecs + " seconds");
                    Thread.sleep(sleepSecs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
