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

import com.autotune.operator.KruizeOperator;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.analyzer.performanceProfiles.PerformanceProfilesDeployment;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

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
//            AnalyzerConstants.DEPLOYMENT_NAME,
//            AnalyzerConstants.SELECTOR
    ));

    private List<String> namespaceDeploymentNameList = new ArrayList<>();

    public ExperimentValidation(Map<String, KruizeObject> mainKruizeExperimentMAP) {
        this.mainKruizeExperimentMAP = mainKruizeExperimentMAP;
        mainKruizeExperimentMAP.forEach((name, ao) -> {
            if (null != ao.getDeployment_name()) {
                namespaceDeploymentNameList.add(                                //TODO this logic should run once for new exp
                        ao.getNamespace().toLowerCase() + ":" + ao.getDeployment_name().toLowerCase()
                );
            }
        });
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
                    if (null != kruizeObject.getDeployment_name()) {
                        String nsDepName = kruizeObject.getNamespace().toLowerCase() + ":" + kruizeObject.getDeployment_name().toLowerCase();
                        if (!namespaceDeploymentNameList.contains(nsDepName)) {
                            if (null != kruizeObject.getPerformanceProfile()) {
                                if (null != kruizeObject.getSloInfo())
                                    errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.SLO_REDUNDANCY_ERROR;
                                else {
                                    if (null == PerformanceProfilesDeployment.performanceProfilesMap.get(kruizeObject.getPerformanceProfile()))
                                        errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_PERF_PROFILE + kruizeObject.getPerformanceProfile();
                                    else
                                        proceed = true;
                                }
                            } else {
                                if (null == kruizeObject.getSloInfo())
                                    errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_SLO_DATA;
                                else {
                                    String perfProfileName = KruizeOperator.setDefaultPerformanceProfile(kruizeObject.getSloInfo(), mode, target_cluster);
                                    kruizeObject.setPerformanceProfile(perfProfileName);
                                    proceed = true;
                                }
                            }
                        } else {
                            if (!kruizeObject.getExperimentUseCaseType().isRemoteMonitoring())
                                errorMsg = errorMsg.concat(String.format("Experiment name : %s with Deployment name : %s is duplicate", expName, nsDepName));
                            else
                                proceed = true;
                        }
                    } else {
                        proceed = true;
                    }
                } else {
                    errorMsg = errorMsg.concat(String.format("Experiment name : %s is duplicate", expName));
                }
                if (!proceed) {
                    kruizeObject.setValidationData(new ValidationOutputData(false, errorMsg));
                    markFailed(errorMsg);
                    break;
                } else {
                    setSuccess(true);
                    kruizeObject.setValidationData(new ValidationOutputData(true, "Registered successfully with Kruize! View registered experiments at /listExperiments."));
                }
            } else {
                kruizeObject.setValidationData(validationOutputData);
                markFailed(validationOutputData.getMessage());
                break;
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

    /**
     * Check if all mandatory values are present.
     *
     * @param expObj - KruizeObject whose mandatory fields needs to be validated
     * @return - Returns the object containing the validation details
     */
    public ValidationOutputData validateMandatoryFields(KruizeObject expObj) {
        List<String> missingMandatoryFields = new ArrayList<>();
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null);
        boolean missingDeploySelector = true;
        String errorMsg = "";
        String expName = expObj.getExperimentName();
        errorMsg = String.format("Experiment Name : %s ", expName);
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
                expObj.setExperimentUseCaseType(new ExperimentUseCaseType(expObj));
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
                // Adding temporary validation skip
                missingDeploySelector = false;
                if (missingDeploySelector) {
                    errorMsg = errorMsg.concat(String.format("Either parameter should be present %s ", mandatoryDeploymentSelector));

                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage(errorMsg);
                } else {
                    validationOutputData.setSuccess(true);
                }
            } catch (Exception e) {
                validationOutputData.setSuccess(false);
                errorMsg = errorMsg.concat(e.getMessage());
                validationOutputData.setMessage(errorMsg);
            }
        } else {
            errorMsg = errorMsg.concat(String.format("Mandatory parameters missing %s ", missingMandatoryFields));
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
