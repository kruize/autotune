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

import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.verification.annotators.CompareDate;
import com.autotune.analyzer.serviceObjects.verification.annotators.KubernetesElementsCheck;
import com.autotune.analyzer.serviceObjects.verification.annotators.MetricProfileCheck;
import com.autotune.analyzer.serviceObjects.verification.annotators.TimeDifferenceCheck;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.sql.Timestamp;
import java.util.List;

@CompareDate(groups = BaseSO.InitialValidation.class, message = AnalyzerErrorConstants.AutotuneObjectErrors.WRONG_TIMESTAMP)
@TimeDifferenceCheck(groups = UpdateResultsAPIObject.EvaluateRemainingConstraints.class, message = AnalyzerErrorConstants.AutotuneObjectErrors.MEASUREMENT_DURATION_ERROR)
@MetricProfileCheck(groups = UpdateResultsAPIObject.EvaluatePerformanceProfileConstraints.class)
@KubernetesElementsCheck(groups = UpdateResultsAPIObject.EvaluateRemainingConstraints.class)
public class UpdateResultsAPIObject extends BaseSO {

    @NotNull(groups = InitialValidation.class)
    @SerializedName(KruizeConstants.JSONKeys.INTERVAL_START_TIME)
    public Timestamp startTimestamp;


    @NotNull(groups = InitialValidation.class)
    @SerializedName(KruizeConstants.JSONKeys.INTERVAL_END_TIME)
    public Timestamp endTimestamp;

    @NotNull(groups = InitialValidation.class)
    @Size(min = 1, max = 1, message = AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_BULK_KUBERNETES)
    @SerializedName(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS)
    private List<KubernetesAPIObject> kubernetesAPIObjects;

    private List<KruizeResponse> errors;

    private KruizeObject kruizeObject;

    public Timestamp getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(Timestamp startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public Timestamp getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(Timestamp endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public List<KubernetesAPIObject> getKubernetesObjects() {
        return kubernetesAPIObjects;
    }

    public void setKubernetesObjects(List<KubernetesAPIObject> kubernetesAPIObjects) {
        this.kubernetesAPIObjects = kubernetesAPIObjects;
    }

    public List<KruizeResponse> getErrors() {
        return errors;
    }

    public void setErrors(List<KruizeResponse> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "UpdateResultsAPIObject{" +
                "startTimestamp=" + startTimestamp +
                ", endTimestamp=" + endTimestamp +
                ", kubernetesAPIObjects=" + kubernetesAPIObjects +
                '}';
    }

    public KruizeObject getKruizeObject() {
        return kruizeObject;
    }

    public void setKruizeObject(KruizeObject kruizeObject) {
        this.kruizeObject = kruizeObject;
    }

    public interface EvaluateRemainingConstraints {
    }
    public interface EvaluatePerformanceProfileConstraints {
    }

    @GroupSequence({UpdateResultsAPIObject.class, InitialValidation.class, EvaluatePerformanceProfileConstraints.class, EvaluateRemainingConstraints.class})
    public interface FullValidationSequence {
    }
}
