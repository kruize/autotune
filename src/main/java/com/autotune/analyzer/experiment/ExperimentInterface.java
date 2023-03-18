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

import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.List;
import java.util.Map;

/**
 * This interface methods are implemented when kruize start using DB to store Experiment meta data with results.
 * This Interface also helps when we migrate ORM framework.
 */
public interface ExperimentInterface {
    //Add New experiments into local storage and set status to Queue
    public boolean addExperimentToLocalStorage(
            Map<String, KruizeObject> mainKruizeExperimentMap,
            List<KruizeObject> kruizeExperimentList);

    //Add New experiments from local storage to DB and set status to Inprogress
    public boolean addExperimentToDB(
            KruizeObject kruizeObject
    );

    //Update experiment status
    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status);

    //Add Experiment results into local storage and set status to Queue
    public boolean addResultsToLocalStorage(Map<String, KruizeObject> mainKruizeExperimentMap,
                                            List<ExperimentResultData> experimentResultDataList);

    //Add experiment results from local storage to DB and set status to Inprogress
    public boolean addResultsToDB(
            KruizeObject kruizeObject, ExperimentResultData resultData
    );

    //If Kruize object restarts load all experiment which are in inprogress
    public boolean loadAllExperiments(Map<String, KruizeObject> mainKruizeExperimentMap);
}
