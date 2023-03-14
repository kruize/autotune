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
import com.autotune.common.performanceProfiles.SloInfo;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.AutotuneSupportedTypes;
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
    @SerializedName("version")
    private String apiVersion;
    private String experimentId;
    @SerializedName("experiment_name")
    private String experimentName;
    @SerializedName("cluster_name")
    private String clusterName;
    private String namespace;               // TODO: Currently adding it at this level with an assumption that there is only one entry in k8s object needs to be changed
    private String mode;                    //Todo convert into Enum
    @SerializedName("target_cluster")
    private String targetCluster;           //Todo convert into Enum
    @SerializedName("slo")
    private SloInfo sloInfo;
    private String hpoAlgoImpl;
    @SerializedName("selector")
    private SelectorInfo selectorInfo;
    private ObjectReference objectReference;
    private AnalyzerConstants.ExperimentStatus status;
    @SerializedName("performance_profile")
    private String performanceProfile;
    private String deployment_name;
    private TrialSettings trial_settings;
    private RecommendationSettings recommendation_settings;
    private ExperimentUseCaseType experimentUseCaseType;
    private Set<ExperimentResultData> resultData;
    private ValidationResultData validationData;
    private HashMap<String, DeploymentObject> deployments;
    private List<K8sObject> kubernetes_objects;


    public KruizeObject(String experimentName,
                        String clusterName,
                        String namespace,
                        String mode,
                        String targetCluster,
                        String hpoAlgoImpl,
                        SelectorInfo selectorInfo,
                        String performanceProfile,
                        ObjectReference objectReference
                        ) throws InvalidValueException {

        HashMap<String, Object> map = new HashMap<>();
        map.put(AnalyzerConstants.AutotuneObjectConstants.NAME, experimentName);
        map.put(AnalyzerConstants.AutotuneObjectConstants.NAMESPACE, namespace);
        map.put(AnalyzerConstants.AutotuneObjectConstants.MODE, mode);
        map.put(AnalyzerConstants.AutotuneObjectConstants.TARGET_CLUSTER, targetCluster);
        map.put(AnalyzerConstants.AutotuneObjectConstants.SELECTOR, selectorInfo);
        map.put(AnalyzerConstants.AutotuneObjectConstants.CLUSTER_NAME, clusterName);

        StringBuilder error = ValidateAutotuneObject.validate(map);
        if (error.toString().isEmpty()) {
            this.experimentName = experimentName;
            this.mode = mode;
            this.targetCluster = targetCluster;
            this.selectorInfo = selectorInfo;
            this.experimentId = Utils.generateID(toString());
            this.objectReference = objectReference;
            this.clusterName = clusterName;
        } else {
            throw new InvalidValueException(error.toString());
        }
        this.performanceProfile = performanceProfile;
        if (AutotuneSupportedTypes.HPO_ALGOS_SUPPORTED.contains(hpoAlgoImpl))
            this.hpoAlgoImpl = hpoAlgoImpl;
        else
            throw new InvalidValueException("Hyperparameter Optimization Algorithm " + hpoAlgoImpl + " not supported");

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
        return sloInfo;
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

    public HashMap<String, DeploymentObject> getDeployments() {
        return deployments;
    }

    public void setDeployments(HashMap<String, DeploymentObject> deployments) {
        this.deployments = deployments;
    }

    public String getHpoAlgoImpl() {
        return hpoAlgoImpl;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public List<K8sObject> getKubernetesObjects() {
        return kubernetes_objects;
    }

    public void setKubernetesObjects(List<K8sObject> kubernetes_objects) {
        this.kubernetes_objects = kubernetes_objects;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String toString() {
        // Creating a temparory cluster name as we allow null for cluster name now
        // Please change it to use `clusterName` variable itself if there is a null check already in place for that
        String tmpClusterName = "";
        if (clusterName != null)
            tmpClusterName = clusterName;
        return "KruizeObject{" +
                "experimentId='" + experimentId + '\'' +
                ", experimentName='" + experimentName + '\'' +
                ", clusterName=" + tmpClusterName +
                ", mode='" + mode + '\'' +
                ", targetCluster='" + targetCluster + '\'' +
                ", hpoAlgoImpl=" + hpoAlgoImpl +
                ", selectorInfo=" + selectorInfo +
                ", objectReference=" + objectReference +
                ", status=" + status +
                ", performanceProfile='" + performanceProfile + '\'' +
                ", deployment_name='" + deployment_name + '\'' +
                ", trial_settings=" + trial_settings +
                ", recommendation_settings=" + recommendation_settings +
                ", experimentUseCaseType=" + experimentUseCaseType +
                ", resultData=" + resultData +
                ", validationData=" + validationData +
                '}';
    }
}
