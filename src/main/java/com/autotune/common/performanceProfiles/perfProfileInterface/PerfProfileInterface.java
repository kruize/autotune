package com.autotune.common.performanceProfiles.perfProfileInterface;

import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.common.k8sObjects.Metric;

import java.util.ArrayList;
import java.util.Set;

public interface PerfProfileInterface {

    boolean validate(KruizeObject kruizeObject);

    void recommend();
}
