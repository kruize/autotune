/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.serviceObjects.verification.annotators.ExperimentNameExist;
import com.google.gson.annotations.SerializedName;
import org.hibernate.validator.constraints.NotBlank;


public abstract class BaseSO {
    @NotBlank(groups = InitialValidation.class)
    @SerializedName("version")
    private String apiVersion;
    @NotBlank(groups = InitialValidation.class)
    @ExperimentNameExist(groups = ExperimentNameExistValidation.class)
    @SerializedName("experiment_name")
    private String experimentName;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public interface InitialValidation {
    }

    public interface ExperimentNameExistValidation {
    }

}
