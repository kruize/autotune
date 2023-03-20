/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.k8sObjects;

import com.autotune.analyzer.serviceObjects.ContainerMetricsHelper;
import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.common.data.result.StartEndTimeStampResults;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

public class ContainerObject {
    @SerializedName(KruizeConstants.JSONKeys.CONTAINER_IMAGE_NAME)
    private String image;
    private String container_name;
    private HashMap<Timestamp, StartEndTimeStampResults> results;
    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATIONS)
    private HashMap<Timestamp, HashMap<String,HashMap<String, Recommendation>>> recommendations;
    private List<ContainerMetricsHelper> metrics;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getContainer_name() {
        return container_name;
    }

    public void setContainer_name(String container_name) {
        this.container_name = container_name;
    }

    public HashMap<Timestamp, StartEndTimeStampResults> getResults() {
        return results;
    }

    public void setResults(HashMap<Timestamp, StartEndTimeStampResults> results) {
        this.results = results;
    }

    public HashMap<Timestamp, HashMap<String,HashMap<String, Recommendation>>> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(HashMap<Timestamp, HashMap<String,HashMap<String, Recommendation>>> recommendations) {
        this.recommendations = recommendations;
    }

    public List<ContainerMetricsHelper> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<ContainerMetricsHelper> metrics) {
        this.metrics = metrics;
    }
    @Override
    public String toString() {
        return "ContainerObject{" +
                "image='" + image + '\'' +
                ", container_name='" + container_name + '\'' +
                ", results=" + results +
                ", recommendations=" + recommendations +
                ", metrics=" + metrics +
                '}';
    }
}
