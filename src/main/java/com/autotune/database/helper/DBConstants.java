package com.autotune.database.helper;

import com.autotune.utils.KruizeConstants;

public class DBConstants {

    public static final class SQLQUERY {
        public static final String SELECT_FROM_EXPERIMENTS = "from KruizeExperimentEntry";
        public static final String SELECT_FROM_EXPERIMENTS_BY_EXP_NAME = "from KruizeExperimentEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_RESULTS = "from KruizeResultsEntry";
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME = "from KruizeResultsEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME_AND_DATE_RANGE = String.format("from KruizeResultsEntry k WHERE k.experiment_name = :%s and k.interval_end_time <= :%s ORDER BY k.interval_end_time DESC", KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.INTERVAL_END_TIME, KruizeConstants.JSONKeys.INTERVAL_START_TIME);
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME_AND_END_TIME = String.format("from KruizeResultsEntry k WHERE k.experiment_name = :%s and k.interval_end_time = :%s", KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.INTERVAL_END_TIME);
<<<<<<< HEAD
=======

>>>>>>> 782932c0 ( Removed end_date from mandatory feilds)
        public static final String SELECT_FROM_RESULTS_BY_EXP_NAME_AND_MAX_END_TIME = String.format("from KruizeResultsEntry k WHERE k.experiment_name = :%s and k.interval_end_time = (SELECT MAX(e.interval_end_time) FROM KruizeResultsEntry e  where e.experiment_name = :%s )", KruizeConstants.JSONKeys.EXPERIMENT_NAME , KruizeConstants.JSONKeys.EXPERIMENT_NAME);
        public static final String SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME = "from KruizeRecommendationEntry k WHERE k.experiment_name = :experimentName";
        public static final String SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME_AND_END_TIME = String.format("from KruizeRecommendationEntry k WHERE k.experiment_name = :%s and k.interval_end_time= :%s", KruizeConstants.JSONKeys.EXPERIMENT_NAME, KruizeConstants.JSONKeys.INTERVAL_END_TIME);
        public static final String SELECT_FROM_RECOMMENDATIONS = "from KruizeRecommendationEntry";
        public static final String SELECT_FROM_PERFORMANCE_PROFILE = "from KruizePerformanceProfileEntry";
        public static final String SELECT_FROM_PERFORMANCE_PROFILE_BY_NAME = "from KruizePerformanceProfileEntry k WHERE k.name = :name";


        public static final String DELETE_FROM_EXPERIMENTS_BY_EXP_NAME = "DELETE FROM KruizeExperimentEntry k WHERE k.experiment_name = :experimentName";
        public static final String DELETE_FROM_RESULTS_BY_EXP_NAME = "DELETE FROM KruizeResultsEntry k WHERE k.experiment_name = :experimentName";
        public static final String DELETE_FROM_RECOMMENDATIONS_BY_EXP_NAME = "DELETE FROM KruizeRecommendationEntry k WHERE k.experiment_name = :experimentName";
    }
}
