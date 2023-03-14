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
package com.autotune.analyzer.utils;

import com.autotune.common.data.ValidationResultData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.common.performanceProfiles.PerformanceProfile;
import com.autotune.common.performanceProfiles.PerformanceProfileInterface.DefaultImpl;
import com.autotune.common.performanceProfiles.PerformanceProfileInterface.PerfProfileImpl;
import com.autotune.utils.AnalyzerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Util class to validate input request related to Experiment Results for metrics collection.
 */
public class ExperimentResultValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentValidation.class);
    private boolean success;
    private String errorMessage;
    private Map<String, KruizeObject> mainKruizeExperimentMAP;
    private Map<String, PerformanceProfile> performanceProfileMap;

    public ExperimentResultValidation(Map<String, KruizeObject> mainKruizeExperimentMAP,Map<String, PerformanceProfile> performanceProfileMap) {
        this.mainKruizeExperimentMAP = mainKruizeExperimentMAP;
        this.performanceProfileMap = performanceProfileMap;
    }

    public void validate(List<ExperimentResultData> experimentResultDataList, Map<String, PerformanceProfile> performanceProfilesMap) {
        try {
            boolean proceed = false;
            String errorMsg = "";
            for (ExperimentResultData resultData : experimentResultDataList) {
                if (null != resultData.getExperiment_name() && null != resultData.getEndtimestamp() && null != resultData.getStarttimestamp()) {
                    if (mainKruizeExperimentMAP.keySet().contains(resultData.getExperiment_name())) {
                        KruizeObject kruizeObject = mainKruizeExperimentMAP.get(resultData.getExperiment_name());
                        boolean isExist = false;
                        if (null != kruizeObject.getResultData())
                            isExist = kruizeObject.getResultData().contains(resultData);
                        if (isExist) {
                            proceed = false;
                            errorMsg = errorMsg.concat(String.format("Experiment name : %s already contains result for timestamp : %s", resultData.getExperiment_name(), resultData.getEndtimestamp()));
                            resultData.setValidationResultData(new ValidationResultData(false, errorMsg));
                            break;
                        }
                        /*
                         Fetch the performance profile from the Map corresponding to the name in the kruize object,
                         and then validate the Performance Profile data
                        */
                        try {
                            LOGGER.info("Kruize Object: {}", kruizeObject);
                            PerformanceProfile performanceProfile = performanceProfilesMap.get(kruizeObject.getPerformanceProfile());
                            // validate the 'resultdata' with the performance profile
                            errorMsg = new PerfProfileImpl().validateResults(performanceProfile,resultData);
                            if (null == errorMsg || errorMsg.isEmpty()) {
                                if (performanceProfile.getName().equalsIgnoreCase(AnalyzerConstants.PerformanceProfileConstants.DEFAULT_PROFILE)) {
                                    errorMsg = new DefaultImpl().recommend(performanceProfile, resultData);
                                } else {
                                    // check the performance profile and instantiate corresponding class for parsing
                                    String validationClassName = AnalyzerConstants.PerformanceProfileConstants
                                            .PERFORMANCE_PROFILE_PKG.concat(new PerfProfileImpl().getName(performanceProfile));
                                    Class<?> validationClass = Class.forName(validationClassName);
                                    Object object = validationClass.getDeclaredConstructor().newInstance();
                                    Class<?>[] parameterTypes = new Class<?>[] { PerformanceProfile.class, ExperimentResultData.class };
                                    Method method = validationClass.getMethod("recommend",parameterTypes);
                                    errorMsg = (String) method.invoke(object, performanceProfile, resultData);
                                }
                                if (null == errorMsg || errorMsg.isEmpty())
                                    proceed = true;
                            } else {
                                proceed = false;
                                resultData.setValidationResultData(new ValidationResultData(false, errorMsg));
                                break;
                            }
                        } catch (NullPointerException | ClassNotFoundException | NoSuchMethodException |
                                 IllegalAccessException | InvocationTargetException e) {
                            LOGGER.error("Caught Exception: {}",e);
                            errorMsg = "Validation failed due to : " + e.getMessage();
                            proceed = false;
                            resultData.setValidationResultData(new ValidationResultData(false, errorMsg));
                            break;
                        }

                    } else {
                        proceed = false;
                        errorMsg = errorMsg.concat(String.format("Experiment name : %s not found", resultData.getExperiment_name()));
                        resultData.setValidationResultData(new ValidationResultData(false, errorMsg));
                        break;
                    }
                    resultData.setValidationResultData(new ValidationResultData(true, "Result Saved successfully! View saved results at /listExperiments ."));
                } else {
                    errorMsg = errorMsg.concat("experiment_name and timestamp are mandatory fields.");
                    proceed = false;
                    resultData.setValidationResultData(new ValidationResultData(false, errorMsg));
                    break;
                }
            }
            if (proceed)
                setSuccess(true);
            else
                markFailed(errorMsg);
        } catch (Exception e) {
            e.printStackTrace();
            setErrorMessage("Validation failed due to : " + e.getMessage());
        }
    }

    private String getValidationClass(String name) {
        String[] words = name.split("-");
        StringBuilder output = new StringBuilder();
        for (String word : words) {
            output.append(word.substring(0,1).toUpperCase() + word.substring(1));
        }
        output.append("Impl");
        LOGGER.debug("ClassName = {}",output);

        return output.toString();
    }

    public void markFailed(String message) {
        setSuccess(false);
        setErrorMessage(message);
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

    @Override
    public String toString() {
        return "ExperimentResultValidation{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
