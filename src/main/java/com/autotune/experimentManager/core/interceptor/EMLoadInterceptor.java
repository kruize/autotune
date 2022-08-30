package com.autotune.experimentManager.core.interceptor;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.experimentManager.utils.EMUtil;

public class EMLoadInterceptor implements BaseInterceptor {
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
}
