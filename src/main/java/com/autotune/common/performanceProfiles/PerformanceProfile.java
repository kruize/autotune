/*******************************************************************************
 * Copyright (c) 2020, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.performanceProfiles;

import com.autotune.common.k8sObjects.SloInfo;

/**
 * Container class for the PerformanceProfile kubernetes kind, which is used to define
 * a profile
 *
 */

public class PerformanceProfile {

    private final double profile_version;

    private final String k8s_type;

    private final SloInfo sloInfo;

    public PerformanceProfile(double profile_version, String k8s_type, SloInfo sloInfo) {
        this.profile_version = profile_version;
        this.k8s_type = k8s_type;
        this.sloInfo = sloInfo;
    }

    public double getProfile_version() {
        return profile_version;
    }

    public String getK8s_type() {
        return k8s_type;
    }

    public SloInfo getSloInfo() {
        return sloInfo;
    }

    @Override
    public String toString() {
        return "PerformanceProfile{" +
                ", profile_version=" + profile_version +
                ", k8s_type='" + k8s_type + '\'' +
                ", sloInfoArrayList=" + sloInfo +
                '}';
    }
}
