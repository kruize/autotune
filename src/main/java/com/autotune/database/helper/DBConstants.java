package com.autotune.database.helper;

public class DBConstants {

    public static final class SQLQUERY {
        public static final String SELECT_FROM_EXPERIMENTS = "from KruizeExperimentEntry";
        public static final String SELECT_FROM_EXPERIMENTS_BY_EXP_NAME = "from KruizeExperimentEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_EXPERIMENTS_BY_CLUSTER_AND_NS_NAME = "from KruizeExperimentEntry k WHERE k.cluster_name = :clusterName AND k.extended_data -> 'kubernetes_objects' @> '[{namespace: namespace}]'";
        public static final String SELECT_FROM_EXPERIMENTS_BY_NS_NAME = "from KruizeExperimentEntry k WHERE k.extended_data -> 'kubernetes_objects' @> '[{namespace: namespace}]'";
        public static final String SELECT_FROM_EXPERIMENTS_BY_CLUSTER_NAME = "from KruizeExperimentEntry k WHERE k.cluster_name = :clusterName";
        public static final String SELECT_FROM_RESULTS = "from KruizeResultsEntry";
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME = "from KruizeResultsEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME = "from KruizeRecommendationEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_RECOMMENDATIONS = "from KruizeRecommendationEntry";
        public static final String SELECT_FROM_PERFORMANCE_PROFILE = "from KruizePerformanceProfileEntry";
        public static final String SELECT_FROM_PERFORMANCE_PROFILE_BY_NAME = "from KruizePerformanceProfileEntry k WHERE k.name = :name";

        public static final String SELECT_DISTINCT_CLUSTER_NAMES_FROM_EXPERIMENTS = "SELECT DISTINCT cluster_name " + SELECT_FROM_EXPERIMENTS ;

        public static final String DELETE_FROM_EXPERIMENTS_BY_EXP_NAME = "DELETE FROM KruizeExperimentEntry k WHERE k.experiment_name = :experimentName";
        public static final String DELETE_FROM_RESULTS_BY_EXP_NAME = "DELETE FROM KruizeResultsEntry k WHERE k.experiment_name = :experimentName";
        public static final String DELETE_FROM_RECOMMENDATIONS_BY_EXP_NAME = "DELETE FROM KruizeRecommendationEntry k WHERE k.experiment_name = :experimentName";
    }
}
