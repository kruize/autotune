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

import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.common.k8sObjects.K8sObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Initiates new experiment data validations and push into queue for worker to
 * execute task.
 */
public class ExperimentInitiator {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentInitiator.class);
    private ValidationOutputData validationOutputData;


    /**
     * Initiate Experiment validation
     *
     * @param mainKruizeExperimentMap
     * @param kruizeExpList
     */
    public void validateAndAddNewExperiments(
            Map<String, KruizeObject> mainKruizeExperimentMap,
            List<KruizeObject> kruizeExpList
    ) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        try {
//            for (KruizeObject kruizeObject : kruizeExpList){
//                for ( K8sObject k8sObject : kruizeObject.getKubernetesObjects()) {
//                for ( ContainerData containerData : k8sObject.getContainerDataList()) {
//                    if ( null == containerData.getContainerRecommendations())
//                        System.out.println("NUll recommendation");
//                    else
//                        System.out.println("recomm = "+containerData.getContainerRecommendations());
//                }}
//
//            }
            ExperimentValidation validationObject = new ExperimentValidation(mainKruizeExperimentMap);
            validationObject.validate(kruizeExpList);
            if (validationObject.isSuccess()) {
                ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
                experimentInterface.addExperimentToLocalStorage(mainKruizeExperimentMap, kruizeExpList);
                validationOutputData.setSuccess(true);
            } else {
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage("Validation failed: " + validationObject.getErrorMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Validate and push experiment falied: " + e.getMessage());
            validationOutputData.setSuccess(false);
            validationOutputData.setMessage("Validation failed: " + e.getMessage());
        }
    }

    /**
     * @param mainKruizeExperimentMap
     * @param experimentResultDataList
     * @param performanceProfilesMap
     */
    public void validateAndUpdateResults(
            Map<String, KruizeObject> mainKruizeExperimentMap,
            List<ExperimentResultData> experimentResultDataList,
            Map<String, PerformanceProfile> performanceProfilesMap) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        try {
            ExperimentResultValidation experimentResultValidation = new ExperimentResultValidation(mainKruizeExperimentMap, performanceProfilesMap);
            experimentResultValidation.validate(experimentResultDataList, performanceProfilesMap);
            if (experimentResultValidation.isSuccess()) {
                ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
                experimentInterface.addResultsToLocalStorage(mainKruizeExperimentMap, experimentResultDataList);
                validationOutputData.setSuccess(true);
            } else {
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage("Validation failed: " + experimentResultValidation.getErrorMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Validate and push experiment falied: " + e.getMessage());
            validationOutputData.setSuccess(false);
            validationOutputData.setMessage("Exception occurred while validating the result data: " + e.getMessage());
            validationOutputData.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
