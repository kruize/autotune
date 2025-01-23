package com.autotune.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.util.concurrent.atomic.AtomicInteger;

public class MetricsConfig {
    
    public static Timer timerListRec, timerListExp, timerCreateExp, timerUpdateResults, timerUpdateRecomendations;
    public static Timer timerLoadRecExpName, timerLoadResultsExpName, timerLoadExpName, timerLoadRecExpNameDate, timerBoxPlots;
    public static Timer timerLoadAllRec, timerLoadAllExp, timerLoadAllResults;
    public static Timer timerAddRecDB, timerAddResultsDB, timerAddExpDB, timerAddBulkResultsDB;
    public static Timer timerAddPerfProfileDB, timerLoadPerfProfileName, timerLoadAllPerfProfiles;
    public static Timer timerAddMetadataProfileDB, timerLoadMetadataProfileName, timerLoadAllMetadataProfiles;
    public static Timer timerImportMetadata, timerGetMetadata;
    public static Timer timerJobStatus, timerCreateBulkJob, timerGetExpMap, timerCreateBulkExp, timerGenerateBulkRec, timerRunJob;
    public static Counter timerKruizeNotifications , timerBulkJobs;
    public static Timer.Builder timerBListRec, timerBListExp, timerBCreateExp, timerBUpdateResults, timerBUpdateRecommendations;
    public static Timer.Builder timerBLoadRecExpName, timerBLoadResultsExpName, timerBLoadExpName, timerBLoadRecExpNameDate, timerBBoxPlots;
    public static Timer.Builder timerBLoadAllRec, timerBLoadAllExp, timerBLoadAllResults;
    public static Timer.Builder timerBAddRecDB, timerBAddResultsDB, timerBAddExpDB, timerBAddBulkResultsDB;
    public static Timer.Builder timerBAddPerfProfileDB, timerBLoadPerfProfileName, timerBLoadAllPerfProfiles;
    public static Counter.Builder timerBKruizeNotifications, timerBBulkJobs;
    public static PrometheusMeterRegistry meterRegistry;
    public static Timer timerListDS, timerImportDSMetadata, timerListDSMetadata;
    public static Timer.Builder timerBListDS, timerBImportDSMetadata, timerBListDSMetadata;
    public static Timer.Builder timerBImportMetadata, timerBGetMetadata;
    public static Timer.Builder timerBJobStatus, timerBCreateBulkJob, timerBGetExpMap, timerBCreateBulkExp, timerBGenerateBulkRec, timerBRunJob;
    public static Timer.Builder timerBAddMetadataProfileDB, timerBLoadMetadataProfileName, timerBLoadAllMetadataProfiles;
    private static MetricsConfig INSTANCE;
    public String API_METRIC_DESC = "Time taken for Kruize APIs";
    public String DB_METRIC_DESC = "Time taken for KruizeDB methods";
    public String METHOD_METRIC_DESC = "Time taken for Kruize methods";
    public static final AtomicInteger activeJobs = new AtomicInteger(0);
    public static Gauge.Builder timerBBulkRunJobs;

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

        timerBListDS = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "datasources").tag("method", "GET");
        timerBImportDSMetadata = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "dsmetadata").tag("method", "POST");
        timerBListDSMetadata = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "dsmetadata").tag("method", "GET");
        timerBKruizeNotifications = Counter.builder("KruizeNotifications").description("Kruize notifications").tag("api", "updateRecommendations");

        timerBImportMetadata = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "datasources").tag("method", "importMetadata");
        timerBGetMetadata = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "datasources").tag("method", "getMetadata");
        timerBJobStatus = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "bulk").tag("method", "jobStatus");
        timerBCreateBulkJob = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "bulk").tag("method", "createBulkJob");
        timerBGetExpMap = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "bulk").tag("method", "getExperimentMap");
        timerBRunJob = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api", "bulk").tag("method", "runBulkJob");
        timerBBulkRunJobs = Gauge.builder("kruizeAPI_active_jobs_count", activeJobs, AtomicInteger::get).description("No.of bulk jobs running").tags("api", "bulk", "method", "runBulkJob" , "status", "running");
        timerBBulkRunJobs.register(meterRegistry);

        timerBAddMetadataProfileDB = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "addMetadataProfileToDB");
        timerBLoadMetadataProfileName = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "loadMetadataProfileByName");
        timerBLoadAllMetadataProfiles = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method", "loadAllMetadataProfiles");

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
