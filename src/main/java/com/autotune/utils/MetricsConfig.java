package com.autotune.utils;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.jetty.util.thread.ThreadPool;

public class MetricsConfig {

    private static MetricsConfig INSTANCE;
    public static Timer timerListRec, timerListExp, timerCreateExp, timerUpdateResults;
    public static Timer timerLoadRecExpName, timerLoadResultsExpName, timerLoadExpName;
    public static Timer timerLoadAllRec, timerLoadAllExp, timerLoadAllResults;
    public static Timer timerAddRecDB , timerAddResultsDB , timerAddExpDB;
    public static Timer.Builder timerBListRec, timerBListExp, timerBCreateExp, timerBUpdateResults;
    public static Timer.Builder timerBLoadRecExpName, timerBLoadResultsExpName, timerBLoadExpName;
    public static Timer.Builder timerBLoadAllRec, timerBLoadAllExp, timerBLoadAllResults;
    public static Timer.Builder timerBAddRecDB , timerBAddResultsDB , timerBAddExpDB;
    public String API_METRIC_DESC = "Time taken for Kruize APIs";
    public String DB_METRIC_DESC = "Time taken for KruizeDB methods";
    public static PrometheusMeterRegistry meterRegistry;

    private MetricsConfig() {
        meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        meterRegistry.config().commonTags("application", "Kruize");

        timerBListRec = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api","listRecommendations").tag("method","GET");
        timerBListExp = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api","listExperiments").tag("method","GET");
        timerBCreateExp = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api","createExperiment").tag("method","POST");
        timerBUpdateResults = Timer.builder("kruizeAPI").description(API_METRIC_DESC).tag("api","updateResults").tag("method","POST");

        timerBLoadRecExpName = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method","loadRecommendationsByExperimentName");
        timerBLoadResultsExpName = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method","loadResultsByExperimentName");
        timerBLoadExpName = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method","loadExperimentByName");
        timerBLoadAllRec = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method","loadAllRecommendations");
        timerBLoadAllExp = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method","loadAllExperiments");
        timerBLoadAllResults = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method","loadAllResults");
        timerBAddRecDB = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method","addRecommendationToDB");
        timerBAddResultsDB = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method","addResultsToDB");
        timerBAddExpDB = Timer.builder("kruizeDB").description(DB_METRIC_DESC).tag("method","addExperimentToDB");

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