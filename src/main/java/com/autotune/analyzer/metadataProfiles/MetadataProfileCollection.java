/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.analyzer.metadataProfiles;

import com.autotune.analyzer.metadataProfiles.utils.MetadataProfileUtil;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * MetadataProfileCollection class stores in-memory MetadataProfile objects created
 * using a HashMap where key is MetadataProfile name for the corresponding MetadataProfile object
 */
public class MetadataProfileCollection {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataProfileCollection.class);
    private static MetadataProfileCollection metadataProfileCollectionInstance = new MetadataProfileCollection();
    private HashMap<String, MetadataProfile> metadataProfileCollection;

    private MetadataProfileCollection() {
        this.metadataProfileCollection = new HashMap<>();
    }

    /**
     * Returns the instance of metadataProfileCollection class
     *
     * @return MetadataProfileCollection instance
     */
    public static MetadataProfileCollection getInstance() {
        return metadataProfileCollectionInstance;
    }


    /**
     * Returns the hashmap of metadata profile objects
     *
     * @return HashMap containing MetadataProfile objects
     */
    public HashMap<String, MetadataProfile> getMetadataProfileCollection() {
        return metadataProfileCollection;
    }

    /**
     * Loads metadataProfiles from database and adds it to the collection
     */
    public void loadMetadataProfilesFromDB() {
        try {
            LOGGER.info(KruizeConstants.MetadataProfileConstants.CHECKING_AVAILABLE_METADATA_PROFILE_FROM_DB);
            Map<String, MetadataProfile> availableMetadataProfiles = new HashMap<>();
            new ExperimentDBService().loadAllMetadataProfiles(availableMetadataProfiles);
            if (availableMetadataProfiles.isEmpty()) {
                LOGGER.info(KruizeConstants.MetadataProfileConstants.NO_METADATA_PROFILE_FOUND_IN_DB);
            } else {
                for (Map.Entry<String, MetadataProfile> metadataProfile : availableMetadataProfiles.entrySet()) {
                    LOGGER.info(KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_FOUND, metadataProfile.getKey());
                    metadataProfileCollection.put(metadataProfile.getKey(), metadataProfile.getValue());
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Adds metadataProfile to collection
     *
     * @param metadataProfile MetadataProfile object containing metadata profile queries
     */
    public void addMetadataProfile(MetadataProfile metadataProfile) {
        String metadataProfileName = metadataProfile.getMetadata().get("name").asText();

        LOGGER.info(KruizeConstants.MetadataProfileConstants.ADDING_METADATA_PROFILE + "{}", metadataProfileName);

        if(metadataProfileCollection.containsKey(metadataProfileName)) {
            LOGGER.error(KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_ALREADY_EXISTS + "{}", metadataProfileName);
        } else {
            LOGGER.info(KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_ADDED , metadataProfileName);
            metadataProfileCollection.put(metadataProfileName, metadataProfile);
        }
    }

    public void addMetadataProfileFromContainerPath(String containerFileName) {
        try {
            String defaultMetadataProfilePath = System.getenv(containerFileName);
            LOGGER.info("MetadataProfile file path: {}", defaultMetadataProfilePath);

            String jsonContent = null;
            try (InputStream inputStream = new FileInputStream(defaultMetadataProfilePath)) {
                jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                LOGGER.error(KruizeConstants.MetadataProfileConstants.MetadataProfileErrorMsgs.FILE_NOT_FOUND_ERROR, containerFileName);
            } catch (IOException e) {
                LOGGER.error(KruizeConstants.MetadataProfileConstants.MetadataProfileErrorMsgs.FILE_READ_ERROR_ERROR_MESSAGE, containerFileName);
            }

            MetadataProfile metadataProfile = Converters.KruizeObjectConverters.convertInputJSONToCreateMetadataProfile(jsonContent);

            ValidationOutputData validationOutputData = MetadataProfileUtil.validateAndAddMetadataProfile(metadataProfileCollection, metadataProfile);
            if (validationOutputData.isSuccess()) {
                ValidationOutputData addedToDB = new ExperimentDBService().addMetadataProfileToDB(metadataProfile);
                if (addedToDB.isSuccess()) {
                    LOGGER.info(KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_ADDED, metadataProfile.getMetadata().get(KruizeConstants.JSONKeys.NAME).asText());
                } else {
                    LOGGER.error(KruizeConstants.MetadataProfileConstants.MetadataProfileErrorMsgs.ADD_METADATA_PROFILE_TO_DB_ERROR,  addedToDB.getMessage());
                }
            } else {
                LOGGER.error(KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_VALIDATION_FAILURE, validationOutputData.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.MetadataProfileConstants.MetadataProfileErrorMsgs.ADD_DEFAULT_METADATA_PROFILE_EXCEPTION, e.getMessage());
        }
    }
}
