/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.validators;

import com.autotune.common.trials.ExperimentTrial;
import com.autotune.common.trials.TrialSettings;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Class which is responsible for all metrics related validations
 */
public class MetricsValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsValidator.class);
    private static MetricsValidator metricsValidator = null;

    /**
     * private constructor as we shouldn't create instances of Metrics Validator outside this class
     * as we maintain this as a singleton
     */
    private MetricsValidator() {

    }

    /**
     * Create the instance of metrics validator if it's not initialised and return it
     * made it protected as only Validator should be able to call this function.
     * @return
     */
    protected static MetricsValidator getInstance() {
        if (null == metricsValidator) {
            metricsValidator = new MetricsValidator();
        }
        return metricsValidator;
    }

    /**
     * Validates all the queries in the experiment trial
     * @param experimentTrial
     * @return
     */
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

    /**
     * Validates the metrics based on pod level aspects
     * TODO: Needs to be implemented
     * @param metric
     * @return
     */
    public static CommonUtils.QueryValidity validatePodMetrics(Metric metric) {
        return CommonUtils.QueryValidity.VALID;
    }

    /**
     * Validates the metrics based on container level aspects
     * TODO: Needs to be implemented
     * @param metric
     * @return
     */
    public static CommonUtils.QueryValidity validateContainerMetrics (Metric metric) {
        return CommonUtils.QueryValidity.VALID;
    }

    /**
     * Validates different aspects of the query at a generic level such as
     *  - if time range in query matches the trial cycle duration
     *  etc
     *  TODO: Other aspects other than query
     * @param metric
     * @param trialSettings
     * @return
     */
    private static CommonUtils.QueryValidity validateBaseMetricFeatures(Metric metric, TrialSettings trialSettings) {
        String query = metric.getQuery();
        // Check if query is null and return NULL QUERY as validity
        if (null == query) {
            LOGGER.error("Invalid Metric - {} : Query - {} is null", metric.getName(), metric.getQuery());
            return CommonUtils.QueryValidity.NULL_QUERY;
        }
        // Check if query is empty and return EMPTY QUERY as validity
        if (query.isEmpty()) {
            LOGGER.error("Invalid Metric - {} : Query - {} is empty", metric.getName(), metric.getQuery());
            return CommonUtils.QueryValidity.EMPTY_QUERY;
        }
        // Check if query has a time range
        boolean timeRangeFound = CommonUtils.checkIfQueryHasTimeRange(query);
        if (timeRangeFound) {
            // Extract time range from query
            String timeContent = CommonUtils.extractTimeUnitFromQuery(query);
            // Check the time match and return VALID if match found
            boolean checkTimeMatch = CommonUtils.checkTimeMatch(timeContent, trialSettings.getTrialMeasurementDuration());
            if (!checkTimeMatch) {
                LOGGER.error("Invalid Metric - {} : Query - {} is invalid as the time range in query doesn't match the trial cycle duration", metric.getName(), metric.getQuery());
                return CommonUtils.QueryValidity.INVALID_RANGE;
            }
        }
        /**
         * Need to check other possibilities but for now we return valid
         */
        LOGGER.debug("Metric - {} is valid", metric.getName());
        return CommonUtils.QueryValidity.VALID;
    }
}
