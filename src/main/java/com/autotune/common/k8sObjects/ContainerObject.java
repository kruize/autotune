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

import com.autotune.common.data.result.Recommendation;
import com.autotune.common.data.result.StartEndTimeStampResults;

import java.sql.Timestamp;
import java.util.HashMap;

public class ContainerObject {
    private String image;
    private String container_name;
    private HashMap<Timestamp, StartEndTimeStampResults> results;
    private HashMap<Timestamp, HashMap<String, Recommendation>> recommendation;

    public ContainerObject(String container_name, String image) {
        this.image = image;
        this.container_name = container_name;
    }

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

    public HashMap<Timestamp, HashMap<String, Recommendation>> getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(HashMap<Timestamp, HashMap<String, Recommendation>> recommendation) {
        this.recommendation = recommendation;
    }

    @Override
    public String toString() {
        return "ContainerObject{" +
                "image='" + image + '\'' +
                ", container_name='" + container_name + '\'' +
                ", results=" + results +
                '}';
    }
}
