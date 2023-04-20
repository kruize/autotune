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
package com.autotune.analyzer.experiment;

import com.autotune.analyzer.kruizeObject.ExperimentUseCaseType;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfilesDeployment;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.operator.KruizeOperator;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static com.autotune.utils.Utils.getApproriateK8sObjectType;

/**
 * create Experiment input validation
 */
public class ExperimentValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentValidation.class);
    private boolean success;
    private String errorMessage;
    private Map<String, KruizeObject> mainKruizeExperimentMAP;
    //Mandatory fields
    private List<String> mandatoryFields = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.NAME,
            AnalyzerConstants.MODE,
            AnalyzerConstants.TARGET_CLUSTER
//            AnalyzerConstants.NAMESPACE
    ));
    private List<String> mandatoryFieldsForLocalRemoteMonitoring = new ArrayList<>((
            List.of(AnalyzerConstants.RECOMMENDATION_SETTINGS)
    ));
    private List<String> mandatoryDeploymentSelector = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.DEPLOYMENT_NAME,
            AnalyzerConstants.SELECTOR,
            AnalyzerConstants.KUBERNETES_OBJECTS
    ));

    private List<String> namespaceDeploymentNameList = new ArrayList<>();
    private boolean invalidType = false;

    public ExperimentValidation(Map<String, KruizeObject> mainKruizeExperimentMAP) {
        this.mainKruizeExperimentMAP = mainKruizeExperimentMAP;
        mainKruizeExperimentMAP.forEach((name, ko) -> ko.getKubernetes_objects().forEach(k8sObject -> {
            if (null != k8sObject.getName()) {
                namespaceDeploymentNameList.add(                                //TODO this logic should run once for new exp
                        k8sObject.getNamespace().toLowerCase() + ":" + k8sObject.getName().toLowerCase()
                );
            }
        }));
    }

    /**
     * Validates Mode and ClusterType parameter values
     *
     * @param kruizeExptList - contains list of experiments to be validated
     */
    public void validate(List<KruizeObject> kruizeExptList) {
        for (KruizeObject kruizeObject : kruizeExptList) {
            ValidationOutputData validationOutputData = validateMandatoryFields(kruizeObject);
            if (validationOutputData.isSuccess()) {
                String expName = kruizeObject.getExperimentName();
                String mode = kruizeObject.getMode();
                String target_cluster = kruizeObject.getTarget_cluster();
                boolean proceed = false;
                String errorMsg = "";
                if (null == this.mainKruizeExperimentMAP.get(expName)) {
                    // check for slo and performance profile
                    if (null != kruizeObject.getPerformanceProfile()) {
                        if (null != kruizeObject.getSloInfo()) {
                            errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.SLO_REDUNDANCY_ERROR;
                            validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
                        } else {
                            if (null == PerformanceProfilesDeployment.performanceProfilesMap.get(kruizeObject.getPerformanceProfile())) {
                                errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_PERF_PROFILE + kruizeObject.getPerformanceProfile();
                                validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
                            } else
                                proceed = true;
                        }
                    } else {
                        if (null == kruizeObject.getSloInfo()) {
                            errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_SLO_DATA;
                            validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
                        } else {
                            String perfProfileName = KruizeOperator.setDefaultPerformanceProfile(kruizeObject.getSloInfo(), mode, target_cluster);
                            kruizeObject.setPerformanceProfile(perfProfileName);
                            proceed = true;
                        }
                    }
                } else {
                    errorMsg = errorMsg.concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.DUPLICATE_EXPERIMENT)).concat(expName);
                    validationOutputData.setErrorCode(HttpServletResponse.SC_CONFLICT);
                }
                if (!proceed) {
                    kruizeObject.setValidationData(new ValidationOutputData(false, errorMsg, validationOutputData.getErrorCode()));
                    markFailed(errorMsg);
                    break;
                } else {
                    setSuccess(true);
                    kruizeObject.setValidationData(new ValidationOutputData(true, AnalyzerConstants.ServiceConstants.EXPERIMENT_REGISTERED, HttpServletResponse.SC_CREATED));
                }
            } else {
                kruizeObject.setValidationData(validationOutputData);
                markFailed(validationOutputData.getMessage());
                break;
            }
            // set performance profile metrics in the kruize Object
            PerformanceProfile performanceProfile = PerformanceProfilesDeployment.performanceProfilesMap.get(kruizeObject.getPerformanceProfile());
            HashMap<AnalyzerConstants.MetricName, Metric> metricsMap = new HashMap<>();
            for (Metric metric : performanceProfile.getSloInfo().getFunctionVariables()) {
                if (metric.getKubernetesObject().equals(KruizeConstants.JSONKeys.CONTAINER))
                    metricsMap.put(AnalyzerConstants.MetricName.valueOf(metric.getName()), metric);
            }
            List<K8sObject> k8sObjectList = new ArrayList<>();
            HashMap<String, ContainerData> containerDataMap = new HashMap<>();
            for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
                for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                    containerDataMap.put(containerData.getContainer_name(), new ContainerData(
                            containerData.getContainer_name(), containerData.getContainer_image_name(), new ContainerRecommendations(), metricsMap));
                }
                k8sObject.setContainerDataMap(containerDataMap);
                k8sObjectList.add(k8sObject);
            }
            kruizeObject.setKubernetes_objects(k8sObjectList);
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void markFailed(String message) {
        setSuccess(false);
        setErrorMessage(message);
    }

    /**
     * Check if all mandatory values are present.
     *
     * @param expObj - KruizeObject whose mandatory fields needs to be validated
     * @return - Returns the object containing the validation details
     */
    public ValidationOutputData validateMandatoryFields(KruizeObject expObj) {
        List<String> missingMandatoryFields = new ArrayList<>();
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        boolean missingDeploySelector = true;
        String errorMsg = "";
        mandatoryFields.forEach(
                mField -> {
                    String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                    try {
                        Method getNameMethod = expObj.getClass().getMethod(methodName);
                        if (getNameMethod.invoke(expObj) == null) {
                            missingMandatoryFields.add(mField);
                        }
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        LOGGER.error("Method name for {} does not exist and the error is {}", mField, e.getMessage());
                    }
                }
        );
        if (missingMandatoryFields.size() == 0) {
            try {
                if (expObj.getExperimentUseCaseType().isRemoteMonitoring() || expObj.getExperimentUseCaseType().isLocalMonitoring()) {
                    mandatoryFieldsForLocalRemoteMonitoring.forEach(
                            mField -> {
                                String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                                try {
                                    Method getNameMethod = expObj.getClass().getMethod(methodName);
                                    if (getNameMethod.invoke(expObj) == null) {
                                        missingMandatoryFields.add(mField);
                                    }
                                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                    LOGGER.error("Method name for {} does not exist and the error is {}", mField, e.getMessage());
                                }
                            }
                    );
                }
                String depType = "";
                if (expObj.getExperimentUseCaseType().isRemoteMonitoring()) {
                    // In case of RM, kubernetes_obj is mandatory
                    mandatoryDeploymentSelector = Collections.singletonList(AnalyzerConstants.KUBERNETES_OBJECTS);
                    // check for valid k8stype
                    for (K8sObject k8sObject : expObj.getKubernetes_objects()) {
                        AnalyzerConstants.K8S_OBJECT_TYPES type = Arrays.stream(AnalyzerConstants.K8S_OBJECT_TYPES.values())
                                .filter(k8sType -> k8sType.equals(getApproriateK8sObjectType(k8sObject.getType())))
                                .findFirst()
                                .orElse(null);
                        if (type == null) {
                            depType = k8sObject.getType();
                            invalidType = true;
                            break;
                        }
                    }
                }

                for (String mField : mandatoryDeploymentSelector) {
                    String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                    try {
                        Method getNameMethod = KruizeObject.class.getMethod(methodName);
                        if (getNameMethod.invoke(expObj) != null) {
                            missingDeploySelector = false;
                        }
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        //LOGGER.warn("Method name for {} does not exist and the error is {}", mField, e.getMessage());
                    }
                }
                if (invalidType)
                    errorMsg = errorMsg.concat(String.format("Invalid deployment type: %s", depType));
                if (missingDeploySelector)
                    errorMsg = errorMsg.concat(String.format("Either parameter should be present: %s", mandatoryDeploymentSelector));
                if (invalidType || missingDeploySelector) {
                    validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage(errorMsg);
                } else {
                    validationOutputData.setSuccess(true);
                }
            } catch (Exception e) {
                validationOutputData.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                validationOutputData.setSuccess(false);
                errorMsg = errorMsg.concat(e.getMessage());
                validationOutputData.setMessage(errorMsg);
            }
        } else {
            errorMsg = errorMsg.concat(String.format("Mandatory parameters missing %s ", missingMandatoryFields));
            validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
            validationOutputData.setSuccess(false);
            validationOutputData.setMessage(errorMsg);
        }
        return validationOutputData;
    }

    @Override
    public String toString() {
        return "ExperimentValidation{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
