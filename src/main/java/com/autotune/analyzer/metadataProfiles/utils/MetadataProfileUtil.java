/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.metadataProfiles.utils;

import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.analyzer.metadataProfiles.MetadataProfileValidation;
import com.autotune.common.data.ValidationOutputData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class MetadataProfileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(com.autotune.analyzer.metadataProfiles.utils.MetadataProfileUtil.class);
    private MetadataProfileUtil() {

    }

    /**
     * validates the metadata profile fields and the data and then adds it to the map
     * @param metadataProfile
     * @return
     */
    public static ValidationOutputData validateAndAddProfile(Map<String, MetadataProfile> metadataProfileMapProfilesMap, MetadataProfile metadataProfile) {
        ValidationOutputData validationOutputData;
        try {
            validationOutputData = new MetadataProfileValidation(metadataProfileMapProfilesMap).validate(metadataProfile);
            if (validationOutputData.isSuccess()) {
                addMetadataProfile(metadataProfileMapProfilesMap, metadataProfile);
            } else {
                validationOutputData.setMessage("Validation failed: " + validationOutputData.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Validate and add profile failed: " + e.getMessage());
            validationOutputData = new ValidationOutputData(false, "Validation failed: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return validationOutputData;
    }


    public static void addMetadataProfile(Map<String, MetadataProfile> metadataProfileMap, MetadataProfile metadataProfile) {
        metadataProfileMap.put(metadataProfile.getMetadata().get("name").asText(), metadataProfile);
        LOGGER.debug("Added MetadataProfile: {} ", metadataProfile.getMetadata().get("name"));
    }

}

