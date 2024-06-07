package com.autotune.metrics;

import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForModel;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.common.data.result.ContainerData;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tags;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * KruizeNotificationCollectionRegistry is responsible for logging and creating metrics for notifications
 * related to Kruize recommendations.
 */
public class KruizeNotificationCollectionRegistry {
    private String experiment_name;
    private Timestamp interval_end_time;
    private String container_name;

    /**
     * Constructor to initialize KruizeNotificationCollectionRegistry with experiment name, interval end time, and container name.
     *
     * @param experiment_name   Name of the experiment.
     * @param interval_end_time End time of the interval.
     * @param container_name    Name of the container.
     */
    public KruizeNotificationCollectionRegistry(String experiment_name, Timestamp interval_end_time, String container_name) {
        this.experiment_name = experiment_name;
        this.interval_end_time = interval_end_time;
        this.container_name = container_name;
    }

    /**
     * Logs notifications from the given ContainerData by iterating through its recommendation structure and creating appropriate counters.
     *
     * @param containerData The container data from which to log notifications.
     */
    public void logNotification(ContainerData containerData) {
        HashMap<Integer, RecommendationNotification> containerLevelNotifications = containerData.getContainerRecommendations().getNotificationMap();
        createCounterTag("container", null, null, containerLevelNotifications.values());
        for (MappedRecommendationForTimestamp mappedRecommendationForTimestamp : containerData.getContainerRecommendations().getData().values()) {
            HashMap<Integer, RecommendationNotification> timeStampNotificationHashMap = mappedRecommendationForTimestamp.getHigherLevelNotificationMap();
            createCounterTag("timestamp", null, null, timeStampNotificationHashMap.values());
            for (Map.Entry<String, TermRecommendations> entry : mappedRecommendationForTimestamp.getRecommendationForTermHashMap().entrySet()) {
                String termName = entry.getKey();
                TermRecommendations termRecommendations = entry.getValue();
                HashMap<Integer, RecommendationNotification> termLevelNotificationHashMap = termRecommendations.getNotifications();
                createCounterTag("term", termName, null, termLevelNotificationHashMap.values());
                if (null != termRecommendations.getRecommendationForModelHashMap()) {
                    for (Map.Entry<String, MappedRecommendationForModel> recommendationForModel : termRecommendations.getRecommendationForModelHashMap().entrySet()) {
                        String modelName = recommendationForModel.getKey();
                        MappedRecommendationForModel mappedRecommendationForModel = recommendationForModel.getValue();
                        HashMap<Integer, RecommendationNotification> modelNotificationHashMap = mappedRecommendationForModel.getNotificationHashMap();
                        createCounterTag("model", termName, modelName, modelNotificationHashMap.values());
                    }
                }
            }
        }
    }

    /**
     * Creates a counter with tags for the given level, term, model, and list of recommendation notifications.
     *
     * @param level                          The level of the notification (e.g., container, timestamp, term, model).
     * @param term                           The term associated with the notification.
     * @param model                          The cost or performance model associated with the notification.
     * @param recommendationNotificationList The list of recommendation notifications to create counters for.
     */
    public void createCounterTag(String level, String term, String model, Collection<RecommendationNotification> recommendationNotificationList) {
        for (RecommendationNotification recommendationNotification : recommendationNotificationList) {
            Tags additionalTags = Tags.empty();
            if (("|" + KruizeDeploymentInfo.log_recommendation_metrics_level + "|").contains("|" + recommendationNotification.getType() + "|") == true) {
                additionalTags = additionalTags.and(KruizeConstants.KRUIZE_RECOMMENDATION_METRICS.TAG_NAME, String.format(KruizeConstants.KRUIZE_RECOMMENDATION_METRICS.notification_format, this.experiment_name, this.container_name, KruizeConstants.DateFormats.simpleDateFormatForUTC.format(this.interval_end_time), level, term, model, String.valueOf(recommendationNotification.getCode()), recommendationNotification.getType(), recommendationNotification.getMessage()));
                Counter counterNotifications = MetricsConfig.meterRegistry().find(KruizeConstants.KRUIZE_RECOMMENDATION_METRICS.METRIC_NAME).tags(additionalTags).counter();
                if (counterNotifications == null) {
                    counterNotifications = MetricsConfig.timerBKruizeNotifications.tags(additionalTags).register(MetricsConfig.meterRegistry);
                }
                counterNotifications.increment();
            }
        }
    }
}
