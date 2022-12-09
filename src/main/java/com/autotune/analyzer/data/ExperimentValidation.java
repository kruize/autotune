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
package com.autotune.analyzer.data;

import com.autotune.common.data.GeneralDataHolder;
import com.autotune.common.k8sObjects.AutotuneObject;
import com.autotune.utils.AnalyzerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * create Experiment input validation
 */
public class ExperimentValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentValidation.class);
    private boolean success;
    private String errorMessage;
    private String experiment_name;
    private AnalyzerConstants.MODEType mode;
    private AnalyzerConstants.TargetType target_cluster;
    private Map<String, AutotuneObject> mainKruizeExperimentMAP;
    //Mandatory fields
    private List<String> mandatoryFields = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.NAME,
            AnalyzerConstants.MODE,
            AnalyzerConstants.TARGET_CLUSTER,
            AnalyzerConstants.NAMESPACE,
            AnalyzerConstants.RECOMMENDATION_SETTINGS
    ));
    private List<String> mandatorySLOPerf = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.SLO,
            AnalyzerConstants.PERFPROFILE
    ));
    private List<String> mandatoryDeploymentSelector = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.DEPLOYMENT_NAME,
            AnalyzerConstants.SELECTOR
    ));
    private List<AutotuneObject> validKruizeExpList = new ArrayList<>();
    private List<String> namespaceDeploymentNameList = new ArrayList<>();

    public ExperimentValidation(Map<String, AutotuneObject> mainKruizeExperimentMAP) {
        this.mainKruizeExperimentMAP = mainKruizeExperimentMAP;
        mainKruizeExperimentMAP.forEach((name, ao) -> {
            if (null != ao.getDeployment_name()) {
                namespaceDeploymentNameList.add(
                        ao.getNamespace().toLowerCase() + ":" + ao.getDeployment_name().toLowerCase()
                );
            }
        });
    }

    public List<AutotuneObject> getValidKruizeExpList() {
        return validKruizeExpList;
    }

    public void setValidKruizeExpList(List<AutotuneObject> validKruizeExpList) {
        this.validKruizeExpList = validKruizeExpList;
    }

    /**
     * Validates Mode and ClusterType parameter values
     *
     * @param kruizeExptList
     */
    public void validate(List<AutotuneObject> kruizeExptList) {
        for (AutotuneObject ao : kruizeExptList) {
            GeneralDataHolder generalDataHolder = validateMandatoryFields(ao);
            if (generalDataHolder.isSuccess()) {
                String expName = ao.getExperimentName();
                String mode = ao.getMode();
                String target_cluster = ao.getTargetCluster();
                LOGGER.debug("expName:{} , mode: {} , target_cluster: {}", expName, mode, target_cluster);
                boolean proceed = false;
                if (null == this.mainKruizeExperimentMAP.get(expName)) {
                    if (null != ao.getDeployment_name()) {
                        String nsDepName = ao.getNamespace().toLowerCase() + ":" + ao.getDeployment_name().toLowerCase();
                        if (!namespaceDeploymentNameList.contains(nsDepName))
                            proceed = true;
                    } else {
                        proceed = true;
                    }
                }

                if (proceed) {
                    if (target_cluster.equals(AnalyzerConstants.LOCAL)) {
                        if (mode.equals(AnalyzerConstants.EXPERIMENT) || mode.equals(AnalyzerConstants.MONITOR)) {
                            LOGGER.debug(this.mainKruizeExperimentMAP.toString());
                            validKruizeExpList.add(ao);
                            setSuccess(true);
                        } else {
                            markFailed(String.format(
                                    "Experiment Name : %s contains Invalid or not supported `mode` type : %s", expName, mode
                            ));
                            break;
                        }
                    } else if (target_cluster.equals(AnalyzerConstants.REMOTE)) {
                        if (mode.equals(AnalyzerConstants.MONITOR)) {
                            validKruizeExpList.add(ao);
                            setSuccess(true);
                        } else {
                            markFailed(String.format(
                                    "Experiment Name : %s contains Invalid or not supported `mode` type : %s", expName, mode
                            ));
                            break;
                        }
                    } else {
                        markFailed(String.format(
                                "Experiment Name : %s contains Invalid or not supported `mode` type : %s", expName, mode
                        ));
                        break;
                    }
                } else {
                    markFailed(String.format(
                            "Either Duplicate Experiment Name : %s  OR duplicate namespace %s deployment %s with status %s",
                            expName,
                            ao.getNamespace(),
                            ao.getDeployment_name(),
                            ao.getStatus()
                    ));
                    break;
                }
            } else {
                markFailed(generalDataHolder.getErrorMessage());
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

    public String getExperiment_name() {
        return experiment_name;
    }

    public void setExperiment_name(String experiment_name) {
        this.experiment_name = experiment_name;
    }

    public AnalyzerConstants.MODEType getMode() {
        return mode;
    }

    public void setMode(AnalyzerConstants.MODEType mode) {
        this.mode = mode;
    }

    public AnalyzerConstants.TargetType getTarget_cluster() {
        return target_cluster;
    }

    public void setTarget_cluster(AnalyzerConstants.TargetType target_cluster) {
        this.target_cluster = target_cluster;
    }

    /**
     * Check if all mandatory values are present.
     *
     * @param expObj
     * @return
     */
    public GeneralDataHolder validateMandatoryFields(AutotuneObject expObj) {
        List<String> missingMandatoryFields = new ArrayList<>();
        GeneralDataHolder generalDataHolder = new GeneralDataHolder();
        boolean missingSLOPerf = true;
        boolean missingDeploySelector = true;
        mandatoryFields.forEach(
                mField -> {
                    String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                    try {
                        Method getNameMethod = expObj.getClass().getMethod(methodName);
                        LOGGER.debug(getNameMethod.getName());
                        if (getNameMethod.invoke(expObj) == null) {
                            missingMandatoryFields.add(mField);
                        }
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        LOGGER.error("Methode name for {} not exsist and error is {}", mField, e.getMessage());
                    }
                }
        );
        for (String mField : mandatorySLOPerf) {
            String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
            try {
                Method getNameMethod = AutotuneObject.class.getMethod(methodName);
                LOGGER.debug(getNameMethod.getName());
                if (getNameMethod.invoke(expObj) != null) {
                    missingSLOPerf = false;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Methode name for {} not exsist and error is {}", mField, e.getMessage());
            }
        }
        for (String mField : mandatoryDeploymentSelector) {
            String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
            try {
                Method getNameMethod = AutotuneObject.class.getMethod(methodName);
                LOGGER.debug(getNameMethod.getName());
                if (getNameMethod.invoke(expObj) != null) {
                    missingDeploySelector = false;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Methode name for {} not exist and error is {}", mField, e.getMessage());
            }
        }
        LOGGER.debug("Following mandatory fields missing {}", missingMandatoryFields.toString());
        LOGGER.debug("missingSLOPerf:{} , missingDeploySelector,{}", missingSLOPerf, missingDeploySelector);
        if (missingMandatoryFields.size() > 0 || missingSLOPerf || missingDeploySelector) {
            String expName = null;
            generalDataHolder.setSuccess(false);
            expName = expObj.getExperimentName();
            String errorMsg = String.format("Experiment Name : %s \n", expName);
            if (missingMandatoryFields.size() > 0) {
                generalDataHolder.setSuccess(false);
                errorMsg = errorMsg.concat(String.format("Missing following Mandatory parameters %s \n ", missingMandatoryFields.toString()));
            }
            if (missingSLOPerf) {
                generalDataHolder.setSuccess(false);
                errorMsg = errorMsg.concat(
                        String.format("Either one of the parameter should present %s \n", mandatorySLOPerf)
                );
            }
            if (missingDeploySelector) {
                generalDataHolder.setSuccess(false);
                errorMsg = errorMsg.concat(
                        String.format("Either one of the parameter should present %s \n", mandatoryDeploymentSelector)
                );
            }
            LOGGER.debug("Validation error message :{}", errorMsg);
            generalDataHolder.setErrorMessage(errorMsg);
        } else {
            generalDataHolder.setSuccess(true);
        }
        LOGGER.debug("{}", generalDataHolder);
        return generalDataHolder;
    }
}
