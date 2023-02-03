/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune.common.k8sObjects;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.utils.ExperimentUseCaseType;
import com.autotune.common.data.ValidationResultData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.Utils;
import com.google.gson.annotations.SerializedName;
import io.fabric8.kubernetes.api.model.ObjectReference;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Container class for the Autotune kubernetes kind objects.
 * <p>
 * Refer to examples dir for a reference AutotuneObject yaml.
 */
public final class KruizeObject {
    private String experimentId;
    @SerializedName("experiment_name")
    private String experimentName;
    private String namespace;
    private String mode;                    //Todo convert into Enum
    private String targetCluster;           //Todo convert into Enum
    @SerializedName("slo")
    private SloInfo sloInfo;
    @SerializedName("selector")
    private SelectorInfo selectorInfo;
    private ObjectReference objectReference;
    private AnalyzerConstants.ExperimentStatus status;
    private String performanceProfile;
    private String deployment_name;
    private TrialSettings trial_settings;
    private RecommendationSettings recommendation_settings;
    private List<ContainerObject> containers;
    private ExperimentUseCaseType experimentUseCaseType;
    private Set<ExperimentResultData> resultData;
    private ValidationResultData validationData;

    public KruizeObject(String experimentName,
                        String namespace,
                        String mode,
                        String targetCluster,
                        SloInfo sloInfo,
                        SelectorInfo selectorInfo,
                        ObjectReference objectReference) throws InvalidValueException {

        HashMap<String, Object> map = new HashMap<>();
        map.put(AnalyzerConstants.AutotuneObjectConstants.NAME, experimentName);
        map.put(AnalyzerConstants.AutotuneObjectConstants.NAMESPACE, namespace);
        map.put(AnalyzerConstants.AutotuneObjectConstants.MODE, mode);
        map.put(AnalyzerConstants.AutotuneObjectConstants.TARGET_CLUSTER, targetCluster);
        map.put(AnalyzerConstants.AutotuneObjectConstants.SLO, sloInfo);
        map.put(AnalyzerConstants.AutotuneObjectConstants.SELECTOR, selectorInfo);

        StringBuilder error = ValidateAutotuneObject.validate(map);
        if (error.toString().isEmpty()) {
            this.experimentName = experimentName;
            this.namespace = namespace;
            this.mode = mode;
            this.targetCluster = targetCluster;
            this.sloInfo = sloInfo;
            this.selectorInfo = selectorInfo;
            this.experimentId = Utils.generateID(toString());
            this.objectReference = objectReference;
        } else {
            throw new InvalidValueException(error.toString());
        }
    }

    public KruizeObject() {

    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public SloInfo getSloInfo() {
        return new SloInfo(sloInfo);
    }

    public void setSloInfo(SloInfo sloInfo) {
        this.sloInfo = sloInfo;
    }

    public SelectorInfo getSelectorInfo() {
        return new SelectorInfo(selectorInfo);
    }

    public void setSelectorInfo(SelectorInfo selectorInfo) {
        this.selectorInfo = selectorInfo;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTargetCluster() {
        return targetCluster;
    }

    public void setTargetCluster(String targetCluster) {
        this.targetCluster = targetCluster;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public ObjectReference getObjectReference() {
        return objectReference;
    }

    public void setObjectReference(ObjectReference objectReference) {
        this.objectReference = objectReference;
    }

    public AnalyzerConstants.ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(AnalyzerConstants.ExperimentStatus status) {
        this.status = status;
    }

    public String getPerformanceProfile() {
        return performanceProfile;
    }

    public void setPerformanceProfile(String performanceProfile) {
        this.performanceProfile = performanceProfile;
    }

    public String getDeployment_name() {
        return deployment_name;
    }

    public void setDeployment_name(String deployment_name) {
        this.deployment_name = deployment_name;
    }

    public TrialSettings getTrial_settings() {
        return trial_settings;
    }

    public void setTrial_settings(TrialSettings trial_settings) {
        this.trial_settings = trial_settings;
    }

    public RecommendationSettings getRecommendation_settings() {
        return recommendation_settings;
    }

    public void setRecommendation_settings(RecommendationSettings recommendation_settings) {
        this.recommendation_settings = recommendation_settings;
    }

    public List<ContainerObject> getContainers() {
        return containers;
    }

    public void setContainers(List<ContainerObject> containers) {
        this.containers = containers;
    }

    public ExperimentUseCaseType getExperimentUseCaseType() {
        return experimentUseCaseType;
    }

    public void setExperimentUseCaseType(ExperimentUseCaseType experimentUseCaseType) {
        this.experimentUseCaseType = experimentUseCaseType;
    }

    public Set<ExperimentResultData> getResultData() {
        return resultData;
    }

    public void setResultData(Set<ExperimentResultData> resultData) {
        this.resultData = resultData;
    }

    public ValidationResultData getValidationData() {
        return validationData;
    }

    public void setValidationData(ValidationResultData validationData) {
        this.validationData = validationData;
    }

    @Override
    public String toString() {
        return "KruizeObject{" +
                "experimentId='" + experimentId + '\'' +
                ", experimentName='" + experimentName + '\'' +
                ", namespace='" + namespace + '\'' +
                ", mode='" + mode + '\'' +
                ", targetCluster='" + targetCluster + '\'' +
                ", sloInfo=" + sloInfo +
                ", selectorInfo=" + selectorInfo +
                ", objectReference=" + objectReference +
                ", status=" + status +
                ", performanceProfile='" + performanceProfile + '\'' +
                ", deployment_name='" + deployment_name + '\'' +
                ", trial_settings=" + trial_settings +
                ", recommendation_settings=" + recommendation_settings +
                ", containers=" + containers +
                ", experimentUseCaseType=" + experimentUseCaseType +
                ", resultData=" + resultData +
                ", validationData=" + validationData +
                '}';
    }
}
