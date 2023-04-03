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

import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface.DefaultImpl;
import com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface.PerfProfileImpl;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.IntervalResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * Util class to validate input request related to Experiment Results for metrics collection.
 */
public class ExperimentResultValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentResultValidation.class);
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
                    if (mainKruizeExperimentMAP.containsKey(resultData.getExperiment_name())) {
                        KruizeObject kruizeObject = mainKruizeExperimentMAP.get(resultData.getExperiment_name());
                        // check if the intervalEnd is greater than intervalStart and interval duration is greater than measurement duration
                        IntervalResults intervalResults = new IntervalResults(resultData.getStarttimestamp(), resultData.getEndtimestamp());
                        Double durationInMins = intervalResults.getDurationInMinutes();
                        String measurementDurationInMins = kruizeObject.getTrial_settings().getMeasurement_durationMinutes();
                        LOGGER.debug("Duration in mins = {}", intervalResults.getDurationInMinutes());
                        if ( durationInMins < 0) {
                            errorMsg = errorMsg.concat(AnalyzerErrorConstants.AutotuneObjectErrors.WRONG_TIMESTAMP);
                            resultData.setValidationOutputData(new ValidationOutputData(false, errorMsg, HttpServletResponse.SC_BAD_REQUEST));
                            break;
                        } else {
                            Double parsedMeasurementDuration = Double.parseDouble(measurementDurationInMins.substring(0,measurementDurationInMins.length()-3));
                            // Calculate the lower and upper bounds for the acceptable range i.e. +-5 seconds
                            double lowerRange = (parsedMeasurementDuration * 60 - 5) / 60.0;
                            double upperRange = (parsedMeasurementDuration * 60 + 5) / 60.0;
                            if (!(durationInMins >= lowerRange && durationInMins <= upperRange)) {
                                errorMsg = errorMsg.concat(AnalyzerErrorConstants.AutotuneObjectErrors.MEASUREMENT_DURATION_ERROR);
                                resultData.setValidationOutputData(new ValidationOutputData(false, errorMsg, HttpServletResponse.SC_BAD_REQUEST));
                                break;
                            }
                        }
                        // check if resultData is present
                        boolean isExist = false;
                        if (null != kruizeObject.getResultData())
                            isExist = kruizeObject.getResultData().contains(resultData);
                        if (isExist) {
                            errorMsg = errorMsg.concat(String.format("Experiment name : %s already contains result for timestamp : %s", resultData.getExperiment_name(), resultData.getEndtimestamp()));
                            resultData.setValidationOutputData(new ValidationOutputData(false, errorMsg, HttpServletResponse.SC_CONFLICT));
                            break;
                        }
                        /*
                         Fetch the performance profile from the Map corresponding to the name in the kruize object,
                         and then validate the Performance Profile data
                        */
                        try {
                            LOGGER.debug("Kruize Object: {}", kruizeObject);
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
                                    Method method = validationClass.getMethod("recommend", parameterTypes);
                                    errorMsg = (String) method.invoke(object, performanceProfile, resultData);
                                }
                                if (errorMsg.isEmpty())
                                    proceed = true;
                            } else {
                                proceed = false;
                                resultData.setValidationOutputData(new ValidationOutputData(false, errorMsg, HttpServletResponse.SC_BAD_REQUEST));
                                break;
                            }
                        } catch (NullPointerException | ClassNotFoundException | NoSuchMethodException |
                                 IllegalAccessException | InvocationTargetException e) {
                            LOGGER.error("Caught Exception: {}",e);
                            errorMsg = "Validation failed: " + e.getMessage();
                            proceed = false;
                            resultData.setValidationOutputData(new ValidationOutputData(false, errorMsg, HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
                            break;
                        }
                    } else {
                        proceed = false;
                        errorMsg = errorMsg.concat(String.format("Experiment name: %s not found", resultData.getExperiment_name()));
                        resultData.setValidationOutputData(new ValidationOutputData(false, errorMsg, HttpServletResponse.SC_BAD_REQUEST));
                        break;
                    }
                    resultData.setValidationOutputData(new ValidationOutputData(true, AnalyzerConstants.ServiceConstants.RESULT_SAVED, HttpServletResponse.SC_CREATED));
                } else {
                    errorMsg = errorMsg.concat("experiment_name and timestamp are mandatory fields.");
                    proceed = false;
                    resultData.setValidationOutputData(new ValidationOutputData(false, errorMsg, HttpServletResponse.SC_BAD_REQUEST));
                    break;
                }
            }
            if (proceed)
                setSuccess(true);
            else
                markFailed(errorMsg);
        } catch (Exception e) {
            e.printStackTrace();
            setErrorMessage("Validation failed: " + e.getMessage());
        }
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
