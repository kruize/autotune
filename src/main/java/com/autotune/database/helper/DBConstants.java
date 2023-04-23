package com.autotune.database.helper;

public class DBConstants {

    public static final class SQLQUERY {
        public static final String SELECT_FROM_EXPERIMENTS = "from KruizeExperimentEntry";
        public static final String SELECT_FROM_RESULTS = "from KruizeResultsEntry";
        public static final String SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME = "SELECT * FROM KruizeRecommendationEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_RECOMMENDATIONS = "from KruizeRecommendationEntry";
    }
}
