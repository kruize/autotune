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

import com.autotune.analyzer.exceptions.InvalidConversionOfRecommendationEntryException;
import com.autotune.analyzer.experiment.ExperimentInterface;
import com.autotune.analyzer.experiment.ExperimentInterfaceImpl;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.utils.PerformanceProfileUtil;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.CreateExperimentAPIObject;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.database.dao.ExperimentDAO;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.helper.DBHelpers;
import com.autotune.database.table.KruizeExperimentEntry;
import com.autotune.database.table.KruizePerformanceProfileEntry;
import com.autotune.database.table.KruizeRecommendationEntry;
import com.autotune.database.table.KruizeResultsEntry;
import com.autotune.operator.KruizeOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
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

    public void loadAllExperiments(Map<String, KruizeObject> mainKruizeExperimentMap) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        List<KruizeExperimentEntry> entries = experimentDAO.loadAllExperiments();
        if (null != entries && !entries.isEmpty()) {
            List<CreateExperimentAPIObject> createExperimentAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertExperimentEntryToCreateExperimentAPIObject(entries);
            if (null != createExperimentAPIObjects && !createExperimentAPIObjects.isEmpty()) {
                List<KruizeObject> kruizeExpList = new ArrayList<>();

                int failureThreshHold = createExperimentAPIObjects.size();
                int failureCount = 0;
                for (CreateExperimentAPIObject createExperimentAPIObject : createExperimentAPIObjects) {
                    KruizeObject kruizeObject = Converters.KruizeObjectConverters.convertCreateExperimentAPIObjToKruizeObject(createExperimentAPIObject);
                    if (null != kruizeObject) {
                        kruizeExpList.add(kruizeObject);
                    } else {
                        failureCount++;
                    }
                }
                if (failureThreshHold > 0 && failureCount == failureThreshHold) {
                    throw new Exception("None of the experiments are able to load from DB.");
                }
                experimentInterface.addExperimentToLocalStorage(mainKruizeExperimentMap, kruizeExpList);
            }
        }
    }

    public void loadAllResults(Map<String, KruizeObject> mainKruizeExperimentMap) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();

        // Load results from the DB and save to local
        List<KruizeResultsEntry> kruizeResultsEntries = experimentDAO.loadAllResults();
        if (null != kruizeResultsEntries && !kruizeResultsEntries.isEmpty()) {
            List<UpdateResultsAPIObject> updateResultsAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertResultEntryToUpdateResultsAPIObject(kruizeResultsEntries);
            if (null != updateResultsAPIObjects && !updateResultsAPIObjects.isEmpty()) {
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
                experimentInterface.addResultsToLocalStorage(mainKruizeExperimentMap, resultDataList);
            }
        }
    }

    public void loadAllRecommendations(Map<String, KruizeObject> mainKruizeExperimentMap) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();

        // Load Recommendations from DB and save to local
        List<KruizeRecommendationEntry> recommendationEntries = experimentDAO.loadAllRecommendations();
        if (null != recommendationEntries && !recommendationEntries.isEmpty()) {
            List<ListRecommendationsAPIObject> recommendationsAPIObjects
                    = null;
            try {
                recommendationsAPIObjects = DBHelpers.Converters.KruizeObjectConverters
                        .convertRecommendationEntryToRecommendationAPIObject(recommendationEntries);
            } catch (InvalidConversionOfRecommendationEntryException e) {
                e.printStackTrace();
            }
            if (null != recommendationsAPIObjects && !recommendationsAPIObjects.isEmpty()) {

                experimentInterface.addRecommendationsToLocalStorage(mainKruizeExperimentMap,
                        recommendationsAPIObjects,
                        true);
            }
        }
    }

    public void loadAllPerformanceProfiles(Map<String, PerformanceProfile> performanceProfileMap) throws Exception {
        if (performanceProfileMap.isEmpty()) {
            List<KruizePerformanceProfileEntry> entries = experimentDAO.loadAllPerformanceProfiles();
            if (null != entries && !entries.isEmpty()) {
                List<PerformanceProfile> performanceProfiles = DBHelpers.Converters.KruizeObjectConverters.convertPerformanceProfileEntryToPerformanceProfileObject(entries);
                if (!performanceProfiles.isEmpty()) {
                    performanceProfiles.forEach(performanceProfile ->
                            PerformanceProfileUtil.addPerformanceProfile(performanceProfileMap, performanceProfile));
                }
            }
        }
    }

    public void loadResultsFromDBByName(Map<String, KruizeObject> mainKruizeExperimentMap, String experimentName, Timestamp interval_end_time, Integer limitRows) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        KruizeObject kruizeObject = mainKruizeExperimentMap.get(experimentName);
        // Load results from the DB and save to local
        List<KruizeResultsEntry> kruizeResultsEntries = experimentDAO.loadResultsByExperimentName(experimentName, kruizeObject.getClusterName(), interval_end_time, limitRows);
        if (null != kruizeResultsEntries && !kruizeResultsEntries.isEmpty()) {
            List<UpdateResultsAPIObject> updateResultsAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertResultEntryToUpdateResultsAPIObject(kruizeResultsEntries);
            if (null != updateResultsAPIObjects && !updateResultsAPIObjects.isEmpty()) {
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
                experimentInterface.addResultsToLocalStorage(mainKruizeExperimentMap, resultDataList);
            }
        }
    }

    public void loadRecommendationsFromDBByName(Map<String, KruizeObject> mainKruizeExperimentMap, String experimentName) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        // Load Recommendations from DB and save to local
        List<KruizeRecommendationEntry> recommendationEntries = experimentDAO.loadRecommendationsByExperimentName(experimentName);
        if (null != recommendationEntries && !recommendationEntries.isEmpty()) {
            List<ListRecommendationsAPIObject> recommendationsAPIObjects
                    = null;
            try {
                recommendationsAPIObjects = DBHelpers.Converters.KruizeObjectConverters
                        .convertRecommendationEntryToRecommendationAPIObject(recommendationEntries);
            } catch (InvalidConversionOfRecommendationEntryException e) {
                e.printStackTrace();
            }
            if (null != recommendationsAPIObjects && !recommendationsAPIObjects.isEmpty()) {
                experimentInterface.addRecommendationsToLocalStorage(mainKruizeExperimentMap,
                        recommendationsAPIObjects,
                        true);
            }
        }
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

    public List<UpdateResultsAPIObject> addResultsToDB(List<ExperimentResultData> resultDataList) {
        List<KruizeResultsEntry> kruizeResultsEntryList = new ArrayList<>();
        List<UpdateResultsAPIObject> failedUpdateResultsAPIObjects = new ArrayList<>();
        for (ExperimentResultData resultData : resultDataList) {
            KruizeResultsEntry kruizeResultsEntry = DBHelpers.Converters.KruizeObjectConverters.convertExperimentResultToExperimentResultsTable(resultData);
            kruizeResultsEntryList.add(kruizeResultsEntry);
        }
        List<KruizeResultsEntry> failedResultsEntries = experimentDAO.addToDBAndFetchFailedResults(kruizeResultsEntryList);
        failedUpdateResultsAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertResultEntryToUpdateResultsAPIObject(failedResultsEntries);
        return failedUpdateResultsAPIObjects;
    }


    public ValidationOutputData addRecommendationToDB(Map<String, KruizeObject> experimentsMap, List<ExperimentResultData> experimentResultDataList) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, "", null);
        if (null == experimentResultDataList) {
            return validationOutputData;
        }
        if (experimentResultDataList.size() == 0) {
            return validationOutputData;
        }
        for (ExperimentResultData experimentResultData : experimentResultDataList) {
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
                ValidationOutputData tempValObj = new ExperimentDAOImpl().addRecommendationToDB(kr);
                if (!tempValObj.isSuccess()) {
                    validationOutputData.setSuccess(false);
                    String errMsg = String.format("Experiment name : %s , Interval end time : %s | ", experimentResultData.getExperiment_name(), experimentResultData.getIntervalEndTime());
                    validationOutputData.setMessage(validationOutputData.getMessage() + errMsg);
                }
            }
        }
        if (validationOutputData.getMessage().equals(""))
            validationOutputData.setSuccess(true);
        return validationOutputData;
    }

    public ValidationOutputData addPerformanceProfileToDB(PerformanceProfile performanceProfile) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        try {
            KruizePerformanceProfileEntry kruizePerformanceProfileEntry = DBHelpers.Converters.KruizeObjectConverters.convertPerfProfileObjToPerfProfileDBObj(performanceProfile);
            validationOutputData = this.experimentDAO.addPerformanceProfileToDB(kruizePerformanceProfileEntry);
        } catch (Exception e) {
            LOGGER.error("Not able to save Performance Profile due to {}", e.getMessage());
        }
        return validationOutputData;
    }

    /*
     * This is a Java method that loads all experiments from the database using an experimentDAO object.
     * The method then converts the retrieved data into KruizeObject format, adds them to a list,
     * and sends it to the ExperimentInterface implementation to store the objects.
     *
     * DEPRECATED: DO NOT USE
     */
    public void loadAllExperimentsData() throws Exception {
        loadAllExperiments(KruizeOperator.autotuneObjectMap);

        loadAllResults(KruizeOperator.autotuneObjectMap);

        loadAllRecommendations(KruizeOperator.autotuneObjectMap);
    }

    public void loadExperimentFromDBByName(Map<String, KruizeObject> mainKruizeExperimentMap, String experimentName) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        List<KruizeExperimentEntry> entries = experimentDAO.loadExperimentByName(experimentName);
        if (null != entries && !entries.isEmpty()) {
            List<CreateExperimentAPIObject> createExperimentAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertExperimentEntryToCreateExperimentAPIObject(entries);
            if (null != createExperimentAPIObjects && !createExperimentAPIObjects.isEmpty()) {
                List<KruizeObject> kruizeExpList = new ArrayList<>();

                int failureThreshHold = createExperimentAPIObjects.size();
                int failureCount = 0;
                for (CreateExperimentAPIObject createExperimentAPIObject : createExperimentAPIObjects) {
                    KruizeObject kruizeObject = Converters.KruizeObjectConverters.convertCreateExperimentAPIObjToKruizeObject(createExperimentAPIObject);
                    if (null != kruizeObject) {
                        kruizeExpList.add(kruizeObject);
                    } else {
                        failureCount++;
                    }
                }
                if (failureThreshHold > 0 && failureCount == failureThreshHold) {
                    throw new Exception("Experiment " + experimentName + " unable to load from DB.");
                }
                experimentInterface.addExperimentToLocalStorage(mainKruizeExperimentMap, kruizeExpList);
            }
        }
    }


    public void loadExperimentAndResultsFromDBByName(Map<String, KruizeObject> mainKruizeExperimentMap, String experimentName) throws Exception {

        loadExperimentFromDBByName(mainKruizeExperimentMap, experimentName);
        loadResultsFromDBByName(mainKruizeExperimentMap, experimentName, null, null);
    }


    public void loadExperimentAndRecommendationsFromDBByName(Map<String, KruizeObject> mainKruizeExperimentMap, String experimentName) throws Exception {

        loadExperimentFromDBByName(mainKruizeExperimentMap, experimentName);

        loadRecommendationsFromDBByName(mainKruizeExperimentMap, experimentName);
    }

    public void loadPerformanceProfileFromDBByName(Map<String, PerformanceProfile> performanceProfileMap, String performanceProfileName) throws Exception {
        List<KruizePerformanceProfileEntry> entries = experimentDAO.loadPerformanceProfileByName(performanceProfileName);
        if (null != entries && !entries.isEmpty()) {
            List<PerformanceProfile> performanceProfiles = DBHelpers.Converters.KruizeObjectConverters
                    .convertPerformanceProfileEntryToPerformanceProfileObject(entries);
            if (!performanceProfiles.isEmpty()) {
                for (PerformanceProfile performanceProfile : performanceProfiles) {
                    if (null != performanceProfile) {
                        PerformanceProfileUtil.addPerformanceProfile(performanceProfileMap, performanceProfile);
                    }
                }
            }
        }
    }

    public void loadAllExperimentsAndRecommendations(Map<String, KruizeObject> mainKruizeExperimentMap) throws Exception {

        loadAllExperiments(mainKruizeExperimentMap);

        loadAllRecommendations(mainKruizeExperimentMap);
    }

    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status) {
        kruizeObject.setStatus(status);
        // TODO   update into database
        return true;
    }


    public List<ExperimentResultData> getExperimentResultData(String experiment_name, String clusterName, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception {
        List<ExperimentResultData> experimentResultDataList = new ArrayList<>();
        List<KruizeResultsEntry> kruizeResultsEntryList = experimentDAO.getKruizeResultsEntry(experiment_name, clusterName, interval_start_time, interval_end_time);
        if (null != kruizeResultsEntryList) {
            List<UpdateResultsAPIObject> updateResultsAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertResultEntryToUpdateResultsAPIObject(kruizeResultsEntryList);
            for (UpdateResultsAPIObject updateObject : updateResultsAPIObjects) {
                experimentResultDataList.add(
                        Converters.KruizeObjectConverters.convertUpdateResultsAPIObjToExperimentResultData(updateObject)
                );
            }
        }
        return experimentResultDataList;
    }
}
