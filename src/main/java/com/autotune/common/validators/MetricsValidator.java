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

    public CommonUtils.QueryValidity validateTrialMetrics(ExperimentTrial experimentTrial) {
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
                validity = validateContainerMetrics(containerMetric);
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
        String query = metric.getQuery();
        if (null == query) {
            return CommonUtils.QueryValidity.NULL_QUERY;
        }
        if (query.isEmpty()) {
            return CommonUtils.QueryValidity.EMPTY_QUERY;
        }
        boolean timeRangeFound = CommonUtils.checkIfQueryHasTimeRange(query);
        if (timeRangeFound) {
            String timeContent = CommonUtils.extractTimeUnitFromQuery(query);
            boolean checkTimeMatch = CommonUtils.checkTimeMatch(timeContent, trialSettings.getTrialMeasurementDuration());
            if (checkTimeMatch) {
                return CommonUtils.QueryValidity.VALID;
            }
            return CommonUtils.QueryValidity.INVALID_RANGE;
        }
        /**
         * Need to check other possibilities but for now we return valid
         */
        return CommonUtils.QueryValidity.VALID;
    }
}
