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

import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ExperimentInterfaceImpl implements ExperimentInterface {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentInterfaceImpl.class);

    @Override
    public boolean addExperiments(Map<String, KruizeObject> mainKruizeExperimentMap, List<KruizeObject> kruizeExperimentList) {
        kruizeExperimentList.forEach(
                (ao) -> {
                    ao.setStatus(AnalyzerConstants.ExpStatus.QUEUED);
                    ao.setExperimentId(Utils.generateID(toString()));
                    mainKruizeExperimentMap.put(
                            ao.getExperimentName(),
                            ao
                    );
                    LOGGER.debug("Added Experiment name : {} into main map.", ao.getExperimentName());
                }
        );
        // TODO   Insert into database
        return true;
    }

    @Override
    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExpStatus status) {
        kruizeObject.setStatus(status);
        // TODO   update into database
        return true;
    }

}
