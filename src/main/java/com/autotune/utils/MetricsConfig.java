package com.autotune.utils;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    public static PrometheusMeterRegistry registry;
    public static Timer timerlistRec;

    @Bean
    public MetricsConfig() {
        createMeterRegistry();
    }

    public void createMeterRegistry() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        timerlistRec = Timer.builder("my.timer.listRec").description("Timer for listRecommendations").register(registry);
        registry.config().commonTags("application", "Kruize");
        //registry.config().meterFilter(MeterFilter.deny(id -> {
        //    String uri = id.getTag("uri");
        //    return uri != null && uri.startsWith("/health");
        //}));
        new ClassLoaderMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        registry.config().namingConvention(NamingConvention.dot);
        //registry.config().meterFilter(MeterFilter.ignoreTags("http.method"))
        //registry.config().meterFilter(MeterFilter.maximumAllowableTags("http.path", 2, MeterFilter.DenyTagValueMeterFilter.onDiscardOldest()));
    }

}
