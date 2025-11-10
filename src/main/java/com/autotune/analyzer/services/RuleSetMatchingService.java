package com.autotune.analyzer.services;

import com.autotune.analyzer.serviceObjects.CreateRuleSetsAPIObject;
import com.autotune.analyzer.Layer.Layer;
import com.autotune.database.service.ExperimentDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

public class RuleSetMatchingService {
    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(RuleSetMatchingService.class);
    private final ExperimentDBService experimentDBService;

    public RuleSetMatchingService() {
        this.experimentDBService = new ExperimentDBService();
    }

    public CreateRuleSetsAPIObject matchRuleSets(HashMap<String, Layer> detectedLayers) {

        if (detectedLayers == null || detectedLayers.isEmpty()) {
            LOGGER.warn("no layers for ruleset matching");
            return null;
        }
        LOGGER.info("matching rulesets");
        LOGGER.info("detectedLayers = {}", detectedLayers.keySet());

        try {
            // get rule sets
            List<CreateRuleSetsAPIObject> ruleSetsList = experimentDBService.loadRulesets();

            if(ruleSetsList == null){
                LOGGER.warn("no rulesets found");
                return null;

            }
            LOGGER.debug("Loaded {} rulesets from database", ruleSetsList.size());
            // find match
            for (CreateRuleSetsAPIObject ruleSet : ruleSetsList) {
                String rulesetName = getRuleSetName(ruleSet);

                if (rulesetName == null || rulesetName.trim().isEmpty()) {
                    LOGGER.warn("Ruleset has no name, skipping");
                    continue;
                }

                String[] requiredLayers = rulesetName.split("-");
                boolean allLayersMatch = true;
                for (String layerName : requiredLayers) {

                    String layertrim = layerName.trim();
                    if(!detectedLayers.containsKey(layertrim)){
                        LOGGER.warn("layer " + layerName + " not found");
                        allLayersMatch = false;
                        break;
                    }
                }
                if(allLayersMatch){
                    LOGGER.info("matching ruleset " + rulesetName);
                    return ruleSet;
                }

            }

            LOGGER.warn("No exact matching ruleset found for detected layers");
            return null;

        } catch (Exception e) {
            LOGGER.error("Error during ruleset matching: {}", e.getMessage(), e);
            return null;
        }
    }

    private String getRuleSetName(CreateRuleSetsAPIObject ruleset) {
        if (ruleset == null || ruleset.getMetadata() == null) {
            LOGGER.warn("Ruleset or metadata is null");
            return null;
        }
        return ruleset.getMetadata().getName();
    }

}
