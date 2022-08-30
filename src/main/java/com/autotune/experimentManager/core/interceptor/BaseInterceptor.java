package com.autotune.experimentManager.core.interceptor;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.experimentManager.utils.EMUtil;

/**
 * Base Interceptor need to be implemented by the modules which needs a check
 * for the some entity which is needed to proceed further in EM flow
 *
 * It's named as interceptor as it intercepts a flow to check if that can proceed
 */
public interface BaseInterceptor {
    /**
     * Detect function need to be implemented with the logic responsible for entity detection
     * @param experimentTrial
     * @return
     */
    public EMUtil.InterceptorDetectionStatus detect(ExperimentTrial experimentTrial);

    /**
     * isAvailable need to be implemented with the logic responsible for entity availability
     * @param experimentTrial
     * @return
     */
    public EMUtil.InterceptorAvailabilityStatus isAvailable(ExperimentTrial experimentTrial);
}
