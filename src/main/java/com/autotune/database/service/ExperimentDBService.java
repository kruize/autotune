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
import com.autotune.analyzer.serviceObjects.*;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.auth.AuthenticationConfig;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataInfo;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.database.dao.ExperimentDAO;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.helper.DBConstants;
import com.autotune.database.helper.DBHelpers;
import com.autotune.database.table.*;
import com.autotune.database.table.lm.KruizeLMExperimentEntry;
import com.autotune.database.table.lm.KruizeLMRecommendationEntry;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.operator.KruizeOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

import static com.autotune.operator.KruizeDeploymentInfo.is_ros_enabled;

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

    public void loadAllLMExperiments(Map<String, KruizeObject> mainKruizeExperimentMap) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        List<KruizeLMExperimentEntry> entries = experimentDAO.loadAllLMExperiments();
        if (null != entries && !entries.isEmpty()) {
            List<CreateExperimentAPIObject> createExperimentAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertLMExperimentEntryToCreateExperimentAPIObject(entries);
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
        KruizeObject kruizeObject;
        // Load results from the DB and save to local
        List<KruizeResultsEntry> kruizeResultsEntries = experimentDAO.loadAllResults();
        if (null != kruizeResultsEntries && !kruizeResultsEntries.isEmpty()) {
            List<UpdateResultsAPIObject> updateResultsAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertResultEntryToUpdateResultsAPIObject(kruizeResultsEntries);
            if (!updateResultsAPIObjects.isEmpty()) {
                List<ExperimentResultData> resultDataList = new ArrayList<>();
                for (UpdateResultsAPIObject updateResultsAPIObject : updateResultsAPIObjects) {
                    try {
                        kruizeObject = mainKruizeExperimentMap.get(updateResultsAPIObject.getExperimentName());
                        updateResultsAPIObject.setKruizeObject(kruizeObject);
                        ExperimentResultData experimentResultData = Converters.KruizeObjectConverters.convertUpdateResultsAPIObjToExperimentResultData(updateResultsAPIObject);
                        resultDataList.add(experimentResultData);
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

    public void loadAllLMRecommendations(Map<String, KruizeObject> mainKruizeExperimentMap) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        // Load Recommendations from DB and save to local
        List<KruizeLMRecommendationEntry> recommendationEntries = experimentDAO.loadAllLMRecommendations();
        if (null != recommendationEntries && !recommendationEntries.isEmpty()) {
            List<ListRecommendationsAPIObject> recommendationsAPIObjects = null;
            try {
                recommendationsAPIObjects = DBHelpers.Converters.KruizeObjectConverters
                        .convertLMRecommendationEntryToRecommendationAPIObject(recommendationEntries);
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

    public void loadAllMetricProfiles(Map<String, PerformanceProfile> metricProfileMap) throws Exception {
        List<KruizeMetricProfileEntry> entries = experimentDAO.loadAllMetricProfiles();
        if (null != entries && !entries.isEmpty()) {
            List<PerformanceProfile> performanceProfiles = DBHelpers.Converters.KruizeObjectConverters.convertMetricProfileEntryToMetricProfileObject(entries);
            if (!performanceProfiles.isEmpty()) {
                performanceProfiles.forEach(performanceProfile ->
                        PerformanceProfileUtil.addMetricProfile(metricProfileMap, performanceProfile));
            }
        }
    }

    public boolean loadResultsFromDBByName(Map<String, KruizeObject> mainKruizeExperimentMap, String experimentName, Timestamp calculated_start_time, Timestamp interval_end_time) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        KruizeObject kruizeObject = mainKruizeExperimentMap.get(experimentName);
        boolean resultsAvailable = false;
        // Load results from the DB and save to local
        List<KruizeResultsEntry> kruizeResultsEntries = experimentDAO.loadResultsByExperimentName(experimentName, kruizeObject.getClusterName(), calculated_start_time, interval_end_time);
        if (null != kruizeResultsEntries && !kruizeResultsEntries.isEmpty()) {
            resultsAvailable = true;
            List<UpdateResultsAPIObject> updateResultsAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertResultEntryToUpdateResultsAPIObject(kruizeResultsEntries);
            if (null != updateResultsAPIObjects && !updateResultsAPIObjects.isEmpty()) {
                List<ExperimentResultData> resultDataList = new ArrayList<>();
                for (UpdateResultsAPIObject updateResultsAPIObject : updateResultsAPIObjects) {
                    updateResultsAPIObject.setKruizeObject(kruizeObject);
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
        return resultsAvailable;
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

    public void loadLMRecommendationsFromDBByName(Map<String, KruizeObject> mainKruizeExperimentMap, String experimentName) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        // Load Recommendations from DB and save to local
        List<KruizeLMRecommendationEntry> recommendationEntries = experimentDAO.loadLMRecommendationsByExperimentName(experimentName);
        if (null != recommendationEntries && !recommendationEntries.isEmpty()) {
            List<ListRecommendationsAPIObject> recommendationsAPIObjects
                    = null;
            try {
                recommendationsAPIObjects = DBHelpers.Converters.KruizeObjectConverters
                        .convertLMRecommendationEntryToRecommendationAPIObject(recommendationEntries);
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
            KruizeLMExperimentEntry kruizeLMExperimentEntry = DBHelpers.Converters.KruizeObjectConverters.convertCreateAPIObjToExperimentDBObj(createExperimentAPIObject);
            if (is_ros_enabled && createExperimentAPIObject.getTargetCluster().equalsIgnoreCase(AnalyzerConstants.REMOTE)) {
                KruizeExperimentEntry oldKruizeExperimentEntry = new KruizeExperimentEntry(kruizeLMExperimentEntry);
                validationOutputData = this.experimentDAO.addExperimentToDB(oldKruizeExperimentEntry);
            } else {
                validationOutputData = this.experimentDAO.addExperimentToDB(kruizeLMExperimentEntry);
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save experiment due to {}", e.getMessage());
        }
        return validationOutputData;
    }

    public List<UpdateResultsAPIObject> addResultsToDB(List<ExperimentResultData> resultDataList) {
        List<KruizeResultsEntry> kruizeResultsEntryList = new ArrayList<>();
        List<UpdateResultsAPIObject> failedUpdateResultsAPIObjects = new ArrayList<>();
        List<KruizeResultsEntry> failedResultsEntries = new ArrayList<>();
        for (ExperimentResultData resultData : resultDataList) {
            KruizeResultsEntry kruizeResultsEntry = DBHelpers.Converters.KruizeObjectConverters.convertExperimentResultToExperimentResultsTable(resultData);
            if (null != kruizeResultsEntry.getErrorReasons() && kruizeResultsEntry.getErrorReasons().size() > 0) {
                failedResultsEntries.add(kruizeResultsEntry);
            } else {
                kruizeResultsEntryList.add(kruizeResultsEntry);
            }
        }
        failedResultsEntries.addAll(experimentDAO.addToDBAndFetchFailedResults(kruizeResultsEntryList));
        failedUpdateResultsAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertResultEntryToUpdateResultsAPIObject(failedResultsEntries);
        return failedUpdateResultsAPIObjects;
    }


    public ValidationOutputData addRecommendationToDB(Map<String, KruizeObject> experimentsMap, KruizeObject kruizeObject,
                                                      Timestamp interval_end_time) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, "", null);
        if (null == kruizeObject) {
            return validationOutputData;
        }
        if (kruizeObject.getKubernetes_objects().isEmpty()) {
            return validationOutputData;
        }
        // TODO: Log the list of invalid experiments and return the error instead of bailing out completely
        if (!experimentsMap.containsKey(kruizeObject.getExperimentName())) {
            LOGGER.error("Trying to locate Recommendation for non existent experiment: " + kruizeObject.getExperimentName());
            return validationOutputData; // todo: need to set the correct message
        }

        if (KruizeDeploymentInfo.is_ros_enabled && kruizeObject.getTarget_cluster().equalsIgnoreCase(AnalyzerConstants.REMOTE)) {
            KruizeRecommendationEntry kr = DBHelpers.Converters.KruizeObjectConverters.
                    convertKruizeObjectTORecommendation(kruizeObject, interval_end_time);
            if (null != kr) {
                ValidationOutputData tempValObj = new ExperimentDAOImpl().addRecommendationToDB(kr);
                if (!tempValObj.isSuccess()) {
                    validationOutputData.setSuccess(false);
                    String errMsg = String.format("Experiment name : %s , Interval end time : %s | ", kruizeObject.getExperimentName(), interval_end_time);
                    validationOutputData.setMessage(validationOutputData.getMessage() + errMsg);
                }
            }
        } else {
            KruizeLMRecommendationEntry kr = DBHelpers.Converters.KruizeObjectConverters.
                    convertKruizeObjectTOLMRecommendation(kruizeObject, interval_end_time);
            if (null != kr) {
                // Create a Calendar object and set the time with the timestamp
                Calendar localDateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                localDateTime.setTime(kr.getInterval_end_time());
                ExperimentDAO dao = new ExperimentDAOImpl();
                int dayOfTheMonth = localDateTime.get(Calendar.DAY_OF_MONTH);
                try {
                    synchronized (new Object()) {
                        dao.addPartitions(DBConstants.TABLE_NAMES.KRUIZE_LM_RECOMMENDATIONS, String.format("%02d", localDateTime.get(Calendar.MONTH) + 1), String.valueOf(localDateTime.get(Calendar.YEAR)), dayOfTheMonth, DBConstants.PARTITION_TYPES.BY_DAY);
                    }
                } catch (Exception e) {
                    LOGGER.warn(e.getMessage());
                }
                ValidationOutputData tempValObj = new ExperimentDAOImpl().addRecommendationToDB(kr);
                if (!tempValObj.isSuccess()) {
                    validationOutputData.setSuccess(false);
                    String errMsg = String.format("Experiment name : %s , Interval end time : %s | ", kruizeObject.getExperimentName(), interval_end_time);
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

    /**
     * Adds Metric Profile to kruizeMetricProfileEntry
     *
     * @param metricProfile Metric profile object to be added
     * @return ValidationOutputData object
     */
    public ValidationOutputData addMetricProfileToDB(PerformanceProfile metricProfile) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        try {
            KruizeMetricProfileEntry kruizeMetricProfileEntry = DBHelpers.Converters.KruizeObjectConverters.convertMetricProfileObjToMetricProfileDBObj(metricProfile);
            validationOutputData = this.experimentDAO.addMetricProfileToDB(kruizeMetricProfileEntry);
        } catch (Exception e) {
            LOGGER.error("Not able to save Metric Profile due to {}", e.getMessage());
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

    public void loadLMExperimentFromDBByName(Map<String, KruizeObject> mainKruizeExperimentMap, String experimentName) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        List<KruizeLMExperimentEntry> entries = experimentDAO.loadLMExperimentByName(experimentName);
        if (null != entries && !entries.isEmpty()) {
            List<CreateExperimentAPIObject> createExperimentAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertLMExperimentEntryToCreateExperimentAPIObject(entries);
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

    public void loadExperimentFromDBByInputJSON(Map<String, KruizeObject> mKruizeExperimentMap, StringBuilder clusterName, List<KubernetesAPIObject> kubernetesAPIObjectList) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        // assuming there will be only one Kubernetes object
        KubernetesAPIObject kubernetesAPIObject = kubernetesAPIObjectList.get(0);
        List<KruizeExperimentEntry> entries = experimentDAO.loadExperimentFromDBByInputJSON(clusterName, kubernetesAPIObject);
        if (null != entries && !entries.isEmpty()) {
            List<CreateExperimentAPIObject> createExperimentAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertExperimentEntryToCreateExperimentAPIObject(entries);
            if (!createExperimentAPIObjects.isEmpty()) {
                List<KruizeObject> kruizeExpList = new ArrayList<>();
                for (CreateExperimentAPIObject createExperimentAPIObject : createExperimentAPIObjects) {
                    KruizeObject kruizeObject = Converters.KruizeObjectConverters.convertCreateExperimentAPIObjToKruizeObject(createExperimentAPIObject);
                    if (null != kruizeObject) {
                        kruizeExpList.add(kruizeObject);
                    }
                }
                experimentInterface.addExperimentToLocalStorage(mKruizeExperimentMap, kruizeExpList);
            }
        }
    }

    public void loadLMExperimentFromDBByInputJSON(Map<String, KruizeObject> mKruizeExperimentMap, StringBuilder clusterName, List<KubernetesAPIObject> kubernetesAPIObjectList) throws Exception {
        ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
        // assuming there will be only one Kubernetes object
        KubernetesAPIObject kubernetesAPIObject = kubernetesAPIObjectList.get(0);
        List<KruizeLMExperimentEntry> entries = experimentDAO.loadLMExperimentFromDBByInputJSON(clusterName, kubernetesAPIObject);
        if (null != entries && !entries.isEmpty()) {
            List<CreateExperimentAPIObject> createExperimentAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertLMExperimentEntryToCreateExperimentAPIObject(entries);
            if (!createExperimentAPIObjects.isEmpty()) {
                List<KruizeObject> kruizeExpList = new ArrayList<>();
                for (CreateExperimentAPIObject createExperimentAPIObject : createExperimentAPIObjects) {
                    KruizeObject kruizeObject = Converters.KruizeObjectConverters.convertCreateExperimentAPIObjToKruizeObject(createExperimentAPIObject);
                    if (null != kruizeObject) {
                        kruizeExpList.add(kruizeObject);
                    }
                }
                experimentInterface.addExperimentToLocalStorage(mKruizeExperimentMap, kruizeExpList);
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

    public void loadLMExperimentAndRecommendationsFromDBByName(Map<String, KruizeObject> mainKruizeExperimentMap, String experimentName) throws Exception {

        loadLMExperimentFromDBByName(mainKruizeExperimentMap, experimentName);

        loadLMRecommendationsFromDBByName(mainKruizeExperimentMap, experimentName);
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

    /**
     * Fetches Metric Profile by name from kruizeMetricProfileEntry
     *
     * @param metricProfileMap  Map to store metric profile loaded from the database
     * @param metricProfileName Metric profile name to be fetched
     * @return ValidationOutputData object
     */
    public void loadMetricProfileFromDBByName(Map<String, PerformanceProfile> metricProfileMap, String metricProfileName) throws Exception {
        List<KruizeMetricProfileEntry> entries = experimentDAO.loadMetricProfileByName(metricProfileName);
        if (null != entries && !entries.isEmpty()) {
            List<PerformanceProfile> metricProfiles = DBHelpers.Converters.KruizeObjectConverters
                    .convertMetricProfileEntryToMetricProfileObject(entries);
            if (!metricProfiles.isEmpty()) {
                for (PerformanceProfile performanceProfile : metricProfiles) {
                    if (null != performanceProfile) {
                        PerformanceProfileUtil.addMetricProfile(metricProfileMap, performanceProfile);
                    }
                }
            }
        }
    }

    public void loadAllExperimentsAndRecommendations(Map<String, KruizeObject> mainKruizeExperimentMap) throws Exception {

        loadAllExperiments(mainKruizeExperimentMap);

        loadAllRecommendations(mainKruizeExperimentMap);
    }

    public void loadAllLMExperimentsAndRecommendations(Map<String, KruizeObject> mainKruizeExperimentMap) throws Exception {

        loadAllLMExperiments(mainKruizeExperimentMap);

        loadAllLMRecommendations(mainKruizeExperimentMap);
    }

    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status) {
        kruizeObject.setStatus(status);
        // TODO   update into database
        return true;
    }


    public List<ExperimentResultData> getExperimentResultData(String experiment_name, KruizeObject kruizeObject, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception {
        List<ExperimentResultData> experimentResultDataList = new ArrayList<>();
        List<KruizeResultsEntry> kruizeResultsEntryList = experimentDAO.getKruizeResultsEntry(experiment_name, kruizeObject.getClusterName(), interval_start_time, interval_end_time);
        if (null != kruizeResultsEntryList) {
            List<UpdateResultsAPIObject> updateResultsAPIObjects = DBHelpers.Converters.KruizeObjectConverters.convertResultEntryToUpdateResultsAPIObject(kruizeResultsEntryList);
            for (UpdateResultsAPIObject updateObject : updateResultsAPIObjects) {
                updateObject.setKruizeObject(kruizeObject);
                experimentResultDataList.add(
                        Converters.KruizeObjectConverters.convertUpdateResultsAPIObjToExperimentResultData(updateObject)
                );
            }
        }
        return experimentResultDataList;
    }

    /**
     * adds datasource to database table
     *
     * @param dataSourceInfo       DataSourceInfo object
     * @param validationOutputData contains validation data
     * @return ValidationOutputData object
     */
    public ValidationOutputData addDataSourceToDB(DataSourceInfo dataSourceInfo, ValidationOutputData validationOutputData) {
        try {
            KruizeDataSourceEntry kruizeDataSource = DBHelpers.Converters.KruizeObjectConverters.convertDataSourceToDataSourceDBObj(dataSourceInfo);
            validationOutputData = this.experimentDAO.addDataSourceToDB(kruizeDataSource, validationOutputData);
        } catch (Exception e) {
            LOGGER.error("Not able to save data source due to {}", e.getMessage());
        }
        return validationOutputData;
    }

    public ValidationOutputData addAuthenticationDetailsToDB(AuthenticationConfig authenticationConfig, String serviceType) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        try {
            KruizeAuthenticationEntry kruizeAuthenticationEntry = DBHelpers.Converters.KruizeObjectConverters.convertAuthDetailsToAuthDetailsDBObj(authenticationConfig, serviceType);
            validationOutputData = this.experimentDAO.addAuthenticationDetailsToDB(kruizeAuthenticationEntry);
        } catch (Exception e) {
            LOGGER.error("Unable to save authentication details: {}", e.getMessage());
        }
        return validationOutputData;
    }

    /**
     * fetches datasource with specified name from database
     *
     * @param name String containing the name of datasource
     * @return DataSourceInfo object containing the details
     */
    public DataSourceInfo loadDataSourceFromDBByName(String name) throws Exception {
        List<KruizeDataSourceEntry> kruizeDataSourceList = experimentDAO.loadDataSourceByName(name);
        List<DataSourceInfo> dataSourceInfoList = new ArrayList<>();
        if (null != kruizeDataSourceList && !kruizeDataSourceList.isEmpty()) {
            dataSourceInfoList = DBHelpers.Converters.KruizeObjectConverters
                    .convertKruizeDataSourceToDataSourceObject(kruizeDataSourceList);
        }
        if (dataSourceInfoList.isEmpty())
            return null;
        else
            return dataSourceInfoList.get(0);
    }

    /**
     * fetches all available datasource from database
     *
     * @return List containing DataSourceInfo objects
     */
    public List<DataSourceInfo> loadAllDataSources() throws Exception {
        List<KruizeDataSourceEntry> entries = experimentDAO.loadAllDataSources();
        List<DataSourceInfo> dataSourceInfoList = null;
        if (null != entries && !entries.isEmpty()) {
            dataSourceInfoList = DBHelpers.Converters.KruizeObjectConverters.convertKruizeDataSourceToDataSourceObject(entries);
            return dataSourceInfoList;
        }
        return dataSourceInfoList;
    }

    /**
     * adds metadata to database table
     *
     * @param dataSourceMetadataInfo DataSourceMetadataInfo object
     * @return
     */
    public ValidationOutputData addMetadataToDB(DataSourceMetadataInfo dataSourceMetadataInfo) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        try {
            List<KruizeDSMetadataEntry> kruizeMetadataList = DBHelpers.Converters.KruizeObjectConverters.convertDataSourceMetadataToMetadataObj(dataSourceMetadataInfo);
            for (KruizeDSMetadataEntry kruizeMetadata : kruizeMetadataList) {
                validationOutputData = this.experimentDAO.addMetadataToDB(kruizeMetadata);
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save metadata due to {}", e.getMessage());
        }
        return validationOutputData;
    }

    /**
     * fetches metadata of specified datasource name from database
     *
     * @param dataSourceName String containing the name of datasource
     * @param verbose
     * @return DataSourceMetadataInfo object containing metadata
     */
    public DataSourceMetadataInfo loadMetadataFromDBByName(String dataSourceName, String verbose) throws Exception {
        List<KruizeDSMetadataEntry> kruizeMetadataList = experimentDAO.loadMetadataByName(dataSourceName);
        List<DataSourceMetadataInfo> dataSourceDetailsInfoList = new ArrayList<>();
        if (null != kruizeMetadataList && !kruizeMetadataList.isEmpty()) {
            if (verbose.equals(AnalyzerConstants.ServiceConstants.FALSE)) {
                dataSourceDetailsInfoList = DBHelpers.Converters.KruizeObjectConverters
                        .convertKruizeMetadataToClusterLevelDataSourceMetadata(kruizeMetadataList);
            } else {
                dataSourceDetailsInfoList = DBHelpers.Converters.KruizeObjectConverters
                        .convertKruizeMetadataToDataSourceMetadataObject(kruizeMetadataList);
            }
        }
        if (dataSourceDetailsInfoList.isEmpty())
            return null;
        else
            return dataSourceDetailsInfoList.get(0);
    }

    /**
     * fetches metadata of specified datasource and cluster name from database
     *
     * @param dataSourceName String containing the name of datasource
     * @param clusterName    String containing the cluster name
     * @param verbose
     * @return DataSourceMetadataInfo object containing metadata
     */
    public DataSourceMetadataInfo loadMetadataFromDBByClusterName(String dataSourceName, String clusterName, String verbose) throws Exception {
        List<KruizeDSMetadataEntry> kruizeMetadataList = experimentDAO.loadMetadataByClusterName(dataSourceName, clusterName);
        List<DataSourceMetadataInfo> dataSourceMetadataInfoList = new ArrayList<>();
        if (null != kruizeMetadataList && !kruizeMetadataList.isEmpty()) {
            if (verbose.equals(AnalyzerConstants.ServiceConstants.FALSE)) {
                dataSourceMetadataInfoList = DBHelpers.Converters.KruizeObjectConverters
                        .convertKruizeMetadataToNamespaceLevelDataSourceMetadata(kruizeMetadataList);
            } else {
                dataSourceMetadataInfoList = DBHelpers.Converters.KruizeObjectConverters
                        .convertKruizeMetadataToDataSourceMetadataObject(kruizeMetadataList);
            }
        }
        if (dataSourceMetadataInfoList.isEmpty())
            return null;
        else
            return dataSourceMetadataInfoList.get(0);
    }

    /**
     * fetches metadata of specified datasource,cluster and namespace from database
     *
     * @param dataSourceName String containing the name of datasource
     * @param clusterName    String containing the name of datasource
     * @param namespace      String containing the name of datasource
     * @return DataSourceMetadataInfo object containing metadata
     * @throws Exception
     */
    public DataSourceMetadataInfo loadMetadataFromDBByNamespace(String dataSourceName, String clusterName, String namespace) throws Exception {
        List<KruizeDSMetadataEntry> kruizeMetadataList = experimentDAO.loadMetadataByNamespace(dataSourceName, clusterName, namespace);
        List<DataSourceMetadataInfo> dataSourceMetadataInfoList = new ArrayList<>();
        if (null != kruizeMetadataList && !kruizeMetadataList.isEmpty()) {
            dataSourceMetadataInfoList = DBHelpers.Converters.KruizeObjectConverters
                    .convertKruizeMetadataToDataSourceMetadataObject(kruizeMetadataList);
        }
        if (dataSourceMetadataInfoList.isEmpty())
            return null;
        else
            return dataSourceMetadataInfoList.get(0);
    }
}
