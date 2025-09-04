package com.autotune.database.dao;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.KubernetesAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.table.*;
import com.autotune.database.table.lm.KruizeBulkJobEntry;
import com.autotune.database.table.lm.KruizeLMExperimentEntry;
import com.autotune.database.table.lm.KruizeLMMetadataProfileEntry;
import com.autotune.database.table.lm.KruizeLMRecommendationEntry;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

public interface ExperimentDAO {

    // Add New experiments from local storage to DB and set status to Inprogress
    public ValidationOutputData addExperimentToDB(KruizeExperimentEntry kruizeExperimentEntry);

    public ValidationOutputData addExperimentToDB(KruizeLMExperimentEntry kruizeLMExperimentEntry);

    // Add experiment results from local storage to DB and set status to Inprogress
    public ValidationOutputData addResultsToDB(KruizeResultsEntry resultsEntry);

    public List<KruizeResultsEntry> addToDBAndFetchFailedResults(List<KruizeResultsEntry> kruizeResultsEntries);

    // Add recommendation  to DB
    public ValidationOutputData addRecommendationToDB(KruizeRecommendationEntry recommendationEntry);

    // Add recommendation  to DB
    public ValidationOutputData addRecommendationToDB(KruizeLMRecommendationEntry recommendationEntry);


    // Add Performance Profile  to DB
    public ValidationOutputData addPerformanceProfileToDB(KruizePerformanceProfileEntry kruizePerformanceProfileEntry);

    // Add Metric Profile  to DB
    public ValidationOutputData addMetricProfileToDB(KruizeMetricProfileEntry kruizeMetricProfileEntry);

    // Add Metadata Profile  to DB
    public ValidationOutputData addMetadataProfileToDB(KruizeLMMetadataProfileEntry kruizeMetadataProfileEntry);

    // Update Metadata Profile to DB
    public ValidationOutputData updateMetadataProfileToDB(KruizeLMMetadataProfileEntry kruizeMetadataProfileEntry);

    // Add DataSource to DB
    ValidationOutputData addDataSourceToDB(KruizeDataSourceEntry kruizeDataSourceEntry, ValidationOutputData validationOutputData);

    // Update experiment status
    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status);

    // Delete RM experiment
    public ValidationOutputData deleteKruizeExperimentEntryByName(String experimentName);

    // Delete LM experiment
    public ValidationOutputData deleteKruizeLMExperimentEntryByName(String experimentName);

    // If Kruize object restarts load all experiment which are in inprogress
    public List<KruizeExperimentEntry> loadAllExperiments() throws Exception;


    public List<KruizeLMExperimentEntry> loadAllLMExperiments() throws Exception;

    // If Kruize object restarts load all results from the experiments which are in inprogress
    List<KruizeResultsEntry> loadAllResults() throws Exception;

    // If Kruize restarts load all recommendations
    List<KruizeRecommendationEntry> loadAllRecommendations() throws Exception;

    List<KruizeLMRecommendationEntry> loadAllLMRecommendations(String bulkJobID) throws Exception;

    // If Kruize restarts load all performance profiles
    List<KruizePerformanceProfileEntry> loadAllPerformanceProfiles() throws Exception;

    // If Kruize restarts load all metric profiles
    List<KruizeMetricProfileEntry> loadAllMetricProfiles() throws Exception;

    // If Kruize restarts load all metadata profiles
    List<KruizeLMMetadataProfileEntry> loadAllMetadataProfiles() throws Exception;

    // Load a single experiment based on experimentName
    List<KruizeExperimentEntry> loadExperimentByName(String experimentName) throws Exception;

    // Load a single experiment based on experimentName
    List<KruizeLMExperimentEntry> loadLMExperimentByName(String experimentName) throws Exception;


    // Load a single data source based on name
    List<KruizeDataSourceEntry> loadDataSourceByName(String name) throws Exception;

    // Load all results for a particular experimentName

    List<KruizeResultsEntry> loadResultsByExperimentName(String experimentName, String cluster_name, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception;

    // Load all recommendations of a particular experiment
    List<KruizeRecommendationEntry> loadRecommendationsByExperimentName(String experimentName) throws Exception;

    // Load all recommendations of a particular experiment
    List<KruizeLMRecommendationEntry> loadLMRecommendationsByExperimentName(String experimentName, String bulkJobId) throws Exception;

    // Load a single Performance Profile based on name
    List<KruizePerformanceProfileEntry> loadPerformanceProfileByName(String performanceProfileName) throws Exception;

    // Load a single Metric Profile based on name
    List<KruizeMetricProfileEntry> loadMetricProfileByName(String metricProfileName) throws Exception;

    // Load a single Metadata Profile based on name
    List<KruizeLMMetadataProfileEntry> loadMetadataProfileByName(String metadataProfileName) throws Exception;

    // Delete metric profile for the specified metric profile name
    public ValidationOutputData deleteKruizeMetricProfileEntryByName(String metricProfileName);

    // Delete metadata profile for the specified metadata profile name
    public ValidationOutputData deleteKruizeLMMetadataProfileEntryByName(String metadataProfileName);

    // Load all recommendations of a particular experiment and interval end Time
    KruizeRecommendationEntry loadRecommendationsByExperimentNameAndDate(String experimentName, String cluster_name, Timestamp interval_end_time) throws Exception;

    KruizeLMRecommendationEntry loadLMRecommendationsByExperimentNameAndDate(String experimentName, String cluster_name, Timestamp interval_end_time) throws Exception;

    // Get KruizeResult Record
    List<KruizeResultsEntry> getKruizeResultsEntry(String experiment_name, String cluster_name, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception;

    void deletePartitions(int thresholdDaysCount);

    public void addPartitions(String tableName, String month, String year, int dayOfTheMonth, String partitionType) throws Exception;

    List<KruizeExperimentEntry> loadExperimentFromDBByInputJSON(StringBuilder clusterName, KubernetesAPIObject kubernetesAPIObject) throws Exception;

    List<KruizeLMExperimentEntry> loadLMExperimentFromDBByInputJSON(StringBuilder clusterName, KubernetesAPIObject kubernetesAPIObject) throws Exception;

    // Load all the datasources
    List<KruizeDataSourceEntry> loadAllDataSources() throws Exception;

    // Load data source metadata by datasource name
    List<KruizeDSMetadataEntry> loadMetadataByName(String dataSourceName) throws Exception;

    // Load data source metadata by cluster name
    List<KruizeDSMetadataEntry> loadMetadataByClusterName(String dataSourceName, String clusterName) throws Exception;

    // Load data source metadata by namespace
    List<KruizeDSMetadataEntry> loadMetadataByNamespace(String dataSourceName, String clusterName, String namespace) throws Exception;

    // add metadata
    ValidationOutputData addMetadataToDB(KruizeDSMetadataEntry kruizeDSMetadataEntry);

    // Delete metadata
    public ValidationOutputData deleteKruizeDSMetadataEntryByName(String dataSourceName);

    ValidationOutputData addAuthenticationDetailsToDB(KruizeAuthenticationEntry kruizeAuthenticationEntry);

    // save ,get, partial update and delete  BulkJob data
    ValidationOutputData bulkJobSave(KruizeBulkJobEntry kruizeBulkJobEntry);

    KruizeBulkJobEntry findBulkJobById(String jobId) throws Exception;

    ValidationOutputData updateBulkJobByExperiment(String jobId, String experimentName, String notification, String recommendationJson) throws Exception;

    void deleteBulkJobByID(String jobId);

    boolean updateExperimentDates(Set<String> experimentNames, Timestamp currentTimestamp) throws Exception;

    void deleteOldPerformanceProfile(Double currentPerformanceProfileVersion) throws Exception;

}
