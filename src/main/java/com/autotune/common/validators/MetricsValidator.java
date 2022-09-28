package com.autotune.common.validators;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialSettings;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.common.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class MetricsValidator {
    private static MetricsValidator metricsValidator = null;
    private MetricsValidator() {

    }

    protected static MetricsValidator getInstance() {
        if (null == metricsValidator) {
            metricsValidator = new MetricsValidator();
        }
        return metricsValidator;
    }

    public static CommonUtils.QueryValidity validateTrialMetrics(ExperimentTrial experimentTrial) {
        HashMap<String, Metric> podMetrics = experimentTrial.getPodMetricsHashMap();
        for (Map.Entry<String, Metric> podMetricEntry : podMetrics.entrySet()) {
            Metric podMetric = podMetricEntry.getValue();
            CommonUtils.QueryValidity validity = validateBaseMetricFeatures(podMetric, experimentTrial.getExperimentSettings().getTrialSettings());
            if (CommonUtils.QueryValidity.VALID != validity) {
                return validity;
            }
            validity = validatePodMetrics(podMetric);
            if (CommonUtils.QueryValidity.VALID != validity) {
                return validity;
            }
        }
        HashMap<String , HashMap<String, Metric>> containerMetricMap = experimentTrial.getContainerMetricsHashMap();
        for (Map.Entry<String, HashMap<String, Metric>> containerMetricMapEntry : containerMetricMap.entrySet()) {
            HashMap<String, Metric> containerMetrics = containerMetricMapEntry.getValue();
            for (Map.Entry<String, Metric> containerMetricEntry : containerMetrics.entrySet()) {
                Metric containerMetric = containerMetricEntry.getValue();
                CommonUtils.QueryValidity validity = validateBaseMetricFeatures(containerMetric, experimentTrial.getExperimentSettings().getTrialSettings());
                if (CommonUtils.QueryValidity.VALID != validity) {
                    return validity;
                }
                validity = validatePodMetrics(containerMetric);
                if (CommonUtils.QueryValidity.VALID != validity) {
                    return validity;
                }
            }
        }
        return CommonUtils.QueryValidity.VALID;
    }

    public static CommonUtils.QueryValidity validatePodMetrics(Metric metric) {
        return CommonUtils.QueryValidity.VALID;
    }

    public static CommonUtils.QueryValidity validateContainerMetrics (Metric metric) {
        return CommonUtils.QueryValidity.VALID;
    }

    private static CommonUtils.QueryValidity validateBaseMetricFeatures(Metric metric, TrialSettings trialSettings) {
        return CommonUtils.QueryValidity.VALID;
    }
}
