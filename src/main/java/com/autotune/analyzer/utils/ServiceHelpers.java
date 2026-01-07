/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.utils;

import com.autotune.analyzer.application.ApplicationDeployment;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfilesDeployment;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Date;

import static com.autotune.operator.KruizeOperator.deploymentMap;

/**
 * Helper functions used by the REST APIs to create the output JSON object
 */
public class ServiceHelpers {
    private ServiceHelpers() {
    }

    /**
     * Copy over the details of the experiment from the given Autotune Object to the JSON object provided.
     *
     * @param experimentJson
     * @param kruizeObject
     */
    public static void addExperimentDetails(JSONObject experimentJson, KruizeObject kruizeObject) {
        PerformanceProfile performanceProfile = PerformanceProfilesDeployment.performanceProfilesMap
                .get(kruizeObject.getPerformanceProfile());
        experimentJson.put(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME, kruizeObject.getExperimentName());
        experimentJson.put(AnalyzerConstants.AutotuneObjectConstants.DIRECTION, performanceProfile.getSloInfo().getDirection());
        experimentJson.put(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION, performanceProfile.getSloInfo().getObjectiveFunction());
        experimentJson.put(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS, performanceProfile.getSloInfo().getSloClass());
        experimentJson.put(AnalyzerConstants.AutotuneObjectConstants.EXPERIMENT_ID, kruizeObject.getExperiment_id());
        experimentJson.put(AnalyzerConstants.AutotuneObjectConstants.HPO_ALGO_IMPL, kruizeObject.getHpoAlgoImpl());
        experimentJson.put(AnalyzerConstants.AutotuneObjectConstants.NAMESPACE, kruizeObject.getNamespace());
    }

    /**
     * Copy over the array of deployments for the given Autotune Object to the JSON Object provided
     *
     * @param experimentJson JSON object to be updated
     * @param kruizeObject
     */
    public static void addDeploymentDetails(JSONObject experimentJson, KruizeObject kruizeObject) {
        if (deploymentMap.get(kruizeObject.getExperimentName()).isEmpty()) {
            return;
        }

        JSONArray deploymentArray = new JSONArray();
        for (String deploymentName : deploymentMap.get(kruizeObject.getExperimentName()).keySet()) {
            JSONObject deploymentJson = new JSONObject();
            ApplicationDeployment applicationDeployment = deploymentMap.get(kruizeObject.getExperimentName()).get(deploymentName);
            deploymentJson.put(AnalyzerConstants.ServiceConstants.DEPLOYMENT_NAME, applicationDeployment.getDeploymentName());
            deploymentJson.put(AnalyzerConstants.ServiceConstants.NAMESPACE, applicationDeployment.getNamespace());
            deploymentArray.put(deploymentJson);
        }

        experimentJson.put(AnalyzerConstants.ServiceConstants.DEPLOYMENTS, deploymentArray);
    }

    public static class KruizeObjectOperations {
        private KruizeObjectOperations() {

        }

        public static boolean checkRecommendationTimestampExists(KruizeObject kruizeObject, String timestamp) {
            boolean timestampExists = false;
            try {
                if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, timestamp)) {
                    return false;
                }
                Date medDate = Utils.DateUtils.getDateFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, timestamp);
                if (null == medDate) {
                    return false;
                }
                Timestamp givenTimestamp = new Timestamp(medDate.getTime());
                for (K8sObject k8sObject : kruizeObject.getKubernetes_objects()) {
                    for (ContainerData containerData : k8sObject.getContainerDataMap().values()) {
                        for (Timestamp key : containerData.getContainerRecommendations().getData().keySet()) {
                            if (key.equals(givenTimestamp)) {
                                timestampExists = true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return timestampExists;
        }
    }
}
