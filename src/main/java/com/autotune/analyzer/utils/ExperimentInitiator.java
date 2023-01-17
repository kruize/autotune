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

import com.autotune.analyzer.data.ExperimentInterface;
import com.autotune.analyzer.data.ExperimentInterfaceImpl;
import com.autotune.common.data.ActivityResultData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.k8sObjects.KruizeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Initiates new experiment data validations and push into queue for worker to
 * execute task.
 */
public class ExperimentInitiator {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentInitiator.class);
    private ActivityResultData activityResultData;


    /**
     * Initiate Experiment validation
     *
     * @param mainKruizeExperimentMap
     * @param kruizeExpList
     * @return
     */
    public ActivityResultData validateAndAddNewExperiments(
            Map<String, KruizeObject> mainKruizeExperimentMap,
            List<KruizeObject> kruizeExpList
    ) {
        ActivityResultData activityResultData = new ActivityResultData();
        try {
            ExperimentValidation validationObject = new ExperimentValidation(mainKruizeExperimentMap);
            validationObject.validate(kruizeExpList);
            LOGGER.debug(validationObject.toString());
            if (validationObject.isSuccess()) {
                ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
                experimentInterface.addExperimentToLocalStorage(mainKruizeExperimentMap, kruizeExpList);
                activityResultData.setSuccess(true);
            } else {
                activityResultData.setSuccess(false);
                activityResultData.setErrorMessage("Validation failed due to " + validationObject.getErrorMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Validate and push experiment falied due to : " + e.getMessage());
            activityResultData.setSuccess(false);
            activityResultData.setErrorMessage("Validation failed due to " + e.getMessage());
        }
        return activityResultData;
    }

    /**
     * @param mainKruizeExperimentMap
     * @param experimentResultDataList
     * @return
     */
    public ActivityResultData validateAndUpdateResults(
            Map<String, KruizeObject> mainKruizeExperimentMap,
            List<ExperimentResultData> experimentResultDataList
    ) {
        ActivityResultData activityResultData = new ActivityResultData();
        try {
            ExperimentResultValidation experimentResultValidation = new ExperimentResultValidation(mainKruizeExperimentMap);
            experimentResultValidation.validate(experimentResultDataList);
            if (experimentResultValidation.isSuccess()) {
                ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
                experimentInterface.addResultsToLocalStorage(mainKruizeExperimentMap, experimentResultDataList);
                activityResultData.setSuccess(true);
            } else {
                activityResultData.setSuccess(false);
                activityResultData.setErrorMessage("Validation failed due to " + experimentResultValidation.getErrorMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Validate and push experiment falied due to : " + e.getMessage());
            activityResultData.setSuccess(false);
            activityResultData.setErrorMessage("Validation failed due to " + e.getMessage());
        }
        return activityResultData;
    }
}
