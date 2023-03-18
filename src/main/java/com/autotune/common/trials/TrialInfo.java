/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.trials;

import com.google.gson.annotations.SerializedName;

/**
 * This class is used to hold high level information about Trails.
 * Example
 *   "trial_info": {
 *         "trial_id": "",
 *         "trial_num": 0,
 *         "trial_result_url": "http://localhost:8080/listExperiments?experiment_name=quarkus-resteasy-autotune-min-http-response-time-db"
 *     },
 */
public class TrialInfo {
    @SerializedName("trial_id")
    private final String trialId;
    @SerializedName("trial_num")
    private final int trialNum;
    /**
     * URL is used to postback metric results back to analyser.
     */
    @SerializedName("trial_result_url")
    private final String trialResultURL;

    public TrialInfo(String trialId,
                     int trialNum,
                     String trialResultURL) {
        this.trialId = trialId;
        this.trialNum = trialNum;
        this.trialResultURL = trialResultURL;
    }

    public String getTrialId() {
        return trialId;
    }

    public int getTrialNum() {
        return trialNum;
    }

    public String getTrialResultURL() {
        return trialResultURL;
    }


    @Override
    public String toString() {
        return "TrialInfo{" +
                "trialId='" + trialId + '\'' +
                ", trialNum=" + trialNum +
                ", trialResultURL='" + trialResultURL + '\'' +
                '}';
    }
}
