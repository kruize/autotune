package com.autotune.experimentManager.core.interceptor;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.experimentManager.core.ExperimentTrialHandler;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMLoadInterceptor implements BaseInterceptor {
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
        return EMUtil.InterceptorDetectionStatus.DETECTED;
    }

    /**
     * Load availability based on the datasource result.
     * Returns AVAILABLE if the load variation is found
     * @param experimentTrial
     * @return
     */
    @Override
    public EMUtil.InterceptorAvailabilityStatus isAvailable(ExperimentTrial experimentTrial) {
        // Needs to be implemented
        return EMUtil.InterceptorAvailabilityStatus.AVAILABLE;
    }

    public EMUtil.InterceptorFlowDecision verifyLoadToProceed(ExperimentTrial experimentTrial) {
        boolean proceedToMetricsCollection = false;
        if (EMUtil.InterceptorDetectionStatus.DETECTED == this.detect(experimentTrial)) {
            EMUtil.InterceptorAvailabilityStatus currentAvailability = this.isAvailable(experimentTrial);
            // This loop mechanism will be part of an abstraction later (Threshold loop abstraction)
            // for now just like deployment handler we run a for loop and constantly poll for the load availability
            for (int j = 0; j < EMConstants.StandardDefaults.BackOffThresholds.CHECK_LOAD_AVAILABILITY_THRESHOLD; j++) {
                // Proceed to check if load is available (minimal variation)
                if (EMUtil.InterceptorAvailabilityStatus.AVAILABLE == currentAvailability) {
                    // Will proceed for metric cycles if the load is detected
                    proceedToMetricsCollection = true;
                    // breaking the load availability loop
                    break;
                }
                try {
                    LOGGER.debug("The Load is not yet available, will be checking it again");
                    // Will be replaced by a exponential looper mechanism
                    Thread.sleep(EMUtil.timeToSleep(j) * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                currentAvailability = this.isAvailable(experimentTrial);
            }
            if (EMUtil.InterceptorAvailabilityStatus.AVAILABLE != currentAvailability) {
                LOGGER.debug("Load cannot be detected for the particular trial, proceed to collect metrics if it can be ignored");
                if (experimentTrial.getExperimentSettings().getTrialSettings().isForceCollectMetrics()) {
                    proceedToMetricsCollection = true;
                }
            }
        } else {
            LOGGER.debug("Load cannot be detected for the particular trial, proceed to collect metrics if it can be ignored");
            if (experimentTrial.getExperimentSettings().getTrialSettings().isForceCollectMetrics()) {
                proceedToMetricsCollection = true;
            }
        }
        if (proceedToMetricsCollection) {
            return  EMUtil.InterceptorFlowDecision.PROCEED;
        }
        LOGGER.debug("Proceeding to next trial and this trial cannot be completed due to lack of metrics collection. Will be marked as failed trial");
        return EMUtil.InterceptorFlowDecision.EXIT;
    }
}
