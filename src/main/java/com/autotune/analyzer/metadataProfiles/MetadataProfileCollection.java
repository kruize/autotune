package com.autotune.analyzer.metadataProfiles;

import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MetadataProfileCollection {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataProfileCollection.class);
    private static MetadataProfileCollection metadataProfileCollectionInstance = new MetadataProfileCollection();
    private HashMap<String, MetadataProfile> metadataProfileCollection;

    private MetadataProfileCollection() {
        this.metadataProfileCollection = new HashMap<>();
    }

    public static MetadataProfileCollection getInstance() {
        return metadataProfileCollectionInstance;
    }

    public HashMap<String, MetadataProfile> getMetadataProfileCollection() {
        return metadataProfileCollection;
    }

    public void loadMetadataProfilesFromDB() {
        try {
            LOGGER.info(KruizeConstants.MetadataProfileConstants.CHECKING_AVAILABLE_METADATA_PROFILE_FROM_DB);
            Map<String, MetadataProfile> availableMetadataProfiles = new HashMap<>();
            new ExperimentDBService().loadAllMetadataProfiles(availableMetadataProfiles);
            if (availableMetadataProfiles.isEmpty()) {
                LOGGER.info(KruizeConstants.MetadataProfileConstants.NO_METADATA_PROFILE_FOUND_IN_DB);
            }else {
                for (Map.Entry<String, MetadataProfile> metadataProfile : availableMetadataProfiles.entrySet()) {
                    LOGGER.info(KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_FOUND, metadataProfile.getKey());
                    metadataProfileCollection.put(metadataProfile.getKey(), metadataProfile.getValue());
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }


    public void addMetadataProfile(MetadataProfile metadataProfile) {
        String metadataProfileName = metadataProfile.getMetadata().get("name").asText();

        LOGGER.info(KruizeConstants.MetadataProfileConstants.ADDING_METADATA_PROFILE + "{}", metadataProfileName);

        if(metadataProfileCollection.containsKey(metadataProfileName)) {
            LOGGER.error(KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_ALREADY_EXISTS + "{}", metadataProfileName);
        } else {
            LOGGER.info(KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_ADDED + "{}", metadataProfileName);
            metadataProfileCollection.put(metadataProfileName, metadataProfile);
        }
    }
}