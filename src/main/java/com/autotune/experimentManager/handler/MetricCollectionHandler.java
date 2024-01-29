/*******************************************************************************
 * Copyright (c)  2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.experimentManager.handler;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.datasource.DataSourceOperator;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.common.datasource.KruizeDataSourceOperator;
import com.autotune.common.k8sObjects.KubernetesContexts;
import com.autotune.common.parallelengine.executor.KruizeExecutor;
import com.autotune.common.parallelengine.worker.CallableFactory;
import com.autotune.common.parallelengine.worker.KruizeWorker;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.common.trials.ExperimentTrial;
import com.autotune.common.trials.TrialDetails;
import com.autotune.common.utils.CommonUtils;
import com.autotune.experimentManager.data.result.CycleMetaData;
import com.autotune.experimentManager.data.result.StepsMetaData;
import com.autotune.experimentManager.data.result.TrialIterationMetaData;
import com.autotune.experimentManager.handler.eminterface.EMHandlerInterface;
import com.autotune.experimentManager.handler.util.EMStatusUpdateHandler;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Metric collection helper class.
 */
public class MetricCollectionHandler implements EMHandlerInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricCollectionHandler.class);

    @Override
    public void execute(ExperimentTrial experimentTrial, TrialDetails trialDetails, TrialIterationMetaData iterationMetaData, StepsMetaData stepsMeatData, KruizeExecutor kruizeExecutor, ServletContext context) {
        try {
            LOGGER.debug("ExperimentName: \"{}\" - TrialNo: {} - Iteration: {} - StepName: {}",
                    experimentTrial.getExperimentName(),
                    trialDetails.getTrialNumber(),
                    iterationMetaData.getIterationNumber(),
                    stepsMeatData.getStepName()
            );
            stepsMeatData.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
            stepsMeatData.setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
            /**
             * Implement MetricCollectionHandler Logic
             */
            // Get the autotune query variable CRD
            CustomResourceDefinitionContext autotuneQueryVariableCRD = KubernetesContexts.getAutotuneVariableContext();
            KubernetesServices kubernetesServices = null;
            try {
                // Initiate Kubernetes service
                kubernetesServices = new KubernetesServicesImpl();
                // Get the env variables map from kubernetes
                // TODO: Move the constants to common constants or Autotune Constants
                Map<String, Object> envVariblesMap = kubernetesServices.getCRDEnvMap(autotuneQueryVariableCRD, "monitoring", KruizeDeploymentInfo.k8s_type);
                ArrayList<Map<String, String>> queryVarList = (ArrayList<Map<String, String>>) envVariblesMap.get(AnalyzerConstants.AutotuneConfigConstants.QUERY_VARIABLES);
                LinkedHashMap<String, LinkedHashMap<Integer, CycleMetaData>> cycleMetaDataMap = new LinkedHashMap<>();
                LinkedHashMap<String, Integer> cycles = new LinkedHashMap<>();
                String warmupCycles = experimentTrial.getExperimentSettings().getTrialSettings().getTrialWarmupCycles();
                String measurementCycles = experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementCycles();
                int warmupCyclesCount = (warmupCycles != null) ? Integer.parseInt(warmupCycles) : -1;
                int measurementCyclesCount = (measurementCycles != null) ? Integer.parseInt(measurementCycles) : -1;
                if (warmupCyclesCount > 0) cycles.put(KruizeConstants.CycleTypes.WARMUP, warmupCyclesCount);
                if (measurementCyclesCount > 0)
                    cycles.put(KruizeConstants.CycleTypes.MEASUREMENT, measurementCyclesCount);
                cycles.forEach((cycleName, count) -> {
                    LinkedHashMap<Integer, CycleMetaData> iterationCycle = new LinkedHashMap<>();
                    IntStream.rangeClosed(1, count).forEach((iteration) -> {
                        String durationTime = null;
                        if (cycleName == KruizeConstants.CycleTypes.WARMUP) {
                            durationTime = experimentTrial.getExperimentSettings().getTrialSettings().getTrialWarmupDuration();
                        } else if (cycleName == KruizeConstants.CycleTypes.MEASUREMENT) {
                            durationTime = experimentTrial.getExperimentSettings().getTrialSettings().getTrialMeasurementDuration();
                        }
                        int timeToSleep = CommonUtils.getTimeToSleepMillis(CommonUtils.getTimeValue(durationTime), CommonUtils.getTimeUnit(durationTime));
                        try {
                            LOGGER.info("Waiting for {} milli seconds to collect metrics", timeToSleep);
                            Thread.sleep(timeToSleep);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        CycleMetaData cycleMetaData = new CycleMetaData();
                        cycleMetaData.setCycleName(cycleName);
                        cycleMetaData.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
                        // Get pod name of the current trial
                        String podName = EMUtil.getCurrentPodNameOfTrial(experimentTrial);
                        // Listing all pod metrics
                        HashMap<String, Metric> podMetricsMap = experimentTrial.getPodMetricsHashMap();
                        for (Map.Entry<String, Metric> podMetricEntry : podMetricsMap.entrySet()) {
                            Metric podMetric = podMetricEntry.getValue();
                            String updatedPodQuery = EMUtil.replaceQueryVars(podMetric.getQuery(), queryVarList);
                            updatedPodQuery = EMUtil.formatQueryByPodName(updatedPodQuery, podName);
                            // Need to run the updated query by calling the datasource
                            DataSourceOperatorImpl op = DataSourceOperatorImpl.getInstance().getOperator(podMetric.getDatasource());
                            if (null == op) {
                                // TODO: Return an error saying unsupported datasource
                            }
                            String queryResult = (String) op.getValueForQuery(experimentTrial.getDatasourceInfoHashMap()
                                    .get(podMetric.getDatasource())
                                    .getUrl().toString(), updatedPodQuery);
                            if (null != queryResult && !queryResult.isEmpty() && !queryResult.isBlank()) {
                                try {
                                    queryResult = queryResult.trim();
                                    LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<Integer, MetricResults>>> trialDataMap = podMetric.getCycleDataMap();
                                    if (!trialDataMap.containsKey(String.valueOf(trialDetails.getTrialNumber()))) {
                                        trialDataMap.put(String.valueOf(trialDetails.getTrialNumber()), new LinkedHashMap<String, LinkedHashMap<Integer, MetricResults>>());
                                    }
                                    LinkedHashMap<String, LinkedHashMap<Integer, MetricResults>> metricCycleDataMap = trialDataMap.get(String.valueOf(trialDetails.getTrialNumber()));
                                    if (!metricCycleDataMap.containsKey(cycleName)) {
                                        metricCycleDataMap.put(cycleName, new LinkedHashMap<Integer, MetricResults>());
                                    }
                                    MetricResults metricResults = new MetricResults();
                                    metricResults.getAggregationInfoResult().setAvg(Double.parseDouble(queryResult));
                                    metricCycleDataMap.get(cycleName).put(iteration, metricResults);
                                    LOGGER.debug("Query Result - {}", queryResult);
                                } catch (Exception e) {
                                    LOGGER.error("The Query result - {} cannot be parsed as float", queryResult);
                                }
                            }
                        }
                        HashMap<String, HashMap<String, Metric>> containersMap = experimentTrial.getContainerMetricsHashMap();
                        for (Map.Entry<String, HashMap<String, Metric>> containerMapEntry : containersMap.entrySet()) {
                            String containerName = containerMapEntry.getKey();
                            LOGGER.debug("Container name - " + containerName);
                            for (Map.Entry<String, Metric> containerMetricEntry : containerMapEntry.getValue().entrySet()) {
                                Metric containerMetric = containerMetricEntry.getValue();
                                String updatedContainerQuery = EMUtil.replaceQueryVars(containerMetric.getQuery(), queryVarList);
                                updatedContainerQuery = EMUtil.formatQueryByPodName(updatedContainerQuery, podName);
                                updatedContainerQuery = EMUtil.formatQueryByContainerName(updatedContainerQuery, containerName);
                                // Need to run the updated query by calling the datasource
                                DataSourceOperatorImpl op = DataSourceOperatorImpl.getInstance().getOperator(containerMetric.getDatasource());
                                if (null == op) {
                                    // TODO: Return an error saying unsupported datasource
                                }
                                if (null != updatedContainerQuery) {
                                    LOGGER.debug("Updated Query - " + updatedContainerQuery);
                                    String queryResult = (String) op.getValueForQuery(experimentTrial.getDatasourceInfoHashMap()
                                            .get(containerMetric.getDatasource())
                                            .getUrl().toString(), updatedContainerQuery);
                                    if (null != queryResult && !queryResult.isEmpty() && !queryResult.isBlank()) {
                                        try {
                                            queryResult = queryResult.trim();
                                            LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<Integer, MetricResults>>> trialDataMap = containerMetric.getCycleDataMap();
                                            if (!trialDataMap.containsKey(String.valueOf(trialDetails.getTrialNumber()))) {
                                                trialDataMap.put(String.valueOf(trialDetails.getTrialNumber()), new LinkedHashMap<String, LinkedHashMap<Integer, MetricResults>>());
                                            }
                                            LinkedHashMap<String, LinkedHashMap<Integer, MetricResults>> metricCycleDataMap = trialDataMap.get(String.valueOf(trialDetails.getTrialNumber()));
                                            if (!metricCycleDataMap.containsKey(cycleName)) {
                                                metricCycleDataMap.put(cycleName, new LinkedHashMap<Integer, MetricResults>());
                                            }
                                            DecimalFormat df = new DecimalFormat("0.00");
                                            df.setMaximumFractionDigits(2);
                                            MetricResults metricResults = new MetricResults();
                                            Double resultFloat = Double.parseDouble(queryResult);
                                            if (containerMetric.getName().equalsIgnoreCase(EMConstants.QueryNames.Container.CPU_REQUEST)) {
                                                metricResults.getAggregationInfoResult().setFormat("cores");

                                            } else if (containerMetric.getName().equalsIgnoreCase(EMConstants.QueryNames.Container.MEMORY_REQUEST)
                                                    || containerMetric.getName().equalsIgnoreCase(EMConstants.QueryNames.Container.GC)) {
                                                resultFloat = (Double) EMUtil.convertToMiB(resultFloat, EMUtil.MemoryUnits.BYTES);
                                                LOGGER.debug("Result float from util - " + resultFloat);
                                                metricResults.getAggregationInfoResult().setFormat("MiB");
                                            }
                                            LOGGER.debug("Result float before- " + resultFloat);
                                            resultFloat = Double.parseDouble(df.format(resultFloat));
                                            LOGGER.debug("Result float after - " + resultFloat);
                                            metricResults.getAggregationInfoResult().setAvg(resultFloat);
                                            metricCycleDataMap.get(cycleName).put(iteration, metricResults);
                                            LOGGER.debug("Query Result - " + queryResult);
                                        } catch (Exception e) {
                                            LOGGER.error("The Query result - {} cannot be parsed as float", queryResult);
                                        }
                                    }
                                }
                            }
                        }
                        iterationCycle.put(iteration, cycleMetaData);
                    });
                    cycleMetaDataMap.put(cycleName, iterationCycle);
                });
                // Summarization of metrics collected
                HashMap<String, Metric> podMetricsMap = experimentTrial.getPodMetricsHashMap();
                for (Map.Entry<String, Metric> podMetricEntry : podMetricsMap.entrySet()) {
                    Metric podMetric = podMetricEntry.getValue();
                    LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<Integer, MetricResults>>> trialDataMap = podMetric.getCycleDataMap();
                    if (!trialDataMap.containsKey(String.valueOf(trialDetails.getTrialNumber()))) {
                        trialDataMap.put(String.valueOf(trialDetails.getTrialNumber()), new LinkedHashMap<String, LinkedHashMap<Integer, MetricResults>>());
                    }
                    LinkedHashMap<String, LinkedHashMap<Integer, MetricResults>> metricCycleDataMap = trialDataMap.get(String.valueOf(trialDetails.getTrialNumber()));
                    if (metricCycleDataMap.containsKey(KruizeConstants.CycleTypes.MEASUREMENT)) {
                        LinkedHashMap<Integer, MetricResults> measurementMap = metricCycleDataMap.get(KruizeConstants.CycleTypes.MEASUREMENT);
                        double sumVal = 0;
                        int removableEntries = 0;
                        for (Map.Entry<Integer, MetricResults> measurementMapEntry : measurementMap.entrySet()) {
                            MetricResults metricResults = measurementMapEntry.getValue();
                            if (Float.MIN_VALUE == metricResults.getAggregationInfoResult().getAvg())
                                removableEntries = removableEntries + 1;
                            else
                                sumVal = sumVal + metricResults.getAggregationInfoResult().getAvg();
                        }
                        double avgVal = sumVal / (measurementMap.size() - removableEntries);
                        MetricResults metricResults = new MetricResults();
                        metricResults.getAggregationInfoResult().setAvg(avgVal);
                        podMetric.getTrialSummaryResult().put(String.valueOf(trialDetails.getTrialNumber()), metricResults);
                        podMetric.setMetricResult(metricResults);
                    }
                }
                HashMap<String, HashMap<String, Metric>> containersMap = experimentTrial.getContainerMetricsHashMap();
                for (Map.Entry<String, HashMap<String, Metric>> containerMapEntry : containersMap.entrySet()) {
                    String containerName = containerMapEntry.getKey();
                    LOGGER.debug("Container name - " + containerName);
                    for (Map.Entry<String, Metric> containerMetricEntry : containerMapEntry.getValue().entrySet()) {
                        Metric containerMetric = containerMetricEntry.getValue();
                        LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<Integer, MetricResults>>> trialDataMap = containerMetric.getCycleDataMap();
                        if (!trialDataMap.containsKey(String.valueOf(trialDetails.getTrialNumber()))) {
                            trialDataMap.put(String.valueOf(trialDetails.getTrialNumber()), new LinkedHashMap<String, LinkedHashMap<Integer, MetricResults>>());
                        }
                        LinkedHashMap<String, LinkedHashMap<Integer, MetricResults>> metricCycleDataMap = trialDataMap.get(String.valueOf(trialDetails.getTrialNumber()));
                        if (metricCycleDataMap.containsKey(KruizeConstants.CycleTypes.MEASUREMENT)) {
                            LinkedHashMap<Integer, MetricResults> measurementMap = metricCycleDataMap.get(KruizeConstants.CycleTypes.MEASUREMENT);
                            double sumVal = 0;
                            int removableEntries = 0;
                            for (Map.Entry<Integer, MetricResults> measurementMapEntry : measurementMap.entrySet()) {
                                MetricResults metricResults = measurementMapEntry.getValue();
                                if (Float.MIN_VALUE == metricResults.getAggregationInfoResult().getAvg())
                                    removableEntries = removableEntries + 1;
                                else
                                    sumVal = sumVal + metricResults.getAggregationInfoResult().getAvg();
                            }
                            double avgVal = sumVal / (measurementMap.size() - removableEntries);
                            MetricResults metricResults = new MetricResults();
                            if (containerMetric.getName().equalsIgnoreCase(EMConstants.QueryNames.Container.CPU_REQUEST)) {
                                metricResults.getAggregationInfoResult().setFormat("cores");

                            } else if (containerMetric.getName().equalsIgnoreCase(EMConstants.QueryNames.Container.MEMORY_REQUEST)
                                    || containerMetric.getName().equalsIgnoreCase(EMConstants.QueryNames.Container.GC)) {
                                metricResults.getAggregationInfoResult().setFormat("MiB");
                            }
                            metricResults.getAggregationInfoResult().setAvg(avgVal);
                            containerMetric.getTrialSummaryResult().put(String.valueOf(trialDetails.getTrialNumber()), metricResults);
                            containerMetric.setMetricResult(metricResults);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (kubernetesServices != null) {
                    kubernetesServices.shutdownClient();
                }
            }

            stepsMeatData.setEndTimestamp(new Timestamp(System.currentTimeMillis()));
            stepsMeatData.setStatus(EMUtil.EMExpStatus.COMPLETED);
            EMStatusUpdateHandler.updateTrialIterationDataStatus(experimentTrial, trialDetails, iterationMetaData);
            EMStatusUpdateHandler.updateTrialMetaDataStatus(experimentTrial, trialDetails);
            EMStatusUpdateHandler.updateExperimentTrialMetaDataStatus(experimentTrial);
            kruizeExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            KruizeWorker theWorker = new CallableFactory().create(kruizeExecutor.getWorker());
                            theWorker.execute(null, experimentTrial, kruizeExecutor, context);
                        }
                    }
            );
        } catch (Exception e) {
            trialDetails.getTrialMetaData().setStatus(EMUtil.EMExpStatus.FAILED);
            e.printStackTrace();
            LOGGER.error("Failed to execute MetricCollectionHandler ExperimentName: \"{}\" - TrialNo: {} - Iteration: {} - StepName: {} -- {}",
                    experimentTrial.getExperimentName(),
                    trialDetails.getTrialNumber(),
                    iterationMetaData.getIterationNumber(),
                    stepsMeatData.getStepName(),
                    e.getMessage()
            );
        }
    }
}
