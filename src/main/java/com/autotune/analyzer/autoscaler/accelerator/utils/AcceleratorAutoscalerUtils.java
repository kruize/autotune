package com.autotune.analyzer.autoscaler.accelerator.utils;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.utils.AnalyzerConstants;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Quantity;

import java.util.HashMap;
import java.util.Map;

public class AcceleratorAutoscalerUtils {
    public static void updateResourceValues(String containerName,
                                            Container container,
                                            HashMap<AnalyzerConstants.ResourceSetting,
                                                    HashMap<AnalyzerConstants.RecommendationItem,
                                                            RecommendationConfigItem>> recommendations,
                                            Map<String, Map<String, Quantity>> originalResourcesRequests,
                                            Map<String, Map<String, Quantity>> originalResourcesLimits) {

        originalResourcesRequests.put(containerName, container.getResources().getRequests());
        originalResourcesLimits.put(containerName, container.getResources().getLimits());

        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestRecommendation =
                recommendations.get(AnalyzerConstants.ResourceSetting.requests);

        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsRecommendation =
                recommendations.get(AnalyzerConstants.ResourceSetting.limits);

        Map<String, Quantity> requestMap = new HashMap<>();
        Map<String, Quantity> limitsMap = new HashMap<>();

        for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> entry : requestRecommendation.entrySet()) {
            requestMap.put(entry.getKey().toString(), new Quantity(String.valueOf(entry.getValue().getAmount().intValue()), entry.getValue().getFormat()));
        }

        for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> entry : limitsRecommendation.entrySet()) {
            if (entry.getKey().toString().contains("nvidia")) {
                limitsMap.put(entry.getKey().toString(), new Quantity(String.valueOf(entry.getValue().getAmount().intValue())));
            } else {
                limitsMap.put(entry.getKey().toString(), new Quantity(String.valueOf(entry.getValue().getAmount().intValue()), entry.getValue().getFormat()));
            }
        }


        container.getResources().setRequests(requestMap);
        container.getResources().setLimits(limitsMap);
    }
}
