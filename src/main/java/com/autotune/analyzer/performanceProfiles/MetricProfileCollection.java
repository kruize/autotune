/*******************************************************************************
 * Copyright (c) 2024, 2024 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.performanceProfiles;

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.performanceProfiles.utils.PerformanceProfileUtil;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
            LOGGER.info(KruizeConstants.MetricProfileConstants.METRIC_PROFILE_ADDED, metricProfileName);
            metricProfileCollection.put(metricProfileName, metricProfile);
        }
    }

    public void addMetricProfileFromConfigFile() {
        try {
            String metricProfilePath = KruizeDeploymentInfo.metric_profile_file_path;
            LOGGER.info("MetricProfile file path: {}", metricProfilePath);

            String jsonContent = null;
            try (InputStream inputStream = new FileInputStream(metricProfilePath)) {
                jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                LOGGER.error(KruizeConstants.MetricProfileConstants.MetricProfileErrorMsgs.FILE_NOT_FOUND_ERROR, metricProfilePath);
            } catch (IOException e) {
                LOGGER.error(KruizeConstants.MetricProfileConstants.MetricProfileErrorMsgs.FILE_READ_ERROR_ERROR_MESSAGE, metricProfilePath);
            }

            PerformanceProfile metricProfile = Converters.KruizeObjectConverters.convertInputJSONToCreateMetricProfile(jsonContent);

            ValidationOutputData validationOutputData = PerformanceProfileUtil.validateAndAddMetricProfile(metricProfileCollection, metricProfile);
            if (validationOutputData.isSuccess()) {
                ValidationOutputData addedToDB = new ExperimentDBService().addMetricProfileToDB(metricProfile);
                if (addedToDB.isSuccess()) {
                    LOGGER.info(KruizeConstants.MetricProfileConstants.METRIC_PROFILE_ADDED, metricProfile.getMetadata().get(KruizeConstants.JSONKeys.NAME).asText());
                } else {
                    LOGGER.error(KruizeConstants.MetricProfileConstants.MetricProfileErrorMsgs.ADD_METRIC_PROFILE_TO_DB_ERROR,  addedToDB.getMessage());
                }
            } else {
                LOGGER.error(KruizeConstants.MetricProfileConstants.MetricProfileErrorMsgs.METRIC_PROFILE_VALIDATION_FAILURE, validationOutputData.getMessage());
            }
        } catch (Exception | InvalidValueException e) {
            LOGGER.error(KruizeConstants.MetricProfileConstants.MetricProfileErrorMsgs.ADD_DEFAULT_METRIC_PROFILE_EXCEPTION, e.getMessage());
        }
    }
}
