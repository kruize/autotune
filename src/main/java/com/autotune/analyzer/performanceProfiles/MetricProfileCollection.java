package com.autotune.analyzer.performanceProfiles;

import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MetricProfileCollection {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricProfileCollection.class);
    private static MetricProfileCollection metricProfileCollectionInstance = new MetricProfileCollection();
    private HashMap<String, PerformanceProfile> metricProfileCollection;

    private MetricProfileCollection() {
        this.metricProfileCollection = new HashMap<>();
    }

    public static MetricProfileCollection getInstance() {
        return metricProfileCollectionInstance;
    }

    public HashMap<String, PerformanceProfile> getMetricProfileCollection() {
        return metricProfileCollection;
    }

    public void loadMetricProfilesFromDB() {
        try {
            LOGGER.info(KruizeConstants.MetricProfileConstants.CHECKING_AVAILABLE_METRIC_PROFILE_FROM_DB);
            Map<String, PerformanceProfile> availableMetricProfiles = new HashMap<>();
            new ExperimentDBService().loadAllMetricProfiles(availableMetricProfiles);
            if (availableMetricProfiles.isEmpty()) {
                LOGGER.info(KruizeConstants.MetricProfileConstants.NO_METRIC_PROFILE_FOUND_IN_DB);
            }else {
                for (Map.Entry<String, PerformanceProfile> metricProfile : availableMetricProfiles.entrySet()) {
                    LOGGER.info(KruizeConstants.MetricProfileConstants.METRIC_PROFILE_FOUND, metricProfile.getKey());
                    metricProfileCollection.put(metricProfile.getKey(), metricProfile.getValue());
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }


    public void addMetricProfile(PerformanceProfile metricProfile) {
        String metricProfileName = metricProfile.getMetadata().get("name").asText();

        LOGGER.info(KruizeConstants.MetricProfileConstants.ADDING_METRIC_PROFILE + "{}", metricProfileName);

        if(metricProfileCollection.containsKey(metricProfileName)) {
            LOGGER.error(KruizeConstants.MetricProfileConstants.METRIC_PROFILE_ALREADY_EXISTS + "{}", metricProfileName);
        } else {
            LOGGER.info(KruizeConstants.MetricProfileConstants.METRIC_PROFILE_ADDED + "{}", metricProfileName);
            metricProfileCollection.put(metricProfileName, metricProfile);
        }
    }
}
