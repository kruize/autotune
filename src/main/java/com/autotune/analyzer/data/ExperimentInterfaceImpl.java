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

import com.autotune.common.k8sObjects.ExperimentResultData;
import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExperimentInterfaceImpl implements ExperimentInterface {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentInterfaceImpl.class);

    @Override
    public boolean addExperimentToLocalStorage(Map<String, KruizeObject> mainKruizeExperimentMap, List<KruizeObject> kruizeExperimentList) {
        kruizeExperimentList.forEach(
                (kruizeObject) -> {
                    kruizeObject.setStatus(AnalyzerConstants.ExperimentStatus.QUEUED);
                    kruizeObject.setExperimentId(Utils.generateID(toString()));
                    mainKruizeExperimentMap.put(
                            kruizeObject.getExperimentName(),
                            kruizeObject
                    );
                    LOGGER.debug("Added Experiment name : {} into main map.", kruizeObject.getExperimentName());
                }
        );
        return true;
    }

    @Override
    public boolean addExperimentToDB(KruizeObject kruizeObject) {
        //TODO insert in to db
        updateExperimentStatus(kruizeObject, AnalyzerConstants.ExpStatus.IN_PROGRESS);
        return true;
    }

    @Override
    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status) {
        kruizeObject.setStatus(status);
        // TODO   update into database
        return true;
    }

    @Override
    public boolean addResultsToLocalStorage(Map<String, KruizeObject> mainKruizeExperimentMap, List<ExperimentResultData> experimentResultDataList) {
        experimentResultDataList.forEach(
                (resultData) -> {
                    resultData.setStatus(AnalyzerConstants.ExpStatus.QUEUED);
                    KruizeObject ko = mainKruizeExperimentMap.get(resultData.getExperiment_name());
                    Set<ExperimentResultData> results = null;
                    if (ko.getExperimentResultDataSet() == null)
                        results = new HashSet<>();
                    else
                        results = ko.getExperimentResultDataSet();
                    results.add(resultData);
                    ko.setExperimentResultDataSet(results);
                }
        );
        // TODO   Insert into database
        return true;
    }

    @Override
    public boolean addResultsToDB(KruizeObject kruizeObject, ExperimentResultData resultData) {
        // TODO   Insert into database
        resultData.setStatus(AnalyzerConstants.ExpStatus.IN_PROGRESS);
        return false;
    }


    @Override
    public boolean loadAllExperiments(Map<String, KruizeObject> mainKruizeExperimentMap) {
        //TOdo load all experiments from DB
        return false;
    }

}
