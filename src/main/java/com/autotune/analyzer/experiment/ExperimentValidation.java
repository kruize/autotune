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

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfilesDeployment;
import com.autotune.analyzer.performanceProfiles.utils.PerformanceProfileUtil;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.serviceObjects.CreateExperimentAPIObject;
import com.autotune.analyzer.serviceObjects.KubernetesAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.service.ExperimentDBService;
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
    private Map<String, PerformanceProfile> performanceProfilesMap = new HashMap<>();
    //Mandatory fields
    private List<String> mandatoryFields = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.API_VERSION,
            AnalyzerConstants.NAME,
            AnalyzerConstants.MODE,
            AnalyzerConstants.TARGET_CLUSTER,
            KruizeConstants.JSONKeys.TRIAL_SETTINGS,
            KruizeConstants.JSONKeys.RECOMMENDATION_SETTINGS
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

    public ExperimentValidation() {
    }

    /**
     * Validates Mode and ClusterType parameter values
     *
     * @param kruizeExptList - contains list of experiments to be validated
     */
    public void validate(List<KruizeObject> kruizeExptList) {
        for (KruizeObject kruizeObject : kruizeExptList) {
            String expName = kruizeObject.getExperimentName();
            try {
                new ExperimentDBService().loadExperimentFromDBByName(mainKruizeExperimentMAP, expName);
            } catch (Exception e) {
                LOGGER.error("Loading saved experiment {} failed: {} ", expName, e.getMessage());
            }

            boolean proceed = false;
            String errorMsg = "";

            if (!kruizeObject.getValidation_data().isSuccess()) {
                markFailed(kruizeObject.getValidation_data().getMessage());
                break;
            }
            if (null == this.mainKruizeExperimentMAP.get(expName))
                proceed = true;
            else {
                errorMsg = errorMsg.concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.DUPLICATE_EXPERIMENT)).concat(expName);
                kruizeObject.setValidation_data(new ValidationOutputData(false, errorMsg, HttpServletResponse.SC_CONFLICT));
            }
            if (!proceed) {
                markFailed(kruizeObject.getValidation_data().getMessage());
                break;
            } else {
                setSuccess(true);
                kruizeObject.setValidation_data(new ValidationOutputData(true, AnalyzerConstants.ServiceConstants.EXPERIMENT_REGISTERED, HttpServletResponse.SC_CREATED));
            }
            // set Performance Profile metrics in the Kruize Object
            PerformanceProfile performanceProfile = performanceProfilesMap.get(kruizeObject.getPerformanceProfile());
            try {
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
            } catch (Exception e) {
                LOGGER.error("Failed to set Performance Profile Metrics to the Kruize Object: {}", e.getMessage());
            }
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

    public void validateCreateExpAPIObject(CreateExperimentAPIObject createExperimentAPIObject) {
        // set default validation status
        createExperimentAPIObject.setValidationData(new ValidationOutputData(true, null, null));
        // check for mandatory fields first
        new ExperimentValidation().validateMandatoryFields(createExperimentAPIObject);
        if (!createExperimentAPIObject.getValidationData().isSuccess()) {
            LOGGER.error(createExperimentAPIObject.getValidationData().getMessage());
        } else {
            // check for invalid values
            ExperimentValidation.checkForNullOrEmpty(createExperimentAPIObject);
            if (!createExperimentAPIObject.getValidationData().isSuccess()) {
                LOGGER.error(createExperimentAPIObject.getValidationData().getMessage());
                createExperimentAPIObject.setValidationData(createExperimentAPIObject.getValidationData());
            }
        }
    }


    /**
     * Check if all mandatory values are present.
     *
     * @param createExperimentAPIObject - KruizeObject whose mandatory fields needs to be validated
     */
    public void validateMandatoryFields(CreateExperimentAPIObject createExperimentAPIObject) {
        List<String> missingMandatoryFields = new ArrayList<>();
        ValidationOutputData validationOutputData = new ValidationOutputData(true, null, null);
        boolean missingDeploySelector = true;
        String errorMsg = "";
        mandatoryFields.forEach(
                mField -> {
                    String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                    try {
                        Method getNameMethod = createExperimentAPIObject.getClass().getMethod(methodName);
                        if (getNameMethod.invoke(createExperimentAPIObject) == null) {
                            missingMandatoryFields.add(mField);
                        }
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        LOGGER.error("Method name for {} does not exist and the error is {}", mField, e.getMessage());
                    }
                }
        );
        if (missingMandatoryFields.isEmpty()) {
            for (String mField : mandatoryDeploymentSelector) {
                String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                try {
                    Method getNameMethod = CreateExperimentAPIObject.class.getMethod(methodName);
                    if (getNameMethod.invoke(createExperimentAPIObject) != null) {
                        missingDeploySelector = false;
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    LOGGER.debug("Method name for {} does not exist and the error is {}", mField, e.getMessage());
                }
            }
            if (missingDeploySelector) {
                errorMsg = errorMsg.concat(String.format("Either parameter should be present: %s",
                        mandatoryDeploymentSelector));
            } else {
                validationOutputData.setSuccess(true);
            }
        } else {
            errorMsg = errorMsg.concat(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_MANDATORY_PARAMETERS)
                    .concat(missingMandatoryFields.toString());
        }
        // check for slo and performance profile and create default profile accordingly
        if (null != createExperimentAPIObject.getPerformanceProfile()) {
            if (null != createExperimentAPIObject.getSloInfo()) {
                errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.SLO_REDUNDANCY_ERROR;
                validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                if (createExperimentAPIObject.getPerformanceProfile().equalsIgnoreCase(AnalyzerConstants.NULL)
                        || createExperimentAPIObject.getPerformanceProfile().isEmpty()) {
                    errorMsg = KruizeConstants.JSONKeys.PERFORMANCE_PROFILE.concat(AnalyzerErrorConstants
                            .AutotuneObjectErrors.NOT_EMPTY_OR_NULL);
                } else {
                    if (null == PerformanceProfilesDeployment.performanceProfilesMap.get(createExperimentAPIObject.getPerformanceProfile())) {
                        errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_PERF_PROFILE.concat(
                                createExperimentAPIObject.getPerformanceProfile());
                        validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
                    }
                }
            }
        } else {
            if (null == createExperimentAPIObject.getSloInfo()) {
                errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_SLO_DATA;
                validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                String perfProfileName = KruizeOperator.setDefaultPerformanceProfile(createExperimentAPIObject
                        .getSloInfo(), null, null);
                createExperimentAPIObject.setPerformanceProfile(perfProfileName);
            }
        }

        if (!errorMsg.isBlank()) {
            validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
            validationOutputData.setSuccess(false);
            validationOutputData.setMessage(errorMsg);
        }
        createExperimentAPIObject.setValidationData(validationOutputData);
    }

    public static void checkForNullOrEmpty(CreateExperimentAPIObject createExperimentAPIObject) {
        List<String> failureReasons = new ArrayList<>();
        try {
            // Validate mandatory keys
            if (createExperimentAPIObject.getExperimentName().equalsIgnoreCase(AnalyzerConstants.NULL)
                    || createExperimentAPIObject.getExperimentName().isEmpty()) {
                failureReasons.add(KruizeConstants.JSONKeys.EXPERIMENT_NAME.concat(AnalyzerErrorConstants
                        .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
            }
            if (createExperimentAPIObject.getApiVersion().equalsIgnoreCase(AnalyzerConstants.NULL)
                    || createExperimentAPIObject.getApiVersion().isEmpty()) {
                failureReasons.add(KruizeConstants.JSONKeys.VERSION.concat(AnalyzerErrorConstants
                        .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
            }
            if (createExperimentAPIObject.getMode().equalsIgnoreCase(AnalyzerConstants.NULL)
                    || createExperimentAPIObject.getMode().isEmpty()) {
                failureReasons.add(KruizeConstants.JSONKeys.MODE.concat(AnalyzerErrorConstants
                        .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
            }
            if (createExperimentAPIObject.getTarget_cluster().equalsIgnoreCase(AnalyzerConstants.NULL)
                    || createExperimentAPIObject.getTarget_cluster().isEmpty()) {
                failureReasons.add(KruizeConstants.JSONKeys.TARGET_CLUSTER.concat(AnalyzerErrorConstants
                        .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
            }
            if (createExperimentAPIObject.getClusterName().equalsIgnoreCase(AnalyzerConstants.NULL)
                    || createExperimentAPIObject.getClusterName().isEmpty()) {
                failureReasons.add(KruizeConstants.JSONKeys.CLUSTER_NAME.concat(AnalyzerErrorConstants
                        .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
            }

            // Validate kubernetes_objects
            if (createExperimentAPIObject.getKubernetes_objects() == null  ||
                    createExperimentAPIObject.getKubernetes_objects().isEmpty()) {
                failureReasons.add(KruizeConstants.JSONKeys.KUBERNETES_OBJECTS.concat(AnalyzerErrorConstants
                        .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
            } else {
                for (KubernetesAPIObject kubernetesAPIObject : createExperimentAPIObject.getKubernetes_objects()) {
                    // check for kubernetes type
                    if (kubernetesAPIObject.getType() == null || kubernetesAPIObject.getType()
                            .equalsIgnoreCase(AnalyzerConstants.NULL) || kubernetesAPIObject.getType().isEmpty()) {
                        failureReasons.add((KruizeConstants.JSONKeys.TYPE).concat(AnalyzerErrorConstants
                                .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
                    } else {
                        // check for valid k8stype
                        AnalyzerConstants.K8S_OBJECT_TYPES type = Arrays.stream(AnalyzerConstants.K8S_OBJECT_TYPES.values())
                                .filter(k8sType -> k8sType.equals(getApproriateK8sObjectType(kubernetesAPIObject.getType())))
                                .findFirst()
                                .orElse(null);
                        if (type == null)
                            failureReasons.add("Invalid deployment type: ".concat(kubernetesAPIObject.getType()));
                    }
                    if (kubernetesAPIObject.getName() == null || kubernetesAPIObject.getName()
                            .equalsIgnoreCase(AnalyzerConstants.NULL) || kubernetesAPIObject.getName().isEmpty()) {
                        failureReasons.add((KruizeConstants.JSONKeys.NAME).concat(AnalyzerErrorConstants
                                .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
                    }
                    if (kubernetesAPIObject.getNamespace() == null || kubernetesAPIObject.getNamespace()
                            .equalsIgnoreCase(AnalyzerConstants.NULL) || kubernetesAPIObject.getNamespace().isEmpty()) {
                        failureReasons.add((KruizeConstants.JSONKeys.NAMESPACE).concat(AnalyzerErrorConstants
                                .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
                    }
                    if (kubernetesAPIObject.getContainerAPIObjects() == null || kubernetesAPIObject.getContainerAPIObjects().isEmpty()) {
                        failureReasons.add((KruizeConstants.JSONKeys.CONTAINERS).concat(AnalyzerErrorConstants
                                .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
                    } else {
                        kubernetesAPIObject.getContainerAPIObjects().forEach(container -> {
                            if (container.getContainer_image_name() == null || container.getContainer_image_name()
                                    .equalsIgnoreCase(AnalyzerConstants.NULL) || container.getContainer_image_name().isEmpty()) {
                                failureReasons.add((KruizeConstants.JSONKeys.CONTAINER_IMAGE_NAME).concat(AnalyzerErrorConstants
                                        .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
                            }
                            if (container.getContainer_name() == null || container.getContainer_name()
                                    .equalsIgnoreCase(AnalyzerConstants.NULL) || container.getContainer_name().isEmpty()) {
                                failureReasons.add((KruizeConstants.JSONKeys.CONTAINER_NAME).concat(AnalyzerErrorConstants
                                        .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
                            }
                        });
                    }
                }
            }

            // Validate trial_settings
            if (createExperimentAPIObject.getTrial_settings() == null || createExperimentAPIObject.getTarget_cluster()
                    .equalsIgnoreCase(AnalyzerConstants.NULL) || createExperimentAPIObject.getTrial_settings().
                    toString().isEmpty()) {
                failureReasons.add((KruizeConstants.JSONKeys.TRIAL_SETTINGS).concat(AnalyzerErrorConstants
                        .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
            } else {
                String measurementDuration = createExperimentAPIObject.getTrial_settings().getMeasurement_durationMinutes();
                if (measurementDuration == null || measurementDuration.equalsIgnoreCase(AnalyzerConstants.NULL) ||
                        measurementDuration.isEmpty()) {
                    failureReasons.add((KruizeConstants.JSONKeys.MEASUREMENT_DURATION).concat(AnalyzerErrorConstants
                            .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
                } else if (!measurementDuration.matches("\\d+min")) {
                    failureReasons.add(AnalyzerErrorConstants.AutotuneObjectErrors.WRONG_DURATION_FORMAT);
                }
            }

            // Validate recommendation_settings
            if (createExperimentAPIObject.getRecommendation_settings() == null || createExperimentAPIObject
                    .getRecommendation_settings().toString().isEmpty()) {
                failureReasons.add((KruizeConstants.JSONKeys.RECOMMENDATION_SETTINGS).concat(AnalyzerErrorConstants
                        .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
            } else if (createExperimentAPIObject.getRecommendation_settings().getThreshold() == null) {
                failureReasons.add((KruizeConstants.JSONKeys.THRESHOLD).concat(AnalyzerErrorConstants
                        .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
            }

            // validate selector values, if present
            if (createExperimentAPIObject.getSelectorInfo() != null && createExperimentAPIObject.getSelectorInfo().toString().isEmpty()) {
                if (createExperimentAPIObject.getSelectorInfo().getMatchLabel() == null || createExperimentAPIObject.
                        getSelectorInfo().getMatchLabel().equalsIgnoreCase(AnalyzerConstants.NULL) ||
                        createExperimentAPIObject.getSelectorInfo().getMatchLabel().isEmpty())
                    failureReasons.add((AnalyzerConstants.AutotuneObjectConstants.MATCH_LABEL).concat(AnalyzerErrorConstants
                            .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
                else if (createExperimentAPIObject.getSelectorInfo().getMatchLabelValue() == null || createExperimentAPIObject.
                        getSelectorInfo().getMatchLabelValue().equalsIgnoreCase(AnalyzerConstants.NULL) ||
                        createExperimentAPIObject.getSelectorInfo().getMatchLabelValue().isEmpty())
                    failureReasons.add((AnalyzerConstants.AutotuneObjectConstants.MATCH_LABEL_VALUE).concat(AnalyzerErrorConstants
                            .AutotuneObjectErrors.NOT_EMPTY_OR_NULL));
            }
        } catch (Exception e) {
            LOGGER.error("Exception Occurred: {}", e.getMessage());
            createExperimentAPIObject.setValidationData(new ValidationOutputData(false, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }

        if (!failureReasons.isEmpty())
            createExperimentAPIObject.setValidationData(new ValidationOutputData(false, String.join(", ", failureReasons), HttpServletResponse.SC_BAD_REQUEST));
        else
            createExperimentAPIObject.setValidationData(new ValidationOutputData(true, "", HttpServletResponse.SC_OK));
    }
    public static ValidationOutputData setDefaultPerformanceProfile(CreateExperimentAPIObject createExperimentAPIObject) {
        PerformanceProfile performanceProfile;
        ValidationOutputData validationOutputData;
        try {
            String name = AnalyzerConstants.PerformanceProfileConstants.DEFAULT_PROFILE;
            double profile_version = AnalyzerConstants.DEFAULT_PROFILE_VERSION;
            String k8s_type = AnalyzerConstants.DEFAULT_K8S_TYPE;
            performanceProfile = new PerformanceProfile(name, profile_version, k8s_type, createExperimentAPIObject.getSloInfo());

            validationOutputData = PerformanceProfileUtil.validateAndAddProfile(PerformanceProfilesDeployment.performanceProfilesMap, performanceProfile);
            if (validationOutputData.isSuccess()) {
                createExperimentAPIObject.setPerformanceProfile(performanceProfile.getName());
                LOGGER.debug("Added Performance Profile : {} with version: {}", performanceProfile.getName(),
                        performanceProfile.getProfile_version());
            } else {
                LOGGER.debug("Performance Profile validation failed: {}", validationOutputData.getMessage());
                validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while creating Performance Profile: {} ", e.getMessage());
            validationOutputData = new ValidationOutputData(false, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
