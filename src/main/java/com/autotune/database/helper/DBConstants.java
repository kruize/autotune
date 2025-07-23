package com.autotune.database.helper;

import com.autotune.utils.KruizeConstants;

public class DBConstants {

    public static final class SQLQUERY {
        public static final String SELECT_FROM_EXPERIMENTS = "from KruizeExperimentEntry";
        public static final String SELECT_FROM_LM_EXPERIMENTS = "from KruizeLMExperimentEntry";
        public static final String SELECT_FROM_EXPERIMENTS_BY_EXP_NAME = "from KruizeExperimentEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_LM_EXPERIMENTS_BY_EXP_NAME = "from KruizeLMExperimentEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_BULKJOBS_BY_JOB_ID = "from KruizeBulkJobEntry k WHERE k.jobId = :jobId";
        public static final String UPDATE_BULKJOB_BY_ID = "UPDATE kruize_bulkjobs " +
                "SET experiments = jsonb_set(" +
                "    jsonb_set(experiments, :notificationPath, :newNotification::jsonb, true), " +
                "    :recommendationPath, :newRecommendation::jsonb, true" +
                ") " +
                "WHERE job_id = :jobId";
        public static final String UPDATE_EXPERIMENTS_DATE = "UPDATE kruize_experiments " +
                "SET updated_date = :updatedDate WHERE experiment_name IN (:experimentNames) ";
        public static final String SELECT_FROM_RESULTS = "from KruizeResultsEntry";
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME = "from KruizeResultsEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_DATASOURCE = "from KruizeDataSourceEntry";
        public static final String SELECT_FROM_DATASOURCE_BY_NAME = "from KruizeDataSourceEntry kd WHERE kd.name = :name";
        public static final String SELECT_FROM_METADATA_BY_DATASOURCE_NAME = "from KruizeDSMetadataEntry km WHERE km.datasource_name = :dataSourceName";
        public static final String SELECT_FROM_METADATA_BY_DATASOURCE_NAME_AND_CLUSTER_NAME =
                String.format("from KruizeDSMetadataEntry km " +
                                "WHERE km.datasource_name = :%s and " +
                                "km.cluster_name = :%s",
                        KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.DATASOURCE_NAME,
                        KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CLUSTER_NAME);
        public static final String SELECT_FROM_METADATA_BY_DATASOURCE_NAME_CLUSTER_NAME_AND_NAMESPACE =
                String.format("from KruizeDSMetadataEntry km " +
                                "WHERE km.datasource_name = :%s and " +
                                "km.cluster_name = :%s and " +
                                "km.namespace = :%s",
                        KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.DATASOURCE_NAME,
                        KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CLUSTER_NAME,
                        KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.NAMESPACE);
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME_AND_DATE_RANGE_AND_LIMIT =
                String.format("from KruizeResultsEntry k " +
                                "WHERE k.experiment_name = :%s and " +
                                "k.interval_end_time >= :%s and " +
                                "k.interval_end_time <= :%s ",
                        KruizeConstants.JSONKeys.EXPERIMENT_NAME,
                        KruizeConstants.JSONKeys.CALCULATED_START_TIME,
                        KruizeConstants.JSONKeys.INTERVAL_END_TIME);
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME_AND_START_END_TIME = String.format("from KruizeResultsEntry k " +
                        "WHERE k.experiment_name = :%s and k.interval_start_time >= :%s and " +
                        "k.interval_end_time <= :%s ",
                KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.INTERVAL_START_TIME, KruizeConstants.JSONKeys.INTERVAL_END_TIME);
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME_AND_END_TIME = String.format(
                "from KruizeResultsEntry k WHERE " +
                        "k.experiment_name = :%s " +
                        "and k.interval_end_time = :%s ",
                KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.INTERVAL_END_TIME);
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME_AND_MAX_END_TIME = String.format(
                "from KruizeResultsEntry k WHERE " +
                        "k.experiment_name = :%s and " +
                        "k.interval_end_time = (SELECT MAX(e.interval_end_time) FROM KruizeResultsEntry e  where e.experiment_name = :%s ) ",
                KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.EXPERIMENT_NAME);
        public static final String SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME = String.format("from KruizeRecommendationEntry k WHERE k.experiment_name = :experimentName");
        public static final String SELECT_FROM_LM_RECOMMENDATIONS_BY_EXP_NAME = String.format("from KruizeLMRecommendationEntry k WHERE k.experiment_name = :experimentName");
        public static final String SELECT_FROM_LM_RECOMMENDATIONS_BY_EXP_NAME_BY_JOB_ID =
                String.format(
                        "from KruizeLMRecommendationEntry k WHERE k.experiment_name = :experimentName " +
                                "AND function('jsonb_extract_path_text', extended_data, 'job_id') = :job_id");
        public static final String SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME_AND_END_TIME = String.format(
                "from KruizeRecommendationEntry k WHERE " +
                        "k.experiment_name = :%s and " +
                        "k.interval_end_time= :%s ",
                KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.INTERVAL_END_TIME);
        public static final String SELECT_FROM_LM_RECOMMENDATIONS_BY_EXP_NAME_AND_END_TIME = String.format(
                "from KruizeLMRecommendationEntry k WHERE " +
                        "k.experiment_name = :%s and " +
                        "k.interval_end_time= :%s ",
                KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.INTERVAL_END_TIME);
        public static final String SELECT_FROM_RECOMMENDATIONS = "from KruizeRecommendationEntry";
        public static final String SELECT_FROM_LM_RECOMMENDATIONS = "from KruizeLMRecommendationEntry";
        public static final String SELECT_FROM_LM_RECOMMENDATIONS_BY_JOB_ID = "from KruizeLMRecommendationEntry where function('jsonb_extract_path_text', extended_data, 'job_id') = :job_id";
        public static final String SELECT_FROM_PERFORMANCE_PROFILE = "from KruizePerformanceProfileEntry";
        public static final String SELECT_FROM_PERFORMANCE_PROFILE_BY_NAME = "from KruizePerformanceProfileEntry k WHERE k.name = :name";
        public static final String SELECT_FROM_METRIC_PROFILE = "from KruizeMetricProfileEntry";
        public static final String SELECT_FROM_METRIC_PROFILE_BY_NAME = "from KruizeMetricProfileEntry k WHERE k.name = :name";
        public static final String SELECT_FROM_METADATA_PROFILE = "from KruizeLMMetadataProfileEntry";
        public static final String SELECT_FROM_METADATA_PROFILE_BY_NAME = "from KruizeLMMetadataProfileEntry k WHERE k.name = :name";
        public static final String DELETE_FROM_EXPERIMENTS_BY_EXP_NAME = "DELETE FROM KruizeExperimentEntry k WHERE k.experiment_name = :experimentName";
        public static final String DELETE_FROM_LM_EXPERIMENTS_BY_EXP_NAME = "DELETE FROM KruizeLMExperimentEntry k WHERE k.experiment_name = :experimentName";
        public static final String DELETE_FROM_RESULTS_BY_EXP_NAME = "DELETE FROM KruizeResultsEntry k WHERE k.experiment_name = :experimentName";
        public static final String DELETE_FROM_RECOMMENDATIONS_BY_EXP_NAME = "DELETE FROM KruizeRecommendationEntry k WHERE k.experiment_name = :experimentName";
        public static final String DELETE_FROM_LM_RECOMMENDATIONS_BY_EXP_NAME = "DELETE FROM KruizeLMRecommendationEntry k WHERE k.experiment_name = :experimentName";
        public static final String DELETE_FROM_METADATA_BY_DATASOURCE_NAME = "DELETE FROM KruizeDSMetadataEntry km WHERE km.datasource_name = :dataSourceName";
        public static final String DELETE_FROM_METRIC_PROFILE_BY_PROFILE_NAME = "DELETE FROM KruizeMetricProfileEntry km WHERE km.name = :metricProfileName";
        public static final String DELETE_FROM_METADATA_PROFILE_BY_PROFILE_NAME = "DELETE FROM KruizeLMMetadataProfileEntry km WHERE km.name = :metadataProfileName";
        public static final String DB_PARTITION_DATERANGE = "CREATE TABLE IF NOT EXISTS %s_%s%s%s PARTITION OF %s FOR VALUES FROM ('%s-%s-%s 00:00:00.000') TO ('%s-%s-%s 23:59:59');";
        public static final String SELECT_ALL_KRUIZE_TABLES = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' " +
                "and (table_name like 'kruize_results_%' or table_name like 'kruize_recommendations_%') ";
        public static final String SELECT_FROM_EXPERIMENTS_BY_INPUT_JSON = "SELECT * FROM kruize_experiments WHERE cluster_name = :cluster_name " +
                "AND EXISTS (SELECT 1 FROM jsonb_array_elements(extended_data->'kubernetes_objects') AS kubernetes_object" +
                " WHERE kubernetes_object->>'name' = :name " +
                " AND kubernetes_object->>'namespace' = :namespace " +
                " AND kubernetes_object->>'type' = :type " +
                " AND EXISTS (SELECT 1 FROM jsonb_array_elements(kubernetes_object->'containers') AS container" +
                " WHERE container->>'container_name' = :container_name" +
                " AND container->>'container_image_name' = :container_image_name" +
                " ))";
        public static final String SELECT_FROM_LM_EXPERIMENTS_BY_INPUT_JSON = "SELECT * FROM kruize_lm_experiments WHERE cluster_name = :cluster_name " +
                "AND EXISTS (SELECT 1 FROM jsonb_array_elements(extended_data->'kubernetes_objects') AS kubernetes_object" +
                " WHERE kubernetes_object->>'name' = :name " +
                " AND kubernetes_object->>'namespace' = :namespace " +
                " AND kubernetes_object->>'type' = :type " +
                " AND EXISTS (SELECT 1 FROM jsonb_array_elements(kubernetes_object->'containers') AS container" +
                " WHERE container->>'container_name' = :container_name" +
                " AND container->>'container_image_name' = :container_image_name" +
                " ))";
    }

    public static final class TABLE_NAMES {
        public static final String KRUIZE_EXPERIMENTS = "kruize_experiments";
        public static final String KRUIZE_RESULTS = "kruize_results";
        public static final String KRUIZE_RECOMMENDATIONS = "kruize_recommendations";
        public static final String KRUIZE_LM_RECOMMENDATIONS = "kruize_lm_recommendations";
        public static final String KRUIZE_PERFORMANCE_PROFILES = "kruize_performance_profiles";

    }

    public static final class PARTITION_TYPES {
        public static final String BY_MONTH = "by_month";
        public static final String BY_15_DAYS = "by_fifteen_days";
        public static final String BY_DAY = "by_day";
        public static final int PARTITION_DAY = 25;
        public static final int LAST_N_DAYS = 15;
    }

    public static final class DB_MESSAGES {
        public static final String RECORD_ALREADY_EXISTS = "A record with the name %s already exists within the timestamp range starting from %s and ending on %s.";
        public static final String DUPLICATE_KEY = "duplicate key value";
        public static final String DUPLICATE_KEY_ALT = "A different object with the same identifier value was already associated with the session";
        public static final String NO_PARTITION_RELATION = "no partition of relation";
        public static final String CREATE_PARTITION_RETRY = "Create partition and retry !";
        public static final String INVALID_PARTITION_TYPE = "Invalid Partition Type";
        public static final String DATA_NOT_FOUND_KRUIZE_RESULTS = "Data not found in kruizeResultsEntry for exp_name : {} interval_end_time : {} ";
        public static final String ADD_CONSTRAINT = "add constraint";
        public static final String ADD_COLUMN = "add column";
        public static final String DB_CREATION_SUCCESS = "DB creation successful !";
        public static final String DB_LIVELINESS_PROBE_SUCCESS = "DB Liveliness probe connection successful!";
        public static final String DUPLICATE_DB_OPERATION = " - Attempted operation has already been executed";

    }


}
