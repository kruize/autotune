package com.autotune.database.dao;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.table.KruizeExperimentEntry;
import com.autotune.database.table.KruizeRecommendationEntry;
import com.autotune.database.table.KruizeResultsEntry;

import java.util.List;

public interface ExperimentDAO {

    // Add New experiments from local storage to DB and set status to Inprogress
    public ValidationOutputData addExperimentToDB(KruizeExperimentEntry kruizeExperimentEntry);

    // Add experiment results from local storage to DB and set status to Inprogress
    public ValidationOutputData addResultsToDB(KruizeResultsEntry resultsEntry);

    // Add recommendation  to DB
    public ValidationOutputData addRecommendationToDB(KruizeRecommendationEntry recommendationEntry);

    // Update experiment status
    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status);

    // Delete experiment
    public ValidationOutputData deleteKruizeExperimentEntryByName(String experimentName);

    // If Kruize object restarts load all experiment which are in inprogress
    public List<KruizeExperimentEntry> loadAllExperiments() throws Exception;

    public List<KruizeExperimentEntry> loadPaginatedExperiments(int page, int limit) throws Exception;

    // If Kruize object restarts load all results from the experiments which are in inprogress
    List<KruizeResultsEntry> loadAllResults() throws Exception;
    List<KruizeResultsEntry> loadPaginatedResults(int page, int limit) throws Exception;

    // If Kruize restarts load all recommendations
    List<KruizeRecommendationEntry> loadAllRecommendations() throws Exception;

    List<KruizeRecommendationEntry> loadPaginatedRecommendations(int page, int limit) throws Exception;

    // Load a single experiment based on experimentName
    List<KruizeExperimentEntry> loadExperimentByName(String experimentName) throws Exception;

    // Load all results for a particular experimentName
    List<KruizeResultsEntry> loadResultsByExperimentName(String experimentName) throws Exception;

    List<KruizeResultsEntry> loadPaginatedResultsByExperimentName(String experimentName, int page, int limit) throws Exception;

    // Load all recommendations of a particular experiment
    List<KruizeRecommendationEntry> loadRecommendationsByExperimentName(String experimentName) throws Exception;

    List<KruizeRecommendationEntry> loadPaginatedRecommendationsByExperimentName(String experimentName, int page, int limit) throws Exception;
}
