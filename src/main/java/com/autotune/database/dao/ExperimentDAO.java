package com.autotune.database.dao;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.table.*;

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

    // Add DataSource to DB
    ValidationOutputData addDataSourceToDB(KruizeDataSource kruizeDataSource);

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

    // Load a single data source based on name
    List<KruizeDataSource> loadDataSourceByName(String name) throws Exception;

    // Load all results for a particular experimentName

    List<KruizeResultsEntry> loadResultsByExperimentName(String experimentName, String cluster_name, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception;

    // Load all recommendations of a particular experiment
    List<KruizeRecommendationEntry> loadRecommendationsByExperimentName(String experimentName) throws Exception;


    // Load a single Performance Profile based on name
    List<KruizePerformanceProfileEntry> loadPerformanceProfileByName(String performanceProfileName) throws Exception;


    // Load all recommendations of a particular experiment and interval end Time
    KruizeRecommendationEntry loadRecommendationsByExperimentNameAndDate(String experimentName, String cluster_name, Timestamp interval_end_time) throws Exception;

    // Get KruizeResult Record
    List<KruizeResultsEntry> getKruizeResultsEntry(String experiment_name, String cluster_name, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception;

    void deletePartitions(int thresholdDaysCount);

    public void addPartitions(String tableName, String month, String year, int dayOfTheMonth, String partitionType) throws Exception;

    // Load all the datasources
    List<KruizeDataSource> loadAllDataSources() throws Exception;

    List<KruizeMetadata> loadMetadataByName(String clusterGroupName) throws Exception;
    List<KruizeMetadata> loadMetadataByClusterName(String clusterGroupName, String clusterName) throws Exception;
    List<KruizeMetadata> loadMetadataByNamespace(String clusterGroupName, String clusterName, String namespace) throws Exception;

    ValidationOutputData addMetadataToDB(KruizeMetadata kruizeMetadata);

    List<KruizeMetadata> loadMetadata() throws Exception;
}
