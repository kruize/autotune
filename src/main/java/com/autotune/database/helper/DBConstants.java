package com.autotune.database.helper;

import com.autotune.utils.KruizeConstants;

public class DBConstants {

    public static final class SQLQUERY {
        public static final String SELECT_FROM_EXPERIMENTS = "from KruizeExperimentEntry";
        public static final String SELECT_FROM_EXPERIMENTS_BY_EXP_NAME = "from KruizeExperimentEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_RESULTS = "from KruizeResultsEntry";
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME = "from KruizeResultsEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME_AND_DATE_AND_LIMIT = String.format("from KruizeResultsEntry k " +
                        "WHERE k.cluster_name = :%s and k.experiment_name = :%s and" +
                        " k.interval_end_time <= :%s ORDER BY k.interval_end_time DESC",
                KruizeConstants.JSONKeys.CLUSTER_NAME, KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.INTERVAL_END_TIME);
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME_AND_DATE_RANGE_AND_LIMIT = String.format("from KruizeResultsEntry k " +
                        "WHERE  k.cluster_name = :%s and k.experiment_name = :%s and k.interval_end_time <= :%s " +
                        "ORDER BY k.interval_end_time DESC",
                KruizeConstants.JSONKeys.CLUSTER_NAME, KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.INTERVAL_END_TIME);
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME_AND_START_END_TIME = String.format("from KruizeResultsEntry k " +
                        "WHERE k.cluster_name = :%s and k.experiment_name = :%s and k.interval_start_time >= :%s and " +
                        "k.interval_end_time <= :%s",
                KruizeConstants.JSONKeys.CLUSTER_NAME, KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.INTERVAL_START_TIME, KruizeConstants.JSONKeys.INTERVAL_END_TIME);
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME_AND_END_TIME = String.format("from KruizeResultsEntry k WHERE k.cluster_name = :%s and k.experiment_name = :%s and k.interval_end_time = :%s", KruizeConstants.JSONKeys.CLUSTER_NAME, KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.INTERVAL_END_TIME);
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME_AND_MAX_END_TIME = String.format("from KruizeResultsEntry k WHERE k.cluster_name = :%s and k.experiment_name = :%s and k.interval_end_time = (SELECT MAX(e.interval_end_time) FROM KruizeResultsEntry e  where e.experiment_name = :%s )", KruizeConstants.JSONKeys.CLUSTER_NAME, KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.EXPERIMENT_NAME);
        public static final String SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME = String.format("from KruizeRecommendationEntry k WHERE k.experiment_name = :experimentName");
        public static final String SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME_AND_END_TIME = String.format("from KruizeRecommendationEntry k WHERE k.cluster_name= :%s and k.experiment_name = :%s and k.interval_end_time= :%s", KruizeConstants.JSONKeys.CLUSTER_NAME, KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.INTERVAL_END_TIME);
        public static final String SELECT_FROM_RECOMMENDATIONS = "from KruizeRecommendationEntry";
        public static final String SELECT_FROM_PERFORMANCE_PROFILE = "from KruizePerformanceProfileEntry";
        public static final String SELECT_FROM_PERFORMANCE_PROFILE_BY_NAME = "from KruizePerformanceProfileEntry k WHERE k.name = :name";
        public static final String DELETE_FROM_EXPERIMENTS_BY_EXP_NAME = "DELETE FROM KruizeExperimentEntry k WHERE k.experiment_name = :experimentName";
        public static final String DELETE_FROM_RESULTS_BY_EXP_NAME = "DELETE FROM KruizeResultsEntry k WHERE k.experiment_name = :experimentName";
        public static final String DELETE_FROM_RECOMMENDATIONS_BY_EXP_NAME = "DELETE FROM KruizeRecommendationEntry k WHERE k.experiment_name = :experimentName";
        public static final String DB_PARTITION_DATERANGE = "CREATE TABLE IF NOT EXISTS %s_%s%s%s PARTITION OF %s FOR VALUES FROM ('%s-%s-%s 00:00:00.000') TO ('%s-%s-%s 23:59:59');";
    }

    public static final class TABLE_NAMES {
        public static final String KRUIZE_EXPERIMENTS = "kruize_experiments";
        public static final String KRUIZE_RESULTS = "kruize_results";
        public static final String KRUIZE_RECOMMENDATIONS = "kruize_recommendations";
        public static final String KRUIZE_PERFORMANCE_PROFILES = "kruize_performance_profiles";

    }

    public static final class PARTITION_TYPES {
        public static final String BY_MONTH = "by_month";
        public static final String BY_15_DAYS = "by_fifteen_days";
        public static final String BY_DAY = "by_day";
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
        public static final String DB_CREATION_SUCCESS = "DB creation successful !";
        public static final String DB_LIVELINESS_PROBE_SUCCESS = "DB Liveliness probe connection successful!";

    }


}
