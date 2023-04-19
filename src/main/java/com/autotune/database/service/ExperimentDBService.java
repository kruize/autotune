/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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
package com.autotune.database.service;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.CreateExperimentAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.database.dao.ExperimentDAO;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.helper.DBHelpers;
import com.autotune.database.table.KruizeExperimentEntry;
import com.autotune.database.table.KruizeRecommendationEntry;
import com.autotune.database.table.KruizeResultsEntry;
import com.autotune.operator.KruizeOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ExperimentDBService {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentDBService.class);
    private ExperimentDAO experimentDAO;

    public ExperimentDBService() {
        this.experimentDAO = new ExperimentDAOImpl();
    }

    public ValidationOutputData addExperimentToDB(KruizeObject kruizeObject) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        try {
            updateExperimentStatus(kruizeObject, AnalyzerConstants.ExperimentStatus.IN_PROGRESS);
            KruizeExperimentEntry kruizeExperimentEntry = DBHelpers.Converters.KruizeObjectConverters.convertKruizeObjectToExperimentDBObj(kruizeObject);
            validationOutputData = this.experimentDAO.addExperimentToDB(kruizeExperimentEntry);
            if (!validationOutputData.isSuccess())
                updateExperimentStatus(kruizeObject, AnalyzerConstants.ExperimentStatus.FAILED);
        } catch (Exception e) {
            LOGGER.error("Not able to save experiment due to {}", e.getMessage());
        }
        return validationOutputData;
    }

    public ValidationOutputData addResultsToDB(ExperimentResultData resultData) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        KruizeResultsEntry kruizeResultsEntry = DBHelpers.Converters.KruizeObjectConverters.convertExperimentResultToExperimentResultsTable(resultData);
        validationOutputData = experimentDAO.addResultsToDB(kruizeResultsEntry);
        if (validationOutputData.isSuccess())
            resultData.setStatus(AnalyzerConstants.ExperimentStatus.IN_PROGRESS);
        else {
            resultData.setStatus(AnalyzerConstants.ExperimentStatus.FAILED);
        }
        return validationOutputData;
    }

    public boolean getRecommendationToSave(Map<String, KruizeObject> experimentsMap, List<String> experimentNameList) {
        if (null == experimentNameList)
            return false;
        if (experimentNameList.size() == 0)
            return false;
        for (String experimentName : experimentNameList) {
            // TODO: Log the list of invalid experiments and return the error instead of bailing out completely
            if (!experimentsMap.containsKey(experimentName))
                return false;
        }
        // Generate recommendations for valid experiments
        for (String experimentName : experimentNameList) {
            KruizeObject kruizeObject = experimentsMap.get(experimentName);
            KruizeRecommendationEntry kr = DBHelpers.Converters.KruizeObjectConverters.
                    convertKruizeObjectTORecommendation(kruizeObject);
            new ExperimentDAOImpl().addRecommendationToDB(kr);
        }
        return true;
    }

    public void loadAllExperiments() throws Exception {
        List<KruizeExperimentEntry> entries = experimentDAO.loadAllExperiments();
        List<CreateExperimentAPIObject> createExperimentAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertExperimentEntryToCreateExperimentAPIObject(entries);
        for (CreateExperimentAPIObject createExperimentAPIObject : createExperimentAPIObjects) {
            KruizeObject kruizeObject = Converters.KruizeObjectConverters.convertCreateExperimentAPIObjToKruizeObject(createExperimentAPIObject);
            if (null != kruizeObject)
                KruizeOperator.autotuneObjectMap.put(kruizeObject.getExperimentName(), kruizeObject);
        }
        LOGGER.debug(KruizeOperator.autotuneObjectMap.toString());
        // Get KruizeObject using CreateExperimentAPIObject -> vinay -> done
        //TODO get KruizeResultsEntry to KruizeObject.kubernetes_objects.containerDataMap.results -> Saad
        //TODO get KruizeRecommendationEntry to KruizeObject.kubernetes_objects.containerDataMap.containerRecommendations -> Bharath
        //Populate to KruizeOperator.autotuneObjectMap -> vinay -> done
    }

    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status) {
        kruizeObject.setStatus(status);
        // TODO   update into database
        return true;
    }
}
