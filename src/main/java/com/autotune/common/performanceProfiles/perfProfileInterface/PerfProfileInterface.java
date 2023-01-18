package com.autotune.common.performanceProfiles.perfProfileInterface;

import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.k8sObjects.KruizeObject;

public interface PerfProfileInterface {

    String validate(KruizeObject kruizeObject, ExperimentResultData experimentResultData);

    void recommend();
}
