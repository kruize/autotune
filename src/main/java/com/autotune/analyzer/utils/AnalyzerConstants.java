/*******************************************************************************
 * Copyright (c) 2020, 2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.analyzer.utils;

import com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface.DefaultImpl;
import com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface.ResourceOptimizationOpenshiftImpl;
import com.autotune.analyzer.recommendations.subCategory.DurationBasedRecommendationSubCategory;
import com.autotune.analyzer.recommendations.subCategory.RecommendationSubCategory;
import com.autotune.utils.KruizeConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Holds constants used in other parts of the codebase
 */
public class AnalyzerConstants {
    public static final String MODE = "mode";
    public static final String TARGET_CLUSTER = "target_cluster";
    public static final String MONITOR = "monitor";
    public static final String EXPERIMENT = "experiment";
    public static final String LOCAL = "local";
    public static final String REMOTE = "remote";


    // Used to parse autotune configmaps


    public static final String PROMETHEUS_DATA_SOURCE = "prometheus";
    public static final String PROMETHEUS_API = "/api/v1/query?query=";
    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";
    // Used in Configuration for accessing the autotune objects from kubernetes
    public static final String SCOPE = "Namespaced";
    public static final String GROUP = "recommender.com";
    public static final String API_VERSION_V1 = "v1";
    public static final String POD_TEMPLATE_HASH = "pod-template-hash";
    public static final String AUTOTUNE_PLURALS = "autotunes";

    public static final String AUTOTUNE_RESOURCE_NAME = AUTOTUNE_PLURALS + GROUP;
    public static final String DEFAULT_K8S_TYPE = "openshift";
    public static final String PROFILE_VERSION = "profile_version";
    public static final Double DEFAULT_PROFILE_VERSION = 1.0;
    public static final String AGGREGATION_FUNCTIONS = "aggregation_functions";
    public static final String FUNCTION = "function";
    public static final String VERSIONS = "versions";
    public static final String KUBERNETES_OBJECT = "kubernetes_object";
    public static final String KUBERNETES_OBJECTS = "kubernetes_objects";
    public static final String AUTOTUNE_CONFIG_PLURALS = "autotuneconfigs";
    public static final String AUTOTUNE_CONFIG_RESOURCE_NAME = AUTOTUNE_CONFIG_PLURALS + GROUP;
    public static final String AUTOTUNE_VARIABLE_PLURALS = "autotunequeryvariables";
    public static final String AUTOTUNE_VARIABLE_RESOURCE_NAME = AUTOTUNE_VARIABLE_PLURALS + GROUP;
    public static final String PRESENCE_ALWAYS = "always";
    public static final String NONE = "none";
    public static final String POD_VARIABLE = "$POD$";
    public static final String NAMESPACE_VARIABLE = "$NAMESPACE$";
    public static final String API_VERSION = "apiVersion";
    public static final String KIND = "kind";
    public static final String RESOURCE_VERSION = "resourceVersion";
    public static final String UID = "uid";
    public static final String REASON_NORMAL = "Normal";
    public static final String AUTOTUNE = "Autotune";
    public static final String EXPERIMENT_MAP = "MainExperimentsMAP";
    public static final String NAME = "experimentName";
    public static final String SLO = "sloInfo";
    public static final String NAMESPACE = "namespace";
    public static final String RECOMMENDATION_SETTINGS = "recommendation_settings";
    public static final String DEPLOYMENT_NAME = "name";
    public static final String SELECTOR = "selectorInfo";
    public static final String NULL = "null";
    public static final String BULKUPLOAD_CREATEEXPERIMENT_LIMIT = "bulkupload_createexperiment_limit";
    public static final String PERSISTANCE_STORAGE = "persistance_storage";
    public static final String RESULTS_COUNT = "results_count";
    public static final int GC_THRESHOLD_COUNT = 100;


    private AnalyzerConstants() {
    }

    public enum MODEType {
        MONITORING,
        EXPERIMENT;

    }

    public enum TargetType {
        LOCAL,
        REMOTE;

    }

    public enum ExperimentStatus {
        QUEUED,
        IN_PROGRESS,
        STALE,
        PAUSE,
        RESUME,
        DELETE,
        COMPLETED,
        FAILED;
    }

    public enum RecommendationItem {
        cpu,
        memory
    }

    public enum CapacityMax {
        capacity,
        max
    }

    public enum ResourceSetting {
        requests,
        limits
    }

    public enum PersistenceType {
        LOCAL,              //Store only local  , Default
        HYBRID,             //Store data both in db and local
        DB                  //Store only DB
    }

    public enum RecommendationSection {
        CURRENT_CONFIG(KruizeConstants.JSONKeys.CURRENT),
        RECOMMENDATION_CONFIG(KruizeConstants.JSONKeys.CONFIG),
        VARIATION(KruizeConstants.JSONKeys.VARIATION);

        private String name;

        private RecommendationSection(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public enum RecommendationCategory {
        DURATION_BASED(
                KruizeConstants.JSONKeys.DURATION_BASED,
                new DurationBasedRecommendationSubCategory[]{
                        new DurationBasedRecommendationSubCategory(
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
                        new DurationBasedRecommendationSubCategory(
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
                        new DurationBasedRecommendationSubCategory(
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
        PROFILE_BASED(KruizeConstants.JSONKeys.PROFILE_BASED, null);

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

    public enum RecommendationNotification {
        DURATION_BASED_RECOMMENDATIONS_AVAILABLE (
                NotificationCodes.INFO_DURATION_BASED_RECOMMENDATIONS_AVAILABLE,
                RecommendationNotificationMsgConstant.DURATION_BASED_RECOMMENDATIONS_AVAILABLE,
                RecommendationNotificationTypes.INFO
        ),
        NOT_ENOUGH_DATA (
                NotificationCodes.INFO_NOT_ENOUGH_DATA,
                RecommendationNotificationMsgConstant.NOT_ENOUGH_DATA,
                RecommendationNotificationTypes.INFO
        ),
        AMOUNT_MISSING_IN_CPU_SECTION(
                NotificationCodes.AMOUNT_MISSING_IN_CPU_SECTION,
                RecommendationNotificationMsgConstant.AMOUNT_MISSING_IN_CPU_SECTION,
                RecommendationNotificationTypes.ERROR
        ),
        INVALID_AMOUNT_IN_CPU_SECTION(
                NotificationCodes.INVALID_AMOUNT_IN_CPU_SECTION,
                RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_CPU_SECTION,
                RecommendationNotificationTypes.ERROR
        ),
        FORMAT_MISSING_IN_CPU_SECTION(
                NotificationCodes.FORMAT_MISSING_IN_CPU_SECTION,
                RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_CPU_SECTION,
                RecommendationNotificationTypes.ERROR
        ),
        INVALID_FORMAT_IN_CPU_SECTION(
                NotificationCodes.INVALID_FORMAT_IN_CPU_SECTION,
                RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_CPU_SECTION,
                RecommendationNotificationTypes.ERROR
        ),
        AMOUNT_MISSING_IN_MEMORY_SECTION(
                NotificationCodes.AMOUNT_MISSING_IN_MEMORY_SECTION,
                RecommendationNotificationMsgConstant.AMOUNT_MISSING_IN_MEMORY_SECTION,
                RecommendationNotificationTypes.ERROR
        ),
        INVALID_AMOUNT_IN_MEMORY_SECTION(
                NotificationCodes.INVALID_AMOUNT_IN_MEMORY_SECTION,
                RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_MEMORY_SECTION,
                RecommendationNotificationTypes.ERROR
        ),
        FORMAT_MISSING_IN_MEMORY_SECTION(
                NotificationCodes.FORMAT_MISSING_IN_MEMORY_SECTION,
                RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_MEMORY_SECTION,
                RecommendationNotificationTypes.ERROR
        ),
        INVALID_FORMAT_IN_MEMORY_SECTION(
                NotificationCodes.INVALID_FORMAT_IN_MEMORY_SECTION,
                RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_MEMORY_SECTION,
                RecommendationNotificationTypes.ERROR
        ),
        NUM_PODS_CANNOT_BE_ZERO(
                NotificationCodes.NUM_PODS_CANNOT_BE_ZERO,
                RecommendationNotificationMsgConstant.NUM_PODS_CANNOT_BE_ZERO,
                RecommendationNotificationTypes.ERROR
        ),
        NUM_PODS_CANNOT_BE_NEGATIVE(
                NotificationCodes.NUM_PODS_CANNOT_BE_NEGATIVE,
                RecommendationNotificationMsgConstant.NUM_PODS_CANNOT_BE_NEGATIVE,
                RecommendationNotificationTypes.ERROR
        ),
        HOURS_CANNOT_BE_ZERO(
                NotificationCodes.HOURS_CANNOT_BE_ZERO,
                RecommendationNotificationMsgConstant.HOURS_CANNOT_BE_ZERO,
                RecommendationNotificationTypes.ERROR
        ),
        HOURS_CANNOT_BE_NEGATIVE(
                NotificationCodes.HOURS_CANNOT_BE_NEGATIVE,
                RecommendationNotificationMsgConstant.HOURS_CANNOT_BE_NEGATIVE,
                RecommendationNotificationTypes.ERROR
        ),
        CPU_RECORDS_ARE_IDLE (
                NotificationCodes.NOTICE_CPU_RECORDS_ARE_IDLE,
                RecommendationNotificationMsgConstant.CPU_RECORDS_ARE_IDLE,
                RecommendationNotificationTypes.NOTICE
        ),
        CPU_RECORDS_ARE_ZERO (
                NotificationCodes.NOTICE_CPU_RECORDS_ARE_ZERO,
                RecommendationNotificationMsgConstant.CPU_RECORDS_ARE_ZERO,
                RecommendationNotificationTypes.NOTICE
        ),
        CPU_RECORDS_NOT_AVAILABLE(
                NotificationCodes.NOTICE_CPU_RECORDS_NOT_AVAILABLE,
                RecommendationNotificationMsgConstant.CPU_RECORDS_NOT_AVAILABLE,
                RecommendationNotificationTypes.NOTICE
        ),
        MEMORY_RECORDS_ARE_ZERO (
                NotificationCodes.NOTICE_MEMORY_RECORDS_ARE_ZERO,
                RecommendationNotificationMsgConstant.MEMORY_RECORDS_ARE_ZERO,
                RecommendationNotificationTypes.NOTICE
        ),
        MEMORY_RECORDS_NOT_AVAILABLE(
                NotificationCodes.NOTICE_MEMORY_RECORDS_NOT_AVAILABLE,
                RecommendationNotificationMsgConstant.MEMORY_RECORDS_NOT_AVAILABLE,
                RecommendationNotificationTypes.NOTICE
        ),
        CPU_REQUEST_NOT_SET (
                NotificationCodes.CRITICAL_CPU_REQUEST_NOT_SET,
                RecommendationNotificationMsgConstant.CPU_REQUEST_NOT_SET,
                RecommendationNotificationTypes.CRITICAL
        ),
        MEMORY_REQUEST_NOT_SET (
                NotificationCodes.CRITICAL_MEMORY_REQUEST_NOT_SET,
                RecommendationNotificationMsgConstant.MEMORY_REQUEST_NOT_SET,
                RecommendationNotificationTypes.CRITICAL
        ),
        CPU_LIMIT_NOT_SET (
                NotificationCodes.WARNING_CPU_LIMIT_NOT_SET,
                RecommendationNotificationMsgConstant.CPU_LIMIT_NOT_SET,
                RecommendationNotificationTypes.WARNING
        ),
        MEMORY_LIMIT_NOT_SET (
                NotificationCodes.CRITICAL_MEMORY_LIMIT_NOT_SET,
                RecommendationNotificationMsgConstant.MEMORY_LIMIT_NOT_SET,
                RecommendationNotificationTypes.CRITICAL
        );


        private int code;
        private String msg;
        private RecommendationNotificationTypes type;

        private RecommendationNotification (
                int code,
                String msg,
                RecommendationNotificationTypes type
        ) {
            this.code = code;
            this.msg = msg;
            this.type = type;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }

        public RecommendationNotificationTypes getType() {
            return type;
        }
    }

    public static final class NotificationCodes {
        private NotificationCodes() {
        }

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

        public static final int SECTION_INFO_START = 100000;
        public static final int SECTION_INFO_END = 199999;

        public static final int SECTION_INFO_SUBSECTION_GENERAL_INFO_START = 110000;
        public static final int SECTION_INFO_SUBSECTION_GENERAL_INFO_END = 119999;

        public static final int SECTION_INFO_SUBSECTION_GENERAL_INFO_SUBSYSTEM_GENERAL_START = 110000;
        // Subsystem section: Recommendation Engines
        // Range: 112000 - 112999 (Each subsection can be given 100 entries which fit total of 10 entries or engines)

        // Subsystem subsection: Default Engine
        // Range: 112000 - 112099
        // Subsystem subsection: Duration Based Engine
        // Range: 112100 - 112199
        public static final int DURATION_BASED_ENGINE_START = 112100;
        public static final int INFO_DURATION_BASED_RECOMMENDATIONS_AVAILABLE = 112101;
        public static final int DURATION_BASED_ENGINE_END = 112199;
        // Subsystem subsection: Profile Based Engine
        // Range: 112200 - 112299

        public static final int SECTION_INFO_SUBSECTION_GENERAL_INFO_SUBSYSTEM_GENERAL_END = 112999;

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

        public static final int SECTION_ERROR_START = 200000;
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
        public static final int NUM_PODS_CANNOT_BE_ZERO = 221001;
        public static final int NUM_PODS_CANNOT_BE_NEGATIVE = 221002;
        public static final int HOURS_CANNOT_BE_ZERO = 221003;
        public static final int HOURS_CANNOT_BE_NEGATIVE = 221004;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_GENERAL_END = 222999;

        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_CPU_START = 223000;
        public static final int AMOUNT_MISSING_IN_CPU_SECTION = 223001;
        public static final int INVALID_AMOUNT_IN_CPU_SECTION = 223002;
        public static final int FORMAT_MISSING_IN_CPU_SECTION = 223003;
        public static final int INVALID_FORMAT_IN_CPU_SECTION = 223004;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_CPU_END = 223999;

        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_MEMORY_START = 224000;
        public static final int AMOUNT_MISSING_IN_MEMORY_SECTION = 224001;
        public static final int INVALID_AMOUNT_IN_MEMORY_SECTION = 224002;
        public static final int FORMAT_MISSING_IN_MEMORY_SECTION = 224003;
        public static final int INVALID_FORMAT_IN_MEMORY_SECTION = 224004;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_MEMORY_END = 224999;

        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_NETWORK_START = 225000;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_NETWORK_END = 225999;

        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_DISK_START = 226000;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_DISK_END = 226999;

        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_POWER_START = 227000;
        public static final int SECTION_ERROR_SUBSECTION_DATA_SUBSYSTEM_POWER_END = 227999;

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

        public static final int SECTION_NOTICE_START = 300000;
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
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_CPU_END = 323999;

        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_MEMORY_START = 324000;
        public static final int NOTICE_MEMORY_RECORDS_ARE_ZERO = 324001;
        public static final int NOTICE_MEMORY_RECORDS_NOT_AVAILABLE = 324002;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_MEMORY_END = 324999;

        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_NETWORK_START = 325000;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_NETWORK_END = 325999;

        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_DISK_START = 326000;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_DISK_END = 326999;

        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_POWER_START = 327000;
        public static final int SECTION_NOTICE_SUBSECTION_DATA_SUBSYSTEM_POWER_END = 327999;

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

        public static final int SECTION_WARNING_START = 400000;
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

        public static final int SECTION_CRITICAL_START = 500000;
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

            // Contradicting Codes for DURATION_BASED_RECOMMENDATIONS_AVAILABLE
            Integer[] CODES_CONTRADICT_DURATION_BASED_RECOMMENDATIONS_AVAILABLE = {
                    INFO_NOT_ENOUGH_DATA
            };

            CONTRADICTING_MAP.put(
                    INFO_DURATION_BASED_RECOMMENDATIONS_AVAILABLE,
                    Arrays.asList(CODES_CONTRADICT_DURATION_BASED_RECOMMENDATIONS_AVAILABLE)
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
    }

    public static final class RecommendationConstants {
        public static final Double CPU_ZERO = 0.0;
        public static final Double CPU_ONE_MILLICORE = 0.001;
        public static final Double CPU_TEN_MILLICORE = 0.01;
        public static final Double CPU_HUNDRED_MILLICORE = 0.1;
        public static final Double CPU_FIVE_HUNDRED_MILLICORE = 0.5;
        public static final Double CPU_ONE_CORE = 1.0;
        public static final Double MEM_USAGE_BUFFER_DECIMAL = 0.2;
        public static final Double MEM_SPIKE_BUFFER_DECIMAL = 0.05;
    }

    public static final class RecommendationNotificationMsgConstant {
        public static final String NOT_ENOUGH_DATA = "There is not enough data available to generate a recommendation.";
        public static final String DURATION_BASED_RECOMMENDATIONS_AVAILABLE = "Duration Based Recommendations Available";
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

        private RecommendationNotificationMsgConstant() {

        }
    }

    public enum MetricName {
        cpuRequest,
        cpuLimit,
        cpuUsage,
        cpuThrottle,
        memoryRequest,
        memoryLimit,
        memoryUsage,
        memoryRSS
    }

    public enum K8S_OBJECT_TYPES {
        DEPLOYMENT,
        DEPLOYMENT_CONFIG,
        STATEFULSET,
        REPLICASET,
        REPLICATION_CONTROLLER,
        DAEMONSET,
    }

    public enum RegisterRecommendationEngineStatus {
        SUCCESS,
        ALREADY_EXISTS,
        INVALID
    }

    /**
     * Used to parse the Autotune kind resource
     */
    public static final class AutotuneObjectConstants {

        public static final String SPEC = "spec";
        public static final String SLO = "slo";
        public static final String SLO_CLASS = "slo_class";
        public static final String DIRECTION = "direction";
        public static final String OBJECTIVE_FUNCTION = "objective_function";
        public static final String OBJ_FUNCTION_TYPE = "function_type";
        public static final String EXPRESSION = "expression";
        public static final String FUNCTION_VARIABLES = "function_variables";
        public static final String NAME = "name";
        public static final String QUERY = "query";
        public static final String VALUE_TYPE = "value_type";
        public static final String DATASOURCE = "datasource";
        public static final String TOTAL_TRIALS = "total_trials";
        public static final String PARALLEL_TRIALS = "parallel_trials";
        public static final String MINIMIZE = "minimize";
        public static final String MAXIMIZE = "maximize";
        public static final String SELECTOR = "selector";
        public static final String MATCH_LABEL = "matchLabel";
        public static final String MATCH_LABEL_VALUE = "matchLabelValue";
        public static final String MATCH_ROUTE = "matchRoute";
        public static final String MATCH_URI = "matchURI";
        public static final String MATCH_SERVICE = "matchService";
        public static final String MODE = "mode";
        public static final String DEFAULT_MODE = "experiment";
        public static final String TARGET_CLUSTER = "target_cluster";
        public static final String DEFAULT_TARGET_CLUSTER = "local";
        public static final String METADATA = "metadata";
        public static final String NAMESPACE = "namespace";
        public static final String EXPERIMENT_ID = "experiment_id";
        public static final String HPO_ALGO_IMPL = "hpo_algo_impl";
        public static final String DEFAULT_HPO_ALGO_IMPL = "optuna_tpe";
        public static final String FUNCTION_VARIABLE = "function_variable: ";
        public static final String CLUSTER_NAME = "cluster_name";

        private AutotuneObjectConstants() {
        }
    }

    /**
     * Used to parse the KruizeLayer resource
     */
    public static final class AutotuneConfigConstants {

        public static final String METADATA = "metadata";
        public static final String NAMESPACE = "namespace";
        public static final String DATASOURCE = "datasource";
        public static final String LAYER_PRESENCE = "layer_presence";
        public static final String PRESENCE = "presence";
        public static final String LABEL = "label";
        public static final String QUERY_VARIABLES = "query_variables";
        public static final String VALUE = "value";
        public static final String LAYER_NAME = "layer_name";
        public static final String DETAILS = "details";
        public static final String LAYER_DETAILS = "layer_details";
        public static final String LAYER_LEVEL = "layer_level";
        public static final String TUNABLES = "tunables";
        public static final String QUERIES = "queries";
        public static final String NAME = "name";
        public static final String TUNABLE_NAME = "tunable_name";
        public static final String TUNABLE_VALUE = "tunable_value";
        public static final String QUERY = "query";
        public static final String KEY = "key";
        public static final String VALUE_TYPE = "value_type";
        public static final String UPPER_BOUND = "upper_bound";
        public static final String LOWER_BOUND = "lower_bound";
        public static final String CATEGORICAL_TYPE = "categorical";
        public static final String TUNABLE_CHOICES = "choices";
        public static final String TRUE = "true";
        public static final String FALSE = "false";
        public static final String DOUBLE = "double";
        public static final String LONG = "long";
        public static final String INTEGER = "integer";
        public static final Pattern BOUND_CHARS = Pattern.compile("[\\sa-zA-Z]");
        public static final Pattern BOUND_DIGITS = Pattern.compile("[\\s0-9\\.]");
        public static final String SLO_CLASS = "slo_class";
        public static final String LAYER_PRESENCE_LABEL = "layerPresenceLabel";
        public static final String LAYER_PRESENCE_LABEL_VALUE = "layerPresenceLabelValue";
        public static final String LAYER_PRESENCE_QUERIES = "layerPresenceQueries";
        public static final String LAYER_ID = "layer_id";
        public static final String STEP = "step";
        public static final String LAYER_GENERIC = "generic";
        public static final String LAYER_CONTAINER = "container";
        public static final String LAYER_HOTSPOT = "hotspot";
        public static final String LAYER_QUARKUS = "quarkus";
        public static final String LAYER_OPENJ9 = "openj9";
        public static final String LAYER_NODEJS = "nodejs";

        private AutotuneConfigConstants() {
        }

    }

    /**
     * Contains Strings used in REST services
     */
    public static final class ServiceConstants {

        public static final String JSON_CONTENT_TYPE = "application/json";
        public static final String CHARACTER_ENCODING = "UTF-8";
        public static final String EXPERIMENT_NAME = "experiment_name";
        public static final String DEPLOYMENTS = "deployments";
        public static final String DEPLOYMENT_NAME = "deployment_name";
        public static final String NAMESPACE = "namespace";
        public static final String STACKS = "stacks";
        public static final String STACK_NAME = "stack_name";
        public static final String CONTAINER_NAME = "container_name";
        public static final String LAYER_DETAILS = "layer_details";
        public static final String LAYERS = "layers";
        public static final String QUERY_URL = "query_url";
        public static final String TRAINING = "training";
        public static final String PRODUCTION = "production";
        public static final String TOTAL_TRIALS = "total_trials";
        public static final String TRIALS_COMPLETED = "trials_completed";
        public static final String TRIALS_ONGOING = "trials_ongoing";
        public static final String TRIALS_PASSED = "trials_passed";
        public static final String TRIALS_FAILED = "trials_failed";
        public static final String BEST_TRIAL = "best_trial";
        public static final String TRIALS_SUMMARY = "trials_summary";
        public static final String TRIAL_STATUS = "status";
        public static final String TRIAL_NUMBER = "trial_number";
        public static final String TRIAL_RESULT = "trial_result";
        public static final String TRIAL_ERRORS = "trial_errors";
        public static final String TRIAL_DURATION = "trial_duration";
        public static final String EXPERIMENT_TRIALS = "experiment_trials";
        public static final String NA = "NA";
        public static final String SECONDS = " seconds";
        public static final String LATEST = "latest";
        public static final String EXPERIMENT_REGISTERED = "Registered successfully with Kruize! View registered experiments at /listExperiments";
        public static final String RESULT_SAVED = "Results added successfully! View saved results at /listExperiments.";

        private ServiceConstants() {
        }
    }

    /**
     * Contains Strings used in the HOTSPOT Layer
     */
    public static final class HotspotConstants {

        public static final String XXOPTION = " -XX:";
        public static final String USE = "+Use";
        public static final String SERVER = " -server";
        public static final String ALLOW_PARALLEL_DEFINE_CLASS = "AllowParallelDefineClass";
        public static final String ALLOW_VECTORIZE_ON_DEMAND = "AllowVectorizeOnDemand";
        public static final String ALWAYS_COMPILE_LOOP_METHODS = "AlwaysCompileLoopMethods";
        public static final String ALWAYS_PRE_TOUCH = "AlwaysPreTouch";
        public static final String ALWAYS_TENURE = "AlwaysTenure";
        public static final String BACKGROUND_COMPILATION = "BackgroundCompilation";
        public static final String COMPILE_THRESHOLD = "CompileThreshold";
        public static final String COMPILE_THRESHOLD_SCALING = "CompileThresholdScaling";
        public static final String CONC_GC_THREADS = "ConcGCThreads";
        public static final String DO_ESCAPE_ANALYSIS = "DoEscapeAnalysis";
        public static final String FREQ_INLINE_SIZE = "FreqInlineSize";
        public static final String GC = "gc";
        public static final String INLINE_SMALL_CODE = "InlineSmallCode";
        public static final String LOOP_UNROLL_LIMIT = "LoopUnrollLimit";
        public static final String LOOP_UNROLL_MIN = "LoopUnrollMin";
        public static final String MAX_INLINE_LEVEL = "MaxInlineLevel";
        public static final String MAX_RAM_PERCENTAGE = "MaxRAMPercentage";
        public static final String MIN_INLINING_THRESHOLD = "MinInliningThreshold";
        public static final String MIN_SURVIVOR_RATIO = "MinSurvivorRatio";
        public static final String NETTY_BUFFER_CHECK = "nettyBufferCheck";
        public static final String NETTY_BUFFER_CHECKBOUNDS = "io.netty.buffer.checkBounds";
        public static final String NETTY_BUFFER_CHECKACCESSIBLE = "io.netty.buffer.checkAccessible";
        public static final String NEW_RATIO = "NewRatio";
        public static final String PARALLEL_GC_THREADS = "ParallelGCThreads";
        public static final String STACK_TRACE_IN_THROWABLE = "StackTraceInThrowable";
        public static final String TIERED_COMPILATION = "TieredCompilation";
        public static final String TIERED_STOP_AT_LEVEL = "TieredStopAtLevel";
        public static final String USE_INLINE_CACHES = "UseInlineCaches";
        public static final String USE_LOOP_PREDICATE = "UseLoopPredicate";
        public static final String USE_STRING_DEDUPLICATION = "UseStringDeduplication";
        public static final String USE_SUPER_WORD = "UseSuperWord";
        public static final String USE_TYPE_SPECULATION = "UseTypeSpeculation";

        private HotspotConstants() {
        }

    }

    /**
     * Contains Strings used in the QUARKUS Layer
     */
    public static final class QuarkusConstants {

        public static final String QUARKUS = "quarkus";
        public static final String DOPTION = " -D";

        private QuarkusConstants() {
        }

    }

    /**
     * Contains Strings used in the Container Layer
     */
    public static final class ContainerConstants {

        public static final String CPU_REQUEST = "cpuRequest";
        public static final String MEM_REQUEST = "memoryRequest";

        private ContainerConstants() {
        }

    }

    public static class createExperimentParallelEngineConfigs {
        /**
         * MAX Queue size to stack experiments
         */
        public static int QUEUE_SIZE = 20000;
        /**
         * Core pool size is the minimum number of workers to keep alive
         */
        public static int CORE_POOL_SIZE = 100;
        /**
         * Maximum number of workers limit
         */
        public static int MAX_POOL_SIZE = 1000;
        /**
         * Timeout for idle threads waiting for work. Threads use this timeout when there are more than corePoolSize present or if allowCoreThreadTimeOut. Otherwise they wait forever for new work.
         */
        public static int CORE_POOL_KEEPALIVETIME_IN_SECS = 5;
        /**
         * the time between successive executions
         */
        public static int DELAY_IN_SECS = 2;
        public static String EXECUTOR = "KRUIZE_EXECUTOR";

        private createExperimentParallelEngineConfigs() {
        }
    }

    public static class updateResultsParallelEngineConfigs {
        /**
         * MAX Queue size to stack experiments
         */
        public static int QUEUE_SIZE = 20000;
        /**
         * Core pool size is the minimum number of workers to keep alive
         */
        public static int CORE_POOL_SIZE = 100;
        /**
         * Maximum number of workers limit
         */
        public static int MAX_POOL_SIZE = 1000;
        /**
         * Timeout for idle threads waiting for work. Threads use this timeout when there are more than corePoolSize present or if allowCoreThreadTimeOut. Otherwise they wait forever for new work.
         */
        public static int CORE_POOL_KEEPALIVETIME_IN_SECS = 5;
        /**
         * the time between successive executions
         */
        public static int DELAY_IN_SECS = 2;
        public static String EXECUTOR = "KRUIZE_EXECUTOR";

    }

    public static final class PerformanceProfileConstants {

        public static final String PERFORMANCE_PROFILE_PLURALS = "kruizeperformanceprofiles";
        public static final String PERFORMANCE_PROFILE_RESOURCE_NAME = PERFORMANCE_PROFILE_PLURALS + GROUP;
        public static final String K8S_TYPE = "k8s_type";
        public static final String PERF_PROFILE = "performanceProfile";
        public static final String PERF_PROFILE_MAP = "performanceProfileMap";
        public static final String PERF_PROFILE_NAME = "name";
        public static final String OBJECTIVE_FUNCTION = "objectiveFunction";
        public static final String FUNCTION_VARIABLES = "functionVariables";
        public static final String VALUE_TYPE = "valueType";
        public static final String SOURCE = "source";
        public static final String PERFORMANCE_PROFILE_PKG = "com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface.";
        public static final String DEFAULT_PROFILE = "default";

        // Perf profile names
        public static final String RESOURCE_OPT_OPENSHIFT_PROFILE = "resource-optimization-openshift";
        public static final String RESOURCE_OPT_LOCAL_MON_PROFILE = "resource-optimization-local-monitoring";

        public static final Map<String, String> PerfProfileNames = Map.of(
                RESOURCE_OPT_OPENSHIFT_PROFILE, "ResourceOptimizationOpenshiftImpl",
                RESOURCE_OPT_LOCAL_MON_PROFILE, "ResourceOptimizationOpenshiftImpl"
        );

        public static final Map<String, Class> perfProfileInstances = Map.of(
                DEFAULT_PROFILE, DefaultImpl.class,
                RESOURCE_OPT_OPENSHIFT_PROFILE, ResourceOptimizationOpenshiftImpl.class,
                RESOURCE_OPT_LOCAL_MON_PROFILE, ResourceOptimizationOpenshiftImpl.class
        );
    }

    public static final class K8sObjectConstants {
        private K8sObjectConstants() {

        }

        public static final class Types {
            public static final String DEPLOYMENT = "deployment";
            public static final String DEPLOYMENT_CONFIG = "deploymentConfig";
            public static final String STATEFULSET = "statefulset";
            public static final String REPLICASET = "replicaset";
            public static final String REPLICATION_CONTROLLER = "replicationController";
            public static final String DAEMONSET = "daemonset";

            private Types() {

            }
        }
    }

    public static final class MetricNameConstants {
        public static final String CPU_REQUEST = "cpuRequest";
        public static final String CPU_LIMIT = "cpuLimit";
        public static final String CPU_USAGE = "cpuUsage";
        public static final String CPU_THROTTLE = "cpuThrottle";
        public static final String MEMORY_REQUEST = "memoryRequest";
        public static final String MEMORY_LIMIT = "memoryLimit";
        public static final String MEMORY_USAGE = "memoryUsage";
        public static final String MEMORY_RSS = "memoryRSS";

        private MetricNameConstants() {

        }

    }

    public static final class PercentileConstants {
        public static final Integer FIFTIETH_PERCENTILE = 50;
        public static final Integer NINETIETH_PERCENTILE = 90;
        public static final Integer NINETY_FIFTH_PERCENTILE = 95;
        public static final Integer NINETY_SIXTH_PERCENTILE = 96;
        public static final Integer NINETY_SEVENTH_PERCENTILE = 97;
        public static final Integer NINETY_EIGHTH_PERCENTILE = 98;
        public static final Integer NINETY_NINTH_PERCENTILE = 99;
        public static final Integer HUNDREDTH_PERCENTILE = 100;
    }

    public static final class BooleanString {
        public static final String TRUE_DEFAULT = "True";
        public static final String FALSE_DEFAULT = "False";
        public static final String TRUE_LOWER = TRUE_DEFAULT.toLowerCase();
        public static final String TRUE = TRUE_LOWER;
        public static final String FALSE_LOWER = FALSE_DEFAULT.toLowerCase();
        public static final String FALSE = FALSE_LOWER;
        public static final String TRUE_UPPER = TRUE_DEFAULT.toUpperCase();
        public static final String FALSE_UPPER = FALSE_DEFAULT.toUpperCase();

        private BooleanString() {

        }
    }

    public static class RecommendationEngine {
        private RecommendationEngine() {

        }

        public static class EngineNames {
            public static String DEFAULT_NAME = "Default";
            public static String DURATION_BASED = "Duration Based";
            public static String PROFILE_BASED = "Profile Based";

            private EngineNames() {

            }
        }

        public static class EngineKeys {
            public static String DURATION_BASED_KEY = "duration_based";
            public static String PROFILE_BASED_KEY = "profile_based";

            private EngineKeys() {

            }
        }

        public static class MinConstants {
            private MinConstants() {

            }

            public static class CPU {
                private CPU() {

                }
                public static final double CPU_MIN_RECOMMENDATION_VALUE = 0.1;
            }
        }

        public static class InternalConstants {
            private InternalConstants() {

            }

            public static final String CURRENT_CPU_REQUEST = "CURRENT_CPU_REQUEST";
            public static final String CURRENT_MEMORY_REQUEST = "CURRENT_MEMORY_REQUEST";
            public static final String CURRENT_CPU_LIMIT = "CURRENT_CPU_LIMIT";
            public static final String CURRENT_MEMORY_LIMIT = "CURRENT_MEMORY_LIMIT";
            public static final String RECOMMENDED_CPU_REQUEST = "RECOMMENDED_CPU_REQUEST";
            public static final String RECOMMENDED_MEMORY_REQUEST = "RECOMMENDED_MEMORY_REQUEST";
            public static final String RECOMMENDED_CPU_LIMIT = "RECOMMENDED_CPU_LIMIT";
            public static final String RECOMMENDED_MEMORY_LIMIT = "RECOMMENDED_MEMORY_LIMIT";
        }
    }
}
