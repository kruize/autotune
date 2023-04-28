package com.autotune.database.helper;

public class DBConstants {

    public static final class SQLQUERY {
        public static final String SELECT_FROM_EXPERIMENTS = "from KruizeExperimentEntry";
        public static final String SELECT_FROM_EXPERIMENTS_BY_EXP_NAME = "from KruizeExperimentEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_RESULTS = "from KruizeResultsEntry";
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME = "from KruizeResultsEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME = "from KruizeRecommendationEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_RECOMMENDATIONS = "from KruizeRecommendationEntry";
    }
}
