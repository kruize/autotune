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

package com.autotune.analyzer.kruizeLayer;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LayerPresence {
    private String presence;

    @SerializedName("label")
    private List<LayerDetector> labels;

    private List<LayerDetector> queries;

    public LayerPresence() {}

    public String getPresence() {
        return presence;
    }

    public void setPresence(String presence) {
        this.presence = presence;
    }

    public List<LayerDetector> getLabels() {
        return labels;
    }

    public void setLabels(List<LayerDetector> labels) {
        this.labels = labels;
    }

    public List<LayerDetector> getQueries() {
        return queries;
    }

    public void setQueries(List<LayerDetector> queries) {
        this.queries = queries;
    }

    /**
     * Returns the active list of detectors regardless of whether
     * they came from 'label' or 'queries'.
     */
    public List<LayerDetector> getActiveDetectors() {
        if (labels != null && !labels.isEmpty()) {
            return labels;
        }
        if (queries != null && !queries.isEmpty()) {
            return queries;
        }
        return new java.util.ArrayList<>(); // Return empty list to prevent NullPointerExceptions
    }

    @Override
    public String toString() {
        return "LayerPresence{" +
                "presence='" + presence + '\'' +
                ", label=" + labels +
                ", queries=" + queries +
                '}';
    }
}
