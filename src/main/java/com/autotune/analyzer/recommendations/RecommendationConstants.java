package com.autotune.analyzer.recommendations;

import com.autotune.analyzer.recommendations.subCategory.CostRecommendationSubCategory;
import com.autotune.analyzer.recommendations.subCategory.PerformanceRecommendationSubCategory;
import com.autotune.analyzer.recommendations.subCategory.RecommendationSubCategory;
import com.autotune.utils.KruizeConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.autotune.utils.KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.RecommendationDurationRanges.*;

public class RecommendationConstants {

    public enum RecommendationCategory {
        COST(
                KruizeConstants.JSONKeys.COST,
                new CostRecommendationSubCategory[]{
                        new CostRecommendationSubCategory(
                                KruizeConstants.JSONKeys.SHORT_TERM,
                                KruizeConstants.RecommendationEngineConstants
                                        .DurationBasedEngine.DurationAmount.SHORT_TERM_DURATION_DAYS,
                                TimeUnit.DAYS,
                                KruizeConstants.RecommendationEngineConstants
                                        .DurationBasedEngine.RecommendationDurationRanges
                                        .SHORT_TERM_TOTAL_DURATION_UPPER_BOUND_MINS,
                                KruizeConstants.RecommendationEngineConstants
                                        .DurationBasedEngine.RecommendationDurationRanges
                                        .SHORT_TERM_TOTAL_DURATION_LOWER_BOUND_MINS
                        ),
                        new CostRecommendationSubCategory(
                                KruizeConstants.JSONKeys.MEDIUM_TERM,
                                KruizeConstants.RecommendationEngineConstants
                                        .DurationBasedEngine.DurationAmount.MEDIUM_TERM_DURATION_DAYS,
                                TimeUnit.DAYS,
                                KruizeConstants.RecommendationEngineConstants
                                        .DurationBasedEngine.RecommendationDurationRanges
                                        .MEDIUM_TERM_TOTAL_DURATION_UPPER_BOUND_MINS,
                                KruizeConstants.RecommendationEngineConstants
                                        .DurationBasedEngine.RecommendationDurationRanges
                                        .MEDIUM_TERM_TOTAL_DURATION_LOWER_BOUND_MINS
                        ),
                        new CostRecommendationSubCategory(
                                KruizeConstants.JSONKeys.LONG_TERM,
                                KruizeConstants.RecommendationEngineConstants
                                        .DurationBasedEngine.DurationAmount.LONG_TERM_DURATION_DAYS,
                                TimeUnit.DAYS,
                                KruizeConstants.RecommendationEngineConstants
                                        .DurationBasedEngine.RecommendationDurationRanges
                                        .LONG_TERM_TOTAL_DURATION_UPPER_BOUND_MINS,
                                KruizeConstants.RecommendationEngineConstants
                                        .DurationBasedEngine.RecommendationDurationRanges
                                        .LONG_TERM_TOTAL_DURATION_LOWER_BOUND_MINS
                        ),
                }
        ),
        // Need to update with profile based sub categories
        PERFORMANCE(KruizeConstants.JSONKeys.PERFORMANCE, new PerformanceRecommendationSubCategory[]{
                new PerformanceRecommendationSubCategory(
                        KruizeConstants.JSONKeys.SHORT_TERM,
                        KruizeConstants.RecommendationEngineConstants
                                .DurationBasedEngine.DurationAmount.SHORT_TERM_DURATION_DAYS,
                        TimeUnit.DAYS,
                        KruizeConstants.RecommendationEngineConstants
                                .DurationBasedEngine.RecommendationDurationRanges
                                .SHORT_TERM_TOTAL_DURATION_UPPER_BOUND_MINS,
                        KruizeConstants.RecommendationEngineConstants
                                .DurationBasedEngine.RecommendationDurationRanges
                                .SHORT_TERM_TOTAL_DURATION_LOWER_BOUND_MINS
                ),
                new PerformanceRecommendationSubCategory(
                        KruizeConstants.JSONKeys.MEDIUM_TERM,
                        KruizeConstants.RecommendationEngineConstants
                                .DurationBasedEngine.DurationAmount.MEDIUM_TERM_DURATION_DAYS,
                        TimeUnit.DAYS,
                        KruizeConstants.RecommendationEngineConstants
                                .DurationBasedEngine.RecommendationDurationRanges
                                .MEDIUM_TERM_TOTAL_DURATION_UPPER_BOUND_MINS,
                        KruizeConstants.RecommendationEngineConstants
                                .DurationBasedEngine.RecommendationDurationRanges
                                .MEDIUM_TERM_TOTAL_DURATION_LOWER_BOUND_MINS
                ),
                new PerformanceRecommendationSubCategory(
                        KruizeConstants.JSONKeys.LONG_TERM,
                        KruizeConstants.RecommendationEngineConstants
                                .DurationBasedEngine.DurationAmount.LONG_TERM_DURATION_DAYS,
                        TimeUnit.DAYS,
                        KruizeConstants.RecommendationEngineConstants
                                .DurationBasedEngine.RecommendationDurationRanges
                                .LONG_TERM_TOTAL_DURATION_UPPER_BOUND_MINS,
                        KruizeConstants.RecommendationEngineConstants
                                .DurationBasedEngine.RecommendationDurationRanges
                                .LONG_TERM_TOTAL_DURATION_LOWER_BOUND_MINS
                ),
        });

        private String name;
        private RecommendationSubCategory[] recommendationSubCategories;

        private RecommendationCategory(String name, RecommendationSubCategory[] recommendationSubCategories) {
            this.name = name;
            this.recommendationSubCategories = recommendationSubCategories;
        }

        public String getName() {
            return this.name;
        }

        public RecommendationSubCategory[] getRecommendationSubCategories() {
            return this.recommendationSubCategories;
        }
    }

    public enum RecommendationNotificationTypes {
        INFO("info", 1),
        ERROR("error", 2),
        NOTICE("notice", 3),
        WARNING("warning", 4),
        CRITICAL("critical", 5);

        private String name;
        private int severity;

        private RecommendationNotificationTypes(String name, int severity) {
            this.name = name;
            this.severity = severity;
        }

        public String getName() {
            return name;
        }

        public int getSeverity() {
            return severity;
        }
    }

    public enum RecommendationTerms {
        SHORT_TERM(KruizeConstants.JSONKeys.SHORT_TERM, SHORT_TERM_HOURS, KruizeConstants.RecommendationEngineConstants
                .DurationBasedEngine.RecommendationDurationRanges
                .SHORT_TERM_TOTAL_DURATION_UPPER_BOUND_MINS,
                KruizeConstants.RecommendationEngineConstants
                        .DurationBasedEngine.RecommendationDurationRanges
                        .SHORT_TERM_TOTAL_DURATION_LOWER_BOUND_MINS),
        MEDIUM_TERM(KruizeConstants.JSONKeys.MEDIUM_TERM, MEDIUM_TERM_HOURS, KruizeConstants.RecommendationEngineConstants
                .DurationBasedEngine.RecommendationDurationRanges
                .MEDIUM_TERM_TOTAL_DURATION_UPPER_BOUND_MINS,
                KruizeConstants.RecommendationEngineConstants
                        .DurationBasedEngine.RecommendationDurationRanges
                        .MEDIUM_TERM_TOTAL_DURATION_LOWER_BOUND_MINS),
        LONG_TERM(KruizeConstants.JSONKeys.LONG_TERM, LONG_TERM_HOURS, KruizeConstants.RecommendationEngineConstants
                .DurationBasedEngine.RecommendationDurationRanges
                .LONG_TERM_TOTAL_DURATION_UPPER_BOUND_MINS,
                KruizeConstants.RecommendationEngineConstants
                        .DurationBasedEngine.RecommendationDurationRanges
                        .LONG_TERM_TOTAL_DURATION_LOWER_BOUND_MINS);

        private String value;
        private double durationInHrs;

        private double upperBound;
        private double lowerBound;

        private RecommendationTerms(String value, double durationInHrs, double upperBound, double lowerBound) {
            this.value = value;
            this.durationInHrs = durationInHrs;
            this.upperBound = upperBound;
            this.lowerBound = lowerBound;
        }

        public String getValue() {
            return value;
        }

        public double getDuration() {
            return durationInHrs;
        }

        // Setter for custom duration
        public void setDuration(int durationInHrs) {
            this.durationInHrs = durationInHrs;
        }

        public double getLowerBound() {
            return this.lowerBound;
        }

        public double getUpperBound() {
            return this.upperBound;
        }

        public static double getMaxDuration(RecommendationTerms termValue) {
            return switch (termValue) {
                case SHORT_TERM -> SHORT_TERM_HOURS;
                case MEDIUM_TERM -> MEDIUM_TERM_HOURS;
                case LONG_TERM -> LONG_TERM_HOURS;
            };
        }
    }

    public enum RecommendationNotification {
        INFO_RECOMMENDATIONS_AVAILABLE(
                RecommendationConstants.NotificationCodes.INFO_RECOMMENDATIONS_AVAILABLE,
                RecommendationConstants.RecommendationNotificationMsgConstant.RECOMMENDATIONS_AVAILABLE,
                RecommendationConstants.RecommendationNotificationTypes.INFO
        ),
        INFO_SHORT_TERM_RECOMMENDATIONS_AVAILABLE(
                RecommendationConstants.NotificationCodes.INFO_SHORT_TERM_RECOMMENDATIONS_AVAILABLE,
                RecommendationConstants.RecommendationNotificationMsgConstant.SHORT_TERM_RECOMMENDATIONS_AVAILABLE,
                RecommendationConstants.RecommendationNotificationTypes.INFO
        ),
        INFO_MEDIUM_TERM_RECOMMENDATIONS_AVAILABLE(
                RecommendationConstants.NotificationCodes.INFO_MEDIUM_TERM_RECOMMENDATIONS_AVAILABLE,
                RecommendationConstants.RecommendationNotificationMsgConstant.MEDIUM_TERM_RECOMMENDATIONS_AVAILABLE,
                RecommendationConstants.RecommendationNotificationTypes.INFO
        ),
        INFO_LONG_TERM_RECOMMENDATIONS_AVAILABLE(
                RecommendationConstants.NotificationCodes.INFO_LONG_TERM_RECOMMENDATIONS_AVAILABLE,
                RecommendationConstants.RecommendationNotificationMsgConstant.LONG_TERM_RECOMMENDATIONS_AVAILABLE,
                RecommendationConstants.RecommendationNotificationTypes.INFO
        ),
        INFO_COST_RECOMMENDATIONS_AVAILABLE(
                RecommendationConstants.NotificationCodes.INFO_COST_RECOMMENDATIONS_AVAILABLE,
                RecommendationConstants.RecommendationNotificationMsgConstant.COST_RECOMMENDATIONS_AVAILABLE,
                RecommendationConstants.RecommendationNotificationTypes.INFO
        ),
        INFO_PERFORMANCE_RECOMMENDATIONS_AVAILABLE(
                NotificationCodes.INFO_PERFORMANCE_RECOMMENDATIONS_AVAILABLE,
                RecommendationNotificationMsgConstant.PERFORMANCE_RECOMMENDATIONS_AVAILABLE,
                RecommendationNotificationTypes.INFO
        ),
        INFO_NOT_ENOUGH_DATA(
                RecommendationConstants.NotificationCodes.INFO_NOT_ENOUGH_DATA,
                RecommendationConstants.RecommendationNotificationMsgConstant.NOT_ENOUGH_DATA,
                RecommendationConstants.RecommendationNotificationTypes.INFO
        ),
        ERROR_AMOUNT_MISSING_IN_CPU_SECTION(
                RecommendationConstants.NotificationCodes.ERROR_AMOUNT_MISSING_IN_CPU_SECTION,
                RecommendationConstants.RecommendationNotificationMsgConstant.AMOUNT_MISSING_IN_CPU_SECTION,
                RecommendationConstants.RecommendationNotificationTypes.ERROR
        ),
        ERROR_INVALID_AMOUNT_IN_CPU_SECTION(
                RecommendationConstants.NotificationCodes.ERROR_INVALID_AMOUNT_IN_CPU_SECTION,
                RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_CPU_SECTION,
                RecommendationConstants.RecommendationNotificationTypes.ERROR
        ),
        ERROR_FORMAT_MISSING_IN_CPU_SECTION(
                RecommendationConstants.NotificationCodes.ERROR_FORMAT_MISSING_IN_CPU_SECTION,
                RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_CPU_SECTION,
                RecommendationConstants.RecommendationNotificationTypes.ERROR
        ),
        ERROR_INVALID_FORMAT_IN_CPU_SECTION(
                RecommendationConstants.NotificationCodes.ERROR_INVALID_FORMAT_IN_CPU_SECTION,
                RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_CPU_SECTION,
                RecommendationConstants.RecommendationNotificationTypes.ERROR
        ),
        ERROR_AMOUNT_MISSING_IN_MEMORY_SECTION(
                RecommendationConstants.NotificationCodes.ERROR_AMOUNT_MISSING_IN_MEMORY_SECTION,
                RecommendationConstants.RecommendationNotificationMsgConstant.AMOUNT_MISSING_IN_MEMORY_SECTION,
                RecommendationConstants.RecommendationNotificationTypes.ERROR
        ),
        ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION(
                RecommendationConstants.NotificationCodes.ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION,
                RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_MEMORY_SECTION,
                RecommendationConstants.RecommendationNotificationTypes.ERROR
        ),
        ERROR_FORMAT_MISSING_IN_MEMORY_SECTION(
                RecommendationConstants.NotificationCodes.ERROR_FORMAT_MISSING_IN_MEMORY_SECTION,
                RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_MEMORY_SECTION,
                RecommendationConstants.RecommendationNotificationTypes.ERROR
        ),
        ERROR_INVALID_FORMAT_IN_MEMORY_SECTION(
                RecommendationConstants.NotificationCodes.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION,
                RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_MEMORY_SECTION,
                RecommendationConstants.RecommendationNotificationTypes.ERROR
        ),
        ERROR_NUM_PODS_CANNOT_BE_ZERO(
                RecommendationConstants.NotificationCodes.ERROR_NUM_PODS_CANNOT_BE_ZERO,
                RecommendationConstants.RecommendationNotificationMsgConstant.NUM_PODS_CANNOT_BE_ZERO,
                RecommendationConstants.RecommendationNotificationTypes.ERROR
        ),
        ERROR_NUM_PODS_CANNOT_BE_NEGATIVE(
                RecommendationConstants.NotificationCodes.ERROR_NUM_PODS_CANNOT_BE_NEGATIVE,
                RecommendationConstants.RecommendationNotificationMsgConstant.NUM_PODS_CANNOT_BE_NEGATIVE,
                RecommendationConstants.RecommendationNotificationTypes.ERROR
        ),
        ERROR_HOURS_CANNOT_BE_ZERO(
                RecommendationConstants.NotificationCodes.ERROR_HOURS_CANNOT_BE_ZERO,
                RecommendationConstants.RecommendationNotificationMsgConstant.HOURS_CANNOT_BE_ZERO,
                RecommendationConstants.RecommendationNotificationTypes.ERROR
        ),
        ERROR_HOURS_CANNOT_BE_NEGATIVE(
                RecommendationConstants.NotificationCodes.ERROR_HOURS_CANNOT_BE_NEGATIVE,
                RecommendationConstants.RecommendationNotificationMsgConstant.HOURS_CANNOT_BE_NEGATIVE,
                RecommendationConstants.RecommendationNotificationTypes.ERROR
        ),
        NOTICE_CPU_RECORDS_ARE_IDLE(
                RecommendationConstants.NotificationCodes.NOTICE_CPU_RECORDS_ARE_IDLE,
                RecommendationConstants.RecommendationNotificationMsgConstant.CPU_RECORDS_ARE_IDLE,
                RecommendationConstants.RecommendationNotificationTypes.NOTICE
        ),
        NOTICE_CPU_RECORDS_ARE_ZERO(
                RecommendationConstants.NotificationCodes.NOTICE_CPU_RECORDS_ARE_ZERO,
                RecommendationConstants.RecommendationNotificationMsgConstant.CPU_RECORDS_ARE_ZERO,
                RecommendationConstants.RecommendationNotificationTypes.NOTICE
        ),
        NOTICE_CPU_RECORDS_NOT_AVAILABLE(
                RecommendationConstants.NotificationCodes.NOTICE_CPU_RECORDS_NOT_AVAILABLE,
                RecommendationConstants.RecommendationNotificationMsgConstant.CPU_RECORDS_NOT_AVAILABLE,
                RecommendationConstants.RecommendationNotificationTypes.NOTICE
        ),
        NOTICE_CPU_REQUESTS_OPTIMISED(
                RecommendationConstants.NotificationCodes.NOTICE_CPU_REQUESTS_OPTIMISED,
                RecommendationConstants.RecommendationNotificationMsgConstant.CPU_REQUESTS_OPTIMISED,
                RecommendationConstants.RecommendationNotificationTypes.NOTICE
        ),
        NOTICE_CPU_LIMITS_OPTIMISED(
                RecommendationConstants.NotificationCodes.NOTICE_CPU_LIMITS_OPTIMISED,
                RecommendationConstants.RecommendationNotificationMsgConstant.CPU_LIMITS_OPTIMISED,
                RecommendationConstants.RecommendationNotificationTypes.NOTICE
        ),
        NOTICE_MEMORY_RECORDS_ARE_ZERO(
                RecommendationConstants.NotificationCodes.NOTICE_MEMORY_RECORDS_ARE_ZERO,
                RecommendationConstants.RecommendationNotificationMsgConstant.MEMORY_RECORDS_ARE_ZERO,
                RecommendationConstants.RecommendationNotificationTypes.NOTICE
        ),
        NOTICE_MEMORY_RECORDS_NOT_AVAILABLE(
                RecommendationConstants.NotificationCodes.NOTICE_MEMORY_RECORDS_NOT_AVAILABLE,
                RecommendationConstants.RecommendationNotificationMsgConstant.MEMORY_RECORDS_NOT_AVAILABLE,
                RecommendationConstants.RecommendationNotificationTypes.NOTICE
        ),
        NOTICE_MEMORY_REQUESTS_OPTIMISED(
                RecommendationConstants.NotificationCodes.NOTICE_MEMORY_REQUESTS_OPTIMISED,
                RecommendationConstants.RecommendationNotificationMsgConstant.MEMORY_REQUESTS_OPTIMISED,
                RecommendationConstants.RecommendationNotificationTypes.NOTICE
        ),
        NOTICE_MEMORY_LIMITS_OPTIMISED(
                RecommendationConstants.NotificationCodes.NOTICE_MEMORY_LIMITS_OPTIMISED,
                RecommendationConstants.RecommendationNotificationMsgConstant.MEMORY_LIMITS_OPTIMISED,
                RecommendationConstants.RecommendationNotificationTypes.NOTICE
        ),
        CRITICAL_CPU_REQUEST_NOT_SET(
                RecommendationConstants.NotificationCodes.CRITICAL_CPU_REQUEST_NOT_SET,
                RecommendationConstants.RecommendationNotificationMsgConstant.CPU_REQUEST_NOT_SET,
                RecommendationConstants.RecommendationNotificationTypes.CRITICAL
        ),
        CRITICAL_MEMORY_REQUEST_NOT_SET(
                RecommendationConstants.NotificationCodes.CRITICAL_MEMORY_REQUEST_NOT_SET,
                RecommendationConstants.RecommendationNotificationMsgConstant.MEMORY_REQUEST_NOT_SET,
                RecommendationConstants.RecommendationNotificationTypes.CRITICAL
        ),
        WARNING_CPU_LIMIT_NOT_SET(
                RecommendationConstants.NotificationCodes.WARNING_CPU_LIMIT_NOT_SET,
                RecommendationConstants.RecommendationNotificationMsgConstant.CPU_LIMIT_NOT_SET,
                RecommendationConstants.RecommendationNotificationTypes.WARNING
        ),
        CRITICAL_MEMORY_LIMIT_NOT_SET(
                RecommendationConstants.NotificationCodes.CRITICAL_MEMORY_LIMIT_NOT_SET,
                RecommendationConstants.RecommendationNotificationMsgConstant.MEMORY_LIMIT_NOT_SET,
                RecommendationConstants.RecommendationNotificationTypes.CRITICAL
        );


        private int code;
        private String message;
        private RecommendationConstants.RecommendationNotificationTypes type;

        private RecommendationNotification(
                int code,
                String msg,
                RecommendationConstants.RecommendationNotificationTypes type
        ) {
            this.code = code;
            this.message = msg;
            this.type = type;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public RecommendationConstants.RecommendationNotificationTypes getType() {
            return type;
        }
    }

    public static final class NotificationCodes {
        public static final int SECTION_INFO_START = 100000;

        // Section - Info:                  100000 - 199999
        //      SubSection - General Info:  110000 - 119999
        //          SubSystem - General:    110000 - 112999 (30% of availability)
        //          SubSystem - Reserved:   113000 - 119999 (70% of availability)
        //      SubSection - Data:          120000 - 129999
        //          SubSystem - General:    120000 - 122999 (30% of availability)
        //          SubSystem - CPU:        123000 - 123999 (10% of availability)
        //          SubSystem - Memory      124000 - 124999 (10% of availability)
        //          SubSystem - Network     125000 - 125999 (10% of availability)
        //          SubSystem - Disk        126000 - 126999 (10% of availability)
        //          SubSystem - Power       127000 - 127999 (10% of availability)
        public static final int SECTION_INFO_END = 199999;
        public static final int SECTION_INFO_SUBSECTION_GENERAL_INFO_START = 110000;
        public static final int SECTION_INFO_SUBSECTION_GENERAL_INFO_END = 119999;
        public static final int SECTION_INFO_SUBSECTION_GENERAL_INFO_SUBSYSTEM_GENERAL_START = 110000;
        // Subsystem subsection: Default Engine
        // Range: 112000 - 112099
        // Subsystem subsection: Cost Engine
        // Range: 112100 - 112199
        public static final int COST_ENGINE_START = 112100;
        // Subsystem section: Recommendation Engines
        // Range: 112000 - 112999 (Each subsection can be given 100 entries which fit total of 10 entries or engines)
        public static final int INFO_RECOMMENDATIONS_AVAILABLE = 111000; // TODO: need to discuss the code
        public static final int INFO_SHORT_TERM_RECOMMENDATIONS_AVAILABLE = 111101; // TODO: need to discuss the code
        public static final int INFO_MEDIUM_TERM_RECOMMENDATIONS_AVAILABLE = 111102; // TODO: need to discuss the code
        public static final int INFO_LONG_TERM_RECOMMENDATIONS_AVAILABLE = 111103; // TODO: need to discuss the code;
        public static final int INFO_COST_RECOMMENDATIONS_AVAILABLE = 112101;
        public static final int INFO_PERFORMANCE_RECOMMENDATIONS_AVAILABLE = 112102;
        public static final int COST_ENGINE_END = 112199;
        public static final int SECTION_INFO_SUBSECTION_GENERAL_INFO_SUBSYSTEM_GENERAL_END = 112999;
        // Subsystem subsection: Profile Based Engine
        // Range: 112200 - 112299
        public static final int SECTION_INFO_SUBSECTION_GENERAL_INFO_SUBSYSTEM_RESERVED_START = 113000;
        public static final int SECTION_INFO_SUBSECTION_GENERAL_INFO_SUBSYSTEM_RESERVED_END = 119999;
        public static final int SECTION_INFO_SUBSECTION_DATA_START = 120000;
        public static final int SECTION_INFO_SUBSECTION_DATA_END = 129999;
        public static final int SECTION_INFO_SUBSECTION_DATA_SUBSYSTEM_GENERAL_START = 120000;
        public static final int INFO_NOT_ENOUGH_DATA = 120001;
        public static final int SECTION_INFO_SUBSECTION_DATA_SUBSYSTEM_GENERAL_END = 122999;
        public static final int SECTION_INFO_SUBSECTION_DATA_SUBSYSTEM_CPU_START = 123000;
        public static final int SECTION_INFO_SUBSECTION_DATA_SUBSYSTEM_CPU_END = 123999;
        public static final int SECTION_INFO_SUBSECTION_DATA_SUBSYSTEM_MEMORY_START = 124000;
        public static final int SECTION_INFO_SUBSECTION_DATA_SUBSYSTEM_MEMORY_END = 124999;
        public static final int SECTION_INFO_SUBSECTION_DATA_SUBSYSTEM_NETWORK_START = 125000;
        public static final int SECTION_INFO_SUBSECTION_DATA_SUBSYSTEM_NETWORK_END = 125999;
        public static final int SECTION_INFO_SUBSECTION_DATA_SUBSYSTEM_DISK_START = 126000;
        public static final int SECTION_INFO_SUBSECTION_DATA_SUBSYSTEM_DISK_END = 126999;
        public static final int SECTION_INFO_SUBSECTION_DATA_SUBSYSTEM_POWER_START = 127000;
        public static final int SECTION_INFO_SUBSECTION_DATA_SUBSYSTEM_POWER_END = 127999;
        public static final int SECTION_ERROR_START = 200000;

        // Section - Error:                 200000 - 299999
        //      SubSection - General Info:  210000 - 219999
        //          SubSystem - General:    210000 - 212999 (30% of availability)
        //          SubSystem - Reserved:   213000 - 219999 (70% of availability)
        //      SubSection - Data:          220000 - 229999
        //          SubSystem - General:    221000 - 222999 (30% of availability)
        //          SubSystem - CPU:        223000 - 223999 (10% of availability)
        //          SubSystem - Memory:     224000 - 224999 (10% of availability)
        //          SubSystem - Network:    225000 - 225999 (10% of availability)
        //          SubSystem - Disk:       226000 - 226999 (10% of availability)
        //          SubSystem - Power:      227000 - 227999 (10% of availability)
        public static final int SECTION_ERROR_END = 299999;
        public static final int SECTION_ERROR_SUBSECTION_GENERAL_INFO_START = 210000;
        public static final int SECTION_ERROR_SUBSECTION_GENERAL_INFO_END = 219999;
        public static final int SECTION_ERROR_SUBSECTION_GENERAL_INFO_SUBSYSTEM_GENERAL_START = 210000;
        public static final int SECTION_ERROR_SUBSECTION_GENERAL_INFO_SUBSYSTEM_GENERAL_END = 212999;
        public static final int SECTION_ERROR_SUBSECTION_GENERAL_INFO_SUBSYSTEM_RESERVED_START = 213000;
        public static final int SECTION_ERROR_SUBSECTION_GENERAL_INFO_SUBSYSTEM_RESERVED_END = 219999;
        public static final int SECTION_ERROR_SUBSECTION_DATA_START = 220000;
        public static final int SECTION_ERROR_SUBSECTION_DATA_END = 229999;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_GENERAL_START = 221000;
        public static final int ERROR_NUM_PODS_CANNOT_BE_ZERO = 221001;
        public static final int ERROR_NUM_PODS_CANNOT_BE_NEGATIVE = 221002;
        public static final int ERROR_HOURS_CANNOT_BE_ZERO = 221003;
        public static final int ERROR_HOURS_CANNOT_BE_NEGATIVE = 221004;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_GENERAL_END = 222999;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_CPU_START = 223000;
        public static final int ERROR_AMOUNT_MISSING_IN_CPU_SECTION = 223001;
        public static final int ERROR_INVALID_AMOUNT_IN_CPU_SECTION = 223002;
        public static final int ERROR_FORMAT_MISSING_IN_CPU_SECTION = 223003;
        public static final int ERROR_INVALID_FORMAT_IN_CPU_SECTION = 223004;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_CPU_END = 223999;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_MEMORY_START = 224000;
        public static final int ERROR_AMOUNT_MISSING_IN_MEMORY_SECTION = 224001;
        public static final int ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION = 224002;
        public static final int ERROR_FORMAT_MISSING_IN_MEMORY_SECTION = 224003;
        public static final int ERROR_INVALID_FORMAT_IN_MEMORY_SECTION = 224004;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_MEMORY_END = 224999;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_NETWORK_START = 225000;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_NETWORK_END = 225999;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_DISK_START = 226000;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_DISK_END = 226999;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_POWER_START = 227000;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_POWER_END = 227999;
        public static final int SECTION_NOTICE_START = 300000;

        // Section - Notice:                300000 - 399999
        //      SubSection - General Info:  310000 - 319999
        //          SubSystem - General:    310000 - 312999 (30% of availability)
        //          SubSystem - Reserved:   313000 - 319999 (70% of availability)
        //      SubSection - Data:          320000 - 329999
        //          SubSystem - General:    321000 - 322999 (30% of availability)
        //          SubSystem - CPU:        323000 - 323999 (10% of availability)
        //          SubSystem - Memory:     324000 - 324999 (10% of availability)
        //          SubSystem - Network:    325000 - 325999 (10% of availability)
        //          SubSystem - Disk:       326000 - 326999 (10% of availability)
        //          SubSystem - Power:      327000 - 327999 (10% of availability)
        public static final int SECTION_NOTICE_END = 399999;
        public static final int SECTION_NOTICE_SUBSECTION_GENERAL_INFO_START = 310000;
        public static final int SECTION_NOTICE_SUBSECTION_GENERAL_INFO_END = 319999;
        public static final int SECTION_NOTICE_SUBSECTION_GENERAL_INFO_SUBSYSTEM_GENERAL_START = 310000;
        public static final int SECTION_NOTICE_SUBSECTION_GENERAL_INFO_SUBSYSTEM_GENERAL_END = 312999;
        public static final int SECTION_NOTICE_SUBSECTION_GENERAL_INFO_SUBSYSTEM_RESERVED_START = 313000;
        public static final int SECTION_NOTICE_SUBSECTION_GENERAL_INFO_SUBSYSTEM_RESERVED_END = 319999;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_START = 320000;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_END = 329999;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_GENERAL_START = 321000;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_GENERAL_END = 322999;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_CPU_START = 323000;
        public static final int NOTICE_CPU_RECORDS_ARE_IDLE = 323001;
        public static final int NOTICE_CPU_RECORDS_ARE_ZERO = 323002;
        public static final int NOTICE_CPU_RECORDS_NOT_AVAILABLE = 323003;
        public static final int NOTICE_CPU_REQUESTS_OPTIMISED = 323004;
        public static final int NOTICE_CPU_LIMITS_OPTIMISED = 323005;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_CPU_END = 323999;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_MEMORY_START = 324000;
        public static final int NOTICE_MEMORY_RECORDS_ARE_ZERO = 324001;
        public static final int NOTICE_MEMORY_RECORDS_NOT_AVAILABLE = 324002;
        public static final int NOTICE_MEMORY_REQUESTS_OPTIMISED = 324003;
        public static final int NOTICE_MEMORY_LIMITS_OPTIMISED = 324004;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_MEMORY_END = 324999;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_NETWORK_START = 325000;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_NETWORK_END = 325999;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_DISK_START = 326000;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_DISK_END = 326999;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_POWER_START = 327000;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_POWER_END = 327999;
        public static final int SECTION_WARNING_START = 400000;

        // Section - Warning:               400000 - 499999
        //      SubSection - General Info:  410000 - 419999
        //          SubSystem - General:    410000 - 412999 (30% of availability)
        //          SubSystem - Reserve:    413000 - 419999 (70% of availability)
        //      SubSection - Data:          420000 - 429999
        //          SubSystem - General:    421000 - 422999 (30% of availability)
        //          SubSystem - CPU:        423000 - 423999 (10% of availability)
        //          SubSystem - Memory:     424000 - 424999 (10% of availability)
        //          SubSystem - Network:    425000 - 425999 (10% of availability)
        //          SubSystem - Disk:       426000 - 426999 (10% of availability)
        //          SubSystem - Power:      427000 - 427999 (10% of availability)
        public static final int SECTION_WARNING_END = 499999;
        public static final int SECTION_WARNING_SUBSECTION_GENERAL_INFO_START = 410000;
        public static final int SECTION_WARNING_SUBSECTION_GENERAL_INFO_END = 419999;
        public static final int SECTION_WARNING_SUBSECTION_GENERAL_INFO_SUBSYSTEM_GENERAL_START = 410000;
        public static final int SECTION_WARNING_SUBSECTION_GENERAL_INFO_SUBSYSTEM_GENERAL_END = 412999;
        public static final int SECTION_WARNING_SUBSECTION_GENERAL_INFO_SUBSYSTEM_RESERVED_START = 413000;
        public static final int SECTION_WARNING_SUBSECTION_GENERAL_INFO_SUBSYSTEM_RESERVED_END = 419999;
        public static final int SECTION_WARNING_SUBSECTION_DATA_START = 420000;
        public static final int SECTION_WARNING_SUBSECTION_DATA_END = 429999;
        public static final int SECTION_WARNING_SUBSECTION_DATA_SUBSYSTEM_GENERAL_START = 421000;
        public static final int SECTION_WARNING_SUBSECTION_DATA_SUBSYSTEM_GENERAL_END = 422999;
        public static final int SECTION_WARNING_SUBSECTION_DATA_SUBSYSTEM_CPU_START = 423000;
        public static final int WARNING_CPU_LIMIT_NOT_SET = 423001;
        public static final int SECTION_WARNING_SUBSECTION_DATA_SUBSYSTEM_CPU_END = 423999;
        public static final int SECTION_WARNING_SUBSECTION_DATA_SUBSYSTEM_MEMORY_START = 424000;
        public static final int SECTION_WARNING_SUBSECTION_DATA_SUBSYSTEM_MEMORY_END = 424999;
        public static final int SECTION_WARNING_SUBSECTION_DATA_SUBSYSTEM_NETWORK_START = 425000;
        public static final int SECTION_WARNING_SUBSECTION_DATA_SUBSYSTEM_NETWORK_END = 425999;
        public static final int SECTION_WARNING_SUBSECTION_DATA_SUBSYSTEM_DISK_START = 426000;
        public static final int SECTION_WARNING_SUBSECTION_DATA_SUBSYSTEM_DISK_END = 426999;
        public static final int SECTION_WARNING_SUBSECTION_DATA_SUBSYSTEM_POWER_START = 427000;
        public static final int SECTION_WARNING_SUBSECTION_DATA_SUBSYSTEM_POWER_END = 427999;
        public static final int SECTION_CRITICAL_START = 500000;

        // Section - Critical:              500000 - 599999
        //      SubSection - General Info:  510000 - 519999
        //          SubSystem - General:    510000 - 512999 (30% of availability)
        //          SubSystem - Reserve:    513000 - 519999 (70% of availability)
        //      SubSection - Data:          520000 - 529999
        //          SubSystem - General:    521000 - 522999 (30% of availability)
        //          SubSystem - CPU:        523000 - 523999 (10% of availability)
        //          SubSystem - Memory:     524000 - 524999 (10% of availability)
        //          SubSystem - Network:    525000 - 525999 (10% of availability)
        //          SubSystem - Disk:       526000 - 526999 (10% of availability)
        //          SubSystem - Power:      527000 - 527999 (10% of availability)
        public static final int SECTION_CRITICAL_END = 599999;
        public static final int SECTION_CRITICAL_SUBSECTION_GENERAL_INFO_START = 510000;
        public static final int SECTION_CRITICAL_SUBSECTION_GENERAL_INFO_END = 519999;
        public static final int SECTION_CRITICAL_SUBSECTION_GENERAL_INFO_SUBSYSTEM_GENERAL_START = 510000;
        public static final int SECTION_CRITICAL_SUBSECTION_GENERAL_INFO_SUBSYSTEM_GENERAL_END = 512999;
        public static final int SECTION_CRITICAL_SUBSECTION_GENERAL_INFO_SUBSYSTEM_RESERVED_START = 513000;
        public static final int SECTION_CRITICAL_SUBSECTION_GENERAL_INFO_SUBSYSTEM_RESERVED_END = 519999;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_START = 520000;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_END = 529999;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_SUBSYSTEM_GENERAL_START = 521000;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_SUBSYSTEM_GENERAL_END = 522999;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_SUBSYSTEM_CPU_START = 523000;
        public static final int CRITICAL_CPU_REQUEST_NOT_SET = 523001;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_SUBSYSTEM_CPU_END = 523999;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_SUBSYSTEM_MEMORY_START = 524000;
        public static final int CRITICAL_MEMORY_REQUEST_NOT_SET = 524001;
        public static final int CRITICAL_MEMORY_LIMIT_NOT_SET = 524002;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_SUBSYSTEM_MEMORY_END = 524999;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_SUBSYSTEM_NETWORK_START = 525000;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_SUBSYSTEM_NETWORK_END = 525999;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_SUBSYSTEM_DISK_START = 526000;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_SUBSYSTEM_DISK_END = 526999;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_SUBSYSTEM_POWER_START = 527000;
        public static final int SECTION_CRITICAL_SUBSECTION_DATA_SUBSYSTEM_POWER_END = 527999;
        public static final HashMap<Integer, List<Integer>> CONTRADICTING_MAP = new HashMap<>();

        // Add contents in static block instead of initialising every var as static
        static {
            // Contradicting Codes for NOT_ENOUGH_DATA
            Integer[] CODES_CONTRADICT_NOT_ENOUGH_DATA = {
                    // Add things which contradict
                    // Currently it's added by default so no contradicting codes will be added here
            };

            CONTRADICTING_MAP.put(INFO_NOT_ENOUGH_DATA, Arrays.asList(CODES_CONTRADICT_NOT_ENOUGH_DATA));

            // Contradicting Codes for COST_RECOMMENDATIONS_AVAILABLE
            Integer[] CODES_CONTRADICT_COST_RECOMMENDATIONS_AVAILABLE = {
                    INFO_NOT_ENOUGH_DATA
            };

            CONTRADICTING_MAP.put(
                    INFO_COST_RECOMMENDATIONS_AVAILABLE,
                    Arrays.asList(CODES_CONTRADICT_COST_RECOMMENDATIONS_AVAILABLE)
            );

            // Contradicting Codes for CPU_RECORDS_ARE_IDLE
            Integer[] CODES_CONTRADICT_CPU_RECORDS_ARE_IDLE = {
                    INFO_NOT_ENOUGH_DATA
            };

            CONTRADICTING_MAP.put(
                    NOTICE_CPU_RECORDS_ARE_IDLE,
                    Arrays.asList(CODES_CONTRADICT_CPU_RECORDS_ARE_IDLE)
            );

            // Contradicting Codes for CPU_RECORDS_ARE_ZERO
            Integer[] CODES_CONTRADICT_CPU_RECORDS_ARE_ZERO = {
                    INFO_NOT_ENOUGH_DATA
            };

            CONTRADICTING_MAP.put(
                    NOTICE_CPU_RECORDS_ARE_ZERO,
                    Arrays.asList(CODES_CONTRADICT_CPU_RECORDS_ARE_ZERO)
            );

            // Contradicting Codes for CPU_RECORDS_ARE_MISSING
            Integer[] CODES_CONTRADICT_CPU_RECORDS_ARE_MISSING = {
                    INFO_NOT_ENOUGH_DATA
            };

            CONTRADICTING_MAP.put(
                    NOTICE_CPU_RECORDS_NOT_AVAILABLE,
                    Arrays.asList(CODES_CONTRADICT_CPU_RECORDS_ARE_MISSING)
            );

            // Contradicting Codes for MEMORY_RECORDS_ARE_ZERO -> 1005
            Integer[] CODES_CONTRADICT_MEMORY_RECORDS_ARE_ZERO = {
                    INFO_NOT_ENOUGH_DATA
            };

            CONTRADICTING_MAP.put(
                    NOTICE_MEMORY_RECORDS_ARE_ZERO,
                    Arrays.asList(CODES_CONTRADICT_MEMORY_RECORDS_ARE_ZERO)
            );


            // Contradicting Codes for MEMORY_RECORDS_ARE_MISSING
            Integer[] CODES_CONTRADICT_MEMORY_RECORDS_ARE_MISSING = {
                    INFO_NOT_ENOUGH_DATA
            };

            CONTRADICTING_MAP.put(
                    NOTICE_MEMORY_RECORDS_NOT_AVAILABLE,
                    Arrays.asList(CODES_CONTRADICT_MEMORY_RECORDS_ARE_MISSING)
            );

            // Contradicting Codes for CPU_REQUEST_NOT_SET
            Integer[] CODES_CONTRADICT_CPU_REQUEST_NOT_SET = {
                    INFO_NOT_ENOUGH_DATA
            };

            CONTRADICTING_MAP.put(
                    CRITICAL_CPU_REQUEST_NOT_SET,
                    Arrays.asList(CODES_CONTRADICT_CPU_REQUEST_NOT_SET)
            );

            // Contradicting Codes for MEMORY_REQUEST_NOT_SET
            Integer[] CODES_CONTRADICT_MEMORY_REQUEST_NOT_SET = {
                    INFO_NOT_ENOUGH_DATA
            };

            CONTRADICTING_MAP.put(
                    CRITICAL_MEMORY_REQUEST_NOT_SET,
                    Arrays.asList(CODES_CONTRADICT_MEMORY_REQUEST_NOT_SET)
            );

            // Contradicting Codes for CPU_LIMIT_NOT_SET
            Integer[] CODES_CONTRADICT_CPU_LIMIT_NOT_SET = {
                    INFO_NOT_ENOUGH_DATA
            };

            CONTRADICTING_MAP.put(
                    WARNING_CPU_LIMIT_NOT_SET,
                    Arrays.asList(CODES_CONTRADICT_CPU_LIMIT_NOT_SET)
            );

            // Contradicting Codes for MEMORY_LIMIT_NOT_SET
            Integer[] CODES_CONTRADICT_MEMORY_LIMIT_NOT_SET = {
                    INFO_NOT_ENOUGH_DATA
            };

            CONTRADICTING_MAP.put(
                    CRITICAL_MEMORY_LIMIT_NOT_SET,
                    Arrays.asList(CODES_CONTRADICT_MEMORY_LIMIT_NOT_SET)
            );
        }

        private NotificationCodes() {
        }
    }

    public static final class RecommendationValueConstants {
        public static final Double ZERO_VALUE = 0.0;
        public static final Double MEM_ZERO = ZERO_VALUE;
        public static final Double CPU_ZERO = ZERO_VALUE;
        public static final Double CPU_ONE_MILLICORE = 0.001;
        public static final Double CPU_TEN_MILLICORE = 0.01;
        public static final Double CPU_HUNDRED_MILLICORE = 0.1;
        public static final Double CPU_FIVE_HUNDRED_MILLICORE = 0.5;
        public static final Double CPU_ONE_CORE = 1.0;
        public static final Double MEM_USAGE_BUFFER_DECIMAL = 0.2;
        public static final Double MEM_SPIKE_BUFFER_DECIMAL = 0.05;
        public static final Double DEFAULT_CPU_THRESHOLD = 0.1;
        public static final Double DEFAULT_MEMORY_THRESHOLD = 0.1;

        public static final int THRESHOLD_HRS_SHORT_TERM = 6;
        public static final int THRESHOLD_HRS_MEDIUM_TERM = 6;
        public static final int THRESHOLD_HRS_LONG_TERM = 6;
    }

    public static final class RecommendationNotificationMsgConstant {
        public static final String NOT_ENOUGH_DATA = "There is not enough data available to generate a recommendation.";
        public static final String COST_RECOMMENDATIONS_AVAILABLE = "Cost Recommendations Available";
        public static final String PERFORMANCE_RECOMMENDATIONS_AVAILABLE = "Performance Recommendations Available";
        public static final String RECOMMENDATIONS_AVAILABLE = "Recommendations Are Available";
        public static final String SHORT_TERM_RECOMMENDATIONS_AVAILABLE = "Short Term Recommendations Available";
        public static final String MEDIUM_TERM_RECOMMENDATIONS_AVAILABLE = "Medium Term Recommendations Available";
        public static final String LONG_TERM_RECOMMENDATIONS_AVAILABLE = "Long Term Recommendations Available";
        public static final String CPU_RECORDS_ARE_IDLE = "CPU Usage is less than a millicore, No CPU Recommendations can be generated";
        public static final String CPU_RECORDS_ARE_ZERO = "CPU usage is zero, No CPU Recommendations can be generated";
        public static final String MEMORY_RECORDS_ARE_ZERO = "Memory Usage is zero, No Memory Recommendations can be generated";
        public static final String CPU_REQUEST_NOT_SET = "CPU Request Not Set";
        public static final String MEMORY_REQUEST_NOT_SET = "Memory Request Not Set";
        public static final String MEMORY_LIMIT_NOT_SET = "Memory Limit Not Set";
        public static final String CPU_LIMIT_NOT_SET = "CPU Limit Not Set";
        public static final String CPU_RECORDS_NOT_AVAILABLE = "CPU metrics are not available, No CPU Recommendations can be generated";
        public static final String MEMORY_RECORDS_NOT_AVAILABLE = "Memory metrics are not available, No Memory Recommendations can be generated";
        public static final String AMOUNT_MISSING_IN_CPU_SECTION = "Amount field is missing in the CPU Section";
        public static final String INVALID_AMOUNT_IN_CPU_SECTION = "Invalid Amount in CPU Section";
        public static final String FORMAT_MISSING_IN_CPU_SECTION = "Format field is missing in CPU Section";
        public static final String INVALID_FORMAT_IN_CPU_SECTION = "Invalid Format in CPU Section";
        public static final String AMOUNT_MISSING_IN_MEMORY_SECTION = "Amount field is missing in the Memory Section";
        public static final String INVALID_AMOUNT_IN_MEMORY_SECTION = "Invalid Amount in Memory Section";
        public static final String FORMAT_MISSING_IN_MEMORY_SECTION = "Format field is missing in Memory Section";
        public static final String INVALID_FORMAT_IN_MEMORY_SECTION = "Invalid Format in Memory Section";
        public static final String NUM_PODS_CANNOT_BE_ZERO = "Number of pods cannot be zero";
        public static final String NUM_PODS_CANNOT_BE_NEGATIVE = "Number of pods cannot be negative";
        public static final String HOURS_CANNOT_BE_ZERO = "Duration hours cannot be zero";
        public static final String HOURS_CANNOT_BE_NEGATIVE = "Duration hours cannot be negative";
        public static final String CPU_REQUESTS_OPTIMISED = "Workload is optimised wrt CPU REQUESTS, no changes needed";
        public static final String CPU_LIMITS_OPTIMISED = "Workload is optimised wrt CPU LIMITS, no changes needed";
        public static final String MEMORY_REQUESTS_OPTIMISED = "Workload is optimised wrt MEMORY REQUESTS, no changes needed";
        public static final String MEMORY_LIMITS_OPTIMISED = "Workload is optimised wrt MEMORY LIMITS, no changes needed";

        private RecommendationNotificationMsgConstant() {

        }
    }

    public static class RecommendationEngine {
        private RecommendationEngine() {

        }

        public static class EngineNames {
            public static String DEFAULT_NAME = "default";
            public static String COST = "cost";
            public static String PERFORMANCE = "performance";

            private EngineNames() {

            }
        }

        public static class EngineKeys {
            public static String COST_KEY = "cost";
            public static String PERFORMANCE_BASED_KEY = "performance";

            private EngineKeys() {

            }
        }

        public static class MinConstants {
            private MinConstants() {

            }

            public static class CPU {
                public static final double CPU_MIN_RECOMMENDATION_VALUE = 0.1;

                private CPU() {

                }
            }
        }

        public static class InternalConstants {
            public static final String CURRENT_CPU_REQUEST = "CURRENT_CPU_REQUEST";
            public static final String CURRENT_MEMORY_REQUEST = "CURRENT_MEMORY_REQUEST";
            public static final String CURRENT_CPU_LIMIT = "CURRENT_CPU_LIMIT";
            public static final String CURRENT_MEMORY_LIMIT = "CURRENT_MEMORY_LIMIT";
            public static final String RECOMMENDED_CPU_REQUEST = "RECOMMENDED_CPU_REQUEST";
            public static final String RECOMMENDED_MEMORY_REQUEST = "RECOMMENDED_MEMORY_REQUEST";
            public static final String RECOMMENDED_CPU_LIMIT = "RECOMMENDED_CPU_LIMIT";
            public static final String RECOMMENDED_MEMORY_LIMIT = "RECOMMENDED_MEMORY_LIMIT";

            private InternalConstants() {

            }
        }

        public static class PercentileConstants {
            public static final Integer COST_CPU_PERCENTILE = 60;
            public static final Integer COST_MEMORY_PERCENTILE = 100;
            public static final Integer PERFORMANCE_CPU_PERCENTILE = 98;
            public static final Integer PERFORMANCE_MEMORY_PERCENTILE = 100;

        }
    }
}
