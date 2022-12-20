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
import com.autotune.common.data.GeneralDataHolder;
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
    private GeneralDataHolder generalDataHolder;


    /**
     * Initiates experiment validation
     *
     * @param mainKruizeExperimentMap
     * @param kruizeExpList
     * @return
     */
    public GeneralDataHolder validateAndAdd(
            Map<String, KruizeObject> mainKruizeExperimentMap,
            List<KruizeObject> kruizeExpList
    ) {
        this.generalDataHolder = new GeneralDataHolder();
        try {
            ExperimentValidation validationObject = new ExperimentValidation(mainKruizeExperimentMap);
            validationObject.validate(kruizeExpList);
            LOGGER.debug(validationObject.toString());
            if (validationObject.isSuccess()) {
                ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
                experimentInterface.addExperiments(mainKruizeExperimentMap, kruizeExpList);
                this.generalDataHolder.setSuccess(true);
            } else {
                this.generalDataHolder.setSuccess(false);
                this.generalDataHolder.setErrorMessage("Validation failed due to " + validationObject.getErrorMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Validate and push experiment falied due to : " + e.getMessage());
            this.generalDataHolder.setSuccess(false);
            this.generalDataHolder.setErrorMessage("Validation failed due to " + e.getMessage());
        }
        return this.generalDataHolder;
    }
}
