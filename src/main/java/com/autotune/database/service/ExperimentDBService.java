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

import com.autotune.analyzer.experiment.ExperimentInterface;
import com.autotune.analyzer.experiment.ExperimentInterfaceImpl;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.CreateExperimentAPIObject;
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExperimentDBService {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentDBService.class);
    private ExperimentDAO experimentDAO;

    public ExperimentDBService() {
        this.experimentDAO = new ExperimentDAOImpl();
    }

    public ValidationOutputData addExperimentToDB(CreateExperimentAPIObject createExperimentAPIObject) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        try {
            KruizeExperimentEntry kruizeExperimentEntry = DBHelpers.Converters.KruizeObjectConverters.convertCreateAPIObjToExperimentDBObj(createExperimentAPIObject);
            validationOutputData = this.experimentDAO.addExperimentToDB(kruizeExperimentEntry);
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

    public boolean getRecommendationToSave(Map<String, KruizeObject> experimentsMap, List<ExperimentResultData> experimentResultDataList) {
        if (null == experimentResultDataList)
            return false;
        if (experimentResultDataList.size() == 0)
            return false;
        for (ExperimentResultData experimentResultData: experimentResultDataList) {
            // TODO: Log the list of invalid experiments and return the error instead of bailing out completely
            if (!experimentsMap.containsKey(experimentResultData.getExperiment_name())) {
                LOGGER.error("Trying to locate Recommendation for non existent experiment: " +
                        experimentResultData.getExperiment_name());
                continue;
            }

            KruizeObject kruizeObject = experimentsMap.get(experimentResultData.getExperiment_name());
            KruizeRecommendationEntry kr = DBHelpers.Converters.KruizeObjectConverters.
                    convertKruizeObjectTORecommendation(kruizeObject, experimentResultData);
            if (null != kr) {
                new ExperimentDAOImpl().addRecommendationToDB(kr);
            }
        }
        return true;
    }

    /*
     * This is a Java method that loads all experiments from the database using an experimentDAO object.
     * The method then converts the retrieved data into KruizeObject format, adds them to a list,
     * and sends it to the ExperimentInterface implementation to store the objects.
     */
    public void loadAllExperimentsData() throws Exception {
        List<KruizeExperimentEntry> entries = experimentDAO.loadAllExperiments();
        List<CreateExperimentAPIObject> createExperimentAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertExperimentEntryToCreateExperimentAPIObject(entries);
        List<KruizeObject> kruizeExpList = new ArrayList<>();
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();

        int failureThreshHold = createExperimentAPIObjects.size();
        int failureCount = 0;
        for (CreateExperimentAPIObject createExperimentAPIObject : createExperimentAPIObjects) {
            KruizeObject kruizeObject = Converters.KruizeObjectConverters.convertCreateExperimentAPIObjToKruizeObject(createExperimentAPIObject);
            if (null != kruizeObject) {
                kruizeExpList.add(kruizeObject);
            }
            else {
                failureCount++;
            }
        }
        if (failureThreshHold > 0 && failureCount == failureThreshHold) {
            throw new Exception("None of the experiments are able to load from DB.");
        }
        experimentInterface.addExperimentToLocalStorage(KruizeOperator.autotuneObjectMap, kruizeExpList);

        // Load results from the DB and save to local
        List<KruizeResultsEntry> kruizeResultsEntries = experimentDAO.loadAllResults();
        List<UpdateResultsAPIObject> updateResultsAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertResultEntryToUpdateResultsAPIObject(kruizeResultsEntries);
        List<ExperimentResultData> resultDataList = new ArrayList<>();
        for (UpdateResultsAPIObject updateResultsAPIObject : updateResultsAPIObjects) {
            try {
                ExperimentResultData experimentResultData = Converters.KruizeObjectConverters.convertUpdateResultsAPIObjToExperimentResultData(updateResultsAPIObject);
                if (experimentResultData != null)
                    resultDataList.add(experimentResultData);
                else
                    LOGGER.warn("Converted experimentResultData is null");
            } catch (IllegalArgumentException e) {
                LOGGER.error("Failed to convert DB data to local: {}", e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Unexpected error: {}", e.getMessage());
            }
        }
        experimentInterface.addResultsToLocalStorage(KruizeOperator.autotuneObjectMap, resultDataList);
        LOGGER.debug(KruizeOperator.autotuneObjectMap.toString());
    }

    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status) {
        kruizeObject.setStatus(status);
        // TODO   update into database
        return true;
    }
}
