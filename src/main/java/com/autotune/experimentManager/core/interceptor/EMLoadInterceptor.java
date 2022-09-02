package com.autotune.experimentManager.core.interceptor;

import com.autotune.common.experiments.ExperimentTrial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class EMLoadInterceptor implements LoadInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMLoadInterceptor.class);
    /**
     * Load detection module which returns DETECTED if the load can be detected
     * with attributes provided in experiment trial object
     * @param experimentTrial
     * @return
     */
    @Override
    public EMUtil.InterceptorDetectionStatus detect(ExperimentTrial experimentTrial) {
        // Needs to be implemented
        boolean detectable = false;
        HashMap<String, Metric> podMetrics = experimentTrial.getPodMetricsHashMap();
        String podName = EMUtil.getCurrentPodNameOfTrial(experimentTrial);
        for (Map.Entry<String, Metric> podMetricEntry : podMetrics.entrySet()) {
            Metric podMetric = podMetricEntry.getValue();
            JSONObject resultJSON = EMUtil.runMetricQuery(podMetric.getQuery(), podName, podMetric.getDatasource());
            detectable = EMUtil.isMetricResultValid(podMetric.getName(), podMetric.getDatasource(), resultJSON);
            if (detectable) {
                return EMUtil.InterceptorDetectionStatus.DETECTED;
            }
        }
        return EMUtil.InterceptorDetectionStatus.NOT_DETECTED;
    }

    /**
     * Load availability based on the datasource result.
     * Returns AVAILABLE if the load variation is found
     * @param experimentTrial
     * @return
     */
    @Override
    public EMUtil.InterceptorAvailabilityStatus isAvailable(ExperimentTrial experimentTrial) {
        // Set ready for load
        // Needs to be implemented
        return EMUtil.InterceptorAvailabilityStatus.AVAILABLE;
    }

    @Override
    public EMUtil.LoadAvailabilityStatus isLoadAvailable(ExperimentTrial experimentTrial) {
        if (EMUtil.InterceptorDetectionStatus.DETECTED == this.detect(experimentTrial)) {
            EMUtil.InterceptorAvailabilityStatus currentAvailability = this.isAvailable(experimentTrial);
            // This loop mechanism will be part of an abstraction later (Threshold loop abstraction)
            // for now just like deployment handler we run a for loop and constantly poll for the load availability
            for (int j = 0; j < EMConstants.StandardDefaults.BackOffThresholds.CHECK_LOAD_AVAILABILITY_THRESHOLD; j++) {
                // Proceed to check if load is available (minimal variation)
                if (EMUtil.InterceptorAvailabilityStatus.AVAILABLE == currentAvailability) {
                    // Will proceed for metric cycles if the load is detected
                    return  EMUtil.LoadAvailabilityStatus.LOAD_AVAILABLE;
                }
                try {
                    LOGGER.debug("The Load is not yet available, will be checking it again");
                    // Will be replaced by a exponential looper mechanism
                    Thread.sleep(EMUtil.timeToSleep(j, EMUtil.ThresholdIntervalType.EXPONENTIAL) * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                currentAvailability = this.isAvailable(experimentTrial);
            }
        }
        LOGGER.debug("Proceeding to next trial and this trial cannot be completed due to lack of metrics collection. Will be marked as failed trial");
        return EMUtil.LoadAvailabilityStatus.LOAD_NOT_AVAILABLE;
    }
}
