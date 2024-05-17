package com.autotune.utils;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class MetricsConfig {

    public static Timer timerListRec, timerListExp, timerCreateExp, timerUpdateResults, timerUpdateRecomendations;
    public static Timer timerLoadRecExpName, timerLoadResultsExpName, timerLoadExpName, timerLoadRecExpNameDate, timerBoxPlots;
    public static Timer timerLoadAllRec, timerLoadAllExp, timerLoadAllResults;
    public static Timer timerAddRecDB, timerAddResultsDB, timerAddExpDB, timerAddBulkResultsDB;
    public static Timer timerAddPerfProfileDB, timerLoadPerfProfileName, timerLoadAllPerfProfiles;
    public static Timer.Builder timerBListRec, timerBListExp, timerBCreateExp, timerBUpdateResults, timerBUpdateRecommendations;
    public static Timer.Builder timerBLoadRecExpName, timerBLoadResultsExpName, timerBLoadExpName, timerBLoadRecExpNameDate, timerBBoxPlots;
    public static Timer.Builder timerBLoadAllRec, timerBLoadAllExp, timerBLoadAllResults;
    public static Timer.Builder timerBAddRecDB, timerBAddResultsDB, timerBAddExpDB, timerBAddBulkResultsDB;
    public static Timer.Builder timerBAddPerfProfileDB, timerBLoadPerfProfileName, timerBLoadAllPerfProfiles;
    public static PrometheusMeterRegistry meterRegistry;
    public static Timer timerListDS, timerImportDSMetadata;
    public static Timer.Builder timerBListDS, timerBImportDSMetadata;
    private static MetricsConfig INSTANCE;
    public String API_METRIC_DESC = "Time taken for Kruize APIs";
    public String DB_METRIC_DESC = "Time taken for KruizeDB methods";
    public String METHOD_METRIC_DESC = "Time taken for Kruize methods";

    private MetricsConfig() {
        meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        meterRegistry.config().commonTags("application", "Kruize");

        timerBListRec = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "listRecommendations").tag("method", "GET");
        timerBListExp = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "listExperiments").tag("method", "GET");
        timerBCreateExp = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "createExperiment").tag("method", "POST");
        timerBUpdateResults = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "updateResults").tag("method", "POST");
        timerBUpdateRecommendations = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "updateRecommendations").tag("method", "POST");

        timerBLoadRecExpName = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "loadRecommendationsByExperimentName");
        timerBLoadRecExpNameDate = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "loadRecommendationsByExperimentNameAndDate");
        timerBLoadResultsExpName = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "loadResultsByExperimentName");
        timerBLoadExpName = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "loadExperimentByName");
        timerBLoadAllRec = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "loadAllRecommendations");
        timerBLoadAllExp = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "loadAllExperiments");
        timerBLoadAllResults = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "loadAllResults");
        timerBAddRecDB = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "addRecommendationToDB");
        timerBAddResultsDB = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "addResultToDB");
        timerBAddBulkResultsDB = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "addBulkResultsToDBAndFetchFailedResults");
        timerBAddExpDB = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "addExperimentToDB");
        timerBAddPerfProfileDB = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "addPerformanceProfileToDB");
        timerBLoadPerfProfileName = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "loadPerformanceProfileByName");
        timerBLoadAllPerfProfiles = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "loadAllPerformanceProfiles");
        timerBBoxPlots = Timer.builder("KruizeMethod").description(METHOD_METRIC_DESC).tag("method", "generatePlots");

        timerBListDS = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "listDataSources").tag("method", "GET");
        timerBImportDSMetadata = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "importDataSourceMetadata").tag("method", "POST");
        timerBImportDSMetadata = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "importDataSourceMetadata").tag("method", "GET");
        new ClassLoaderMetrics().bindTo(meterRegistry);
        new ProcessorMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new JvmMemoryMetrics().bindTo(meterRegistry);
        meterRegistry.config().namingConvention(NamingConvention.dot);

    }

    public static PrometheusMeterRegistry meterRegistry() {
        if (INSTANCE == null) {
            synchronized (MetricsConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MetricsConfig();
                }
            }
        }
        return meterRegistry;
    }

}