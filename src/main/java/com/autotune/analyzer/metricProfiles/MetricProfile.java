/*******************************************************************************
 * Copyright (c) 2020, 2024 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.metricProfiles;

import com.autotune.analyzer.kruizeObject.SloInfo;
import com.autotune.analyzer.recommendations.term.Terms;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Container class for the MetricProfile kubernetes kind, which is used to define
 * a profile
 *
 */

public class MetricProfile {

    private String name;

    private double profile_version;

    @SerializedName("k8s_type")
    private String k8s_type;

    @SerializedName("slo")
    private SloInfo sloInfo;

    private Map<String, Terms> terms;

    public void setName(String name) {
        this.name = name;
    }

    public void setProfile_version(double profile_version) {
        this.profile_version = profile_version;
    }

    public String getK8s_type() {
        return k8s_type;
    }

    public void setK8s_type(String k8s_type) {
        this.k8s_type = k8s_type;
    }

    public void setSloInfo(SloInfo sloInfo) {
        this.sloInfo = sloInfo;
    }

    public MetricProfile(String name, double profile_version, String k8s_type, SloInfo sloInfo) {
        this.name = name;
        this.profile_version = profile_version;
        this.k8s_type = k8s_type;
        this.sloInfo = sloInfo;
    }

    public String getName() {
        return name;
    }

    public double getProfile_version() {
        return profile_version;
    }

    public String getK8S_TYPE() {
        return k8s_type;
    }

    public SloInfo getSloInfo() {
        return sloInfo;
    }

    public Map<String, Terms> getTerms() {
        return terms;
    }

    public void setTerms(Map<String, Terms> terms) {
        this.terms = terms;
    }

    @Override
    public String toString() {
        return "MetricProfile{" +
                "name='" + name + '\'' +
                ", profile_version=" + profile_version +
                ", k8s_type='" + k8s_type + '\'' +
                ", sloInfo=" + sloInfo +
                '}';
    }
}
