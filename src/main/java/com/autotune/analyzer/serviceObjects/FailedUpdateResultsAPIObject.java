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
package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.List;

public class FailedUpdateResultsAPIObject extends BaseSO {
    @SerializedName(KruizeConstants.JSONKeys.INTERVAL_START_TIME)
    public Timestamp startTimestamp;

    @SerializedName(KruizeConstants.JSONKeys.INTERVAL_END_TIME)
    public Timestamp endTimestamp;

    private List<KruizeResponse> errors;

    public FailedUpdateResultsAPIObject(String version, String experiment_name, Timestamp startTimestamp, Timestamp endTimestamp, List<KruizeResponse> errors) {
        this.setApiVersion(version);
        this.setExperimentName(experiment_name);
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.errors = errors;
    }
}
