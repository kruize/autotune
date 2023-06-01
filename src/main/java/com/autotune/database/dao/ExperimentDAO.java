package com.autotune.database.dao;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.table.KruizeExperimentEntry;
import com.autotune.database.table.KruizePerformanceProfileEntry;
import com.autotune.database.table.KruizeRecommendationEntry;
import com.autotune.database.table.KruizeResultsEntry;

import java.sql.Timestamp;
import java.util.List;

public interface ExperimentDAO {

    // Add New experiments from local storage to DB and set status to Inprogress
    public ValidationOutputData addExperimentToDB(KruizeExperimentEntry kruizeExperimentEntry);

    // Add experiment results from local storage to DB and set status to Inprogress
    public ValidationOutputData addResultsToDB(KruizeResultsEntry resultsEntry);

    public List<KruizeResultsEntry> addToDBAndFetchFailedResults(List<KruizeResultsEntry> kruizeResultsEntries);

    // Add recommendation  to DB
    public ValidationOutputData addRecommendationToDB(KruizeRecommendationEntry recommendationEntry);

    // Add Performance Profile  to DB
    public ValidationOutputData addPerformanceProfileToDB(KruizePerformanceProfileEntry kruizePerformanceProfileEntry);

    // Update experiment status
    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status);

    // Delete experiment
    public ValidationOutputData deleteKruizeExperimentEntryByName(String experimentName);

    // If Kruize object restarts load all experiment which are in inprogress
    public List<KruizeExperimentEntry> loadAllExperiments() throws Exception;

    // If Kruize object restarts load all results from the experiments which are in inprogress
    List<KruizeResultsEntry> loadAllResults() throws Exception;

    // If Kruize restarts load all recommendations
    List<KruizeRecommendationEntry> loadAllRecommendations() throws Exception;

    // If Kruize restarts load all performance profiles
    List<KruizePerformanceProfileEntry> loadAllPerformanceProfiles() throws Exception;

    // Load a single experiment based on experimentName
    List<KruizeExperimentEntry> loadExperimentByName(String experimentName) throws Exception;

    // Load all results for a particular experimentName
    List<KruizeResultsEntry> loadResultsByExperimentName(String experimentName, Timestamp interval_start_time, Integer limitRows) throws Exception;

    // Load all recommendations of a particular experiment
    List<KruizeRecommendationEntry> loadRecommendationsByExperimentName(String experimentName) throws Exception;


    // Load a single Performance Profile based on name
    List<KruizePerformanceProfileEntry> loadPerformanceProfileByName(String performanceProfileName) throws Exception;


    // Load all recommendations of a particular experiment and interval end Time
    KruizeRecommendationEntry loadRecommendationsByExperimentNameAndDate(String experimentName, Timestamp interval_end_time) throws Exception;

    // Get KruizeResult Record
    List<KruizeResultsEntry> getKruizeResultsEntry(String experiment_name, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception;


}
