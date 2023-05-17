package com.autotune.utils;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.*;

public class MetricsConfig {
    public MeterRegistry meterRegistry() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Timer timer = Timer.builder("my.timer").description("A timer metric").register(registry);
        registry.config().commonTags("application", "Kruize");
        registry.config().meterFilter(MeterFilter.deny(id -> {
                      String uri = id.getTag("uri");
                      return uri != null && uri.startsWith("/health");
                 }));
        new JvmGcMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        //registry.config().meterFilter(MeterFilter.ignoreTags("http.method"))
        registry.config().namingConvention(NamingConvention.dot)
 //       registry.config().meterFilter(MeterFilter.maximumAllowableTags("http.path", 2, MeterFilter.DenyTagValueMeterFilter.onDiscardOldest()));

        return registry;
    }

}
