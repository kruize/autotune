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

public class MetricsConfig {

    private static MetricsConfig INSTANCE;
    public static Timer timerlistRec, timerlistExp, timercreateExp, timerupdateResults;
    public static Timer timerloadRecByExpName, timerloadResultsExpName, timerloadExpName;
    public static Timer timerloadAllRec, timerloadAllExp, timerloadAllResults;
    public static Timer timeraddRecDB, timeraddResultsDB, timeraddExpDB;

    public static PrometheusMeterRegistry meterRegistry;

    // Private constructor to prevent instantiation
    private MetricsConfig() {
        meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        meterRegistry.config().commonTags("application", "Kruize");

        timerlistRec = Timer.builder("kruizeAPI").description("Time taken for Kruize APIs").tag("api","listRecommendations").tag("method","GET").register(meterRegistry);
        timerlistExp = Timer.builder("kruizeAPI").description("Time taken for Kruize APIs").tag("api","listExperiments").tag("method","GET").register(meterRegistry);
        timercreateExp = Timer.builder("kruizeAPI").description("Time taken for Kruize APIs").tag("api","createExperiment").tag("method","POST").register(meterRegistry);
        timerupdateResults = Timer.builder("kruizeAPI").description("Time taken for Kruize APIs").tag("api","updateResults").tag("method","POST").register(meterRegistry);

        timerloadRecByExpName = Timer.builder("kruizeDB").description("Time taken for KruizeDB methods").tag("method","loadRecommendationsByExperimentName").register(meterRegistry);
        timerloadResultsExpName = Timer.builder("kruizeDB").description("Time taken for KruizeDB methods").tag("method","loadResultsByExperimentName").register(meterRegistry);
        timerloadExpName = Timer.builder("kruizeDB").description("Time taken for KruizeDB methods").tag("method","loadExperimentByName").register(meterRegistry);
        timerloadAllRec = Timer.builder("kruizeDB").description("Time taken for KruizeDB methods").tag("method","loadAllRecommendations").register(meterRegistry);
        timerloadAllExp = Timer.builder("kruizeDB").description("Time taken for KruizeDB methods").tag("method","loadAllExperiments").register(meterRegistry);
        timerloadAllResults = Timer.builder("kruizeDB").description("Time taken for KruizeDB methods").tag("method","loadAllResults").register(meterRegistry);
        timeraddRecDB = Timer.builder("kruizeDB").description("Time taken for KruizeDB methods").tag("method","addRecommendationToDB").register(meterRegistry);
        timeraddResultsDB = Timer.builder("kruizeDB").description("Time taken for KruizeDB methods").tag("method","addResultsToDB").register(meterRegistry);
        timeraddExpDB = Timer.builder("kruizeDB").description("Time taken for KruizeDB methods").tag("method","addExperimentToDB").register(meterRegistry);

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