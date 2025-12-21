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

import com.autotune.analyzer.kruizeLayer.presence.*;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Wrapper class for layer_presence that deserializes from YAML
 * and provides the appropriate LayerPresenceDetector implementation
 */
public class LayerPresence {

    private String presence;

    private ArrayList<LayerPresenceQuery> queries;

    private ArrayList<LabelBasedPresence.LayerPresenceLabel> label;

    public LayerPresence() {
    }

    /**
     * Factory method to get the appropriate LayerPresenceDetector implementation
     * based on what fields are populated
     *
     * @return LayerPresenceDetector implementation
     */
    public LayerPresenceDetector getDetector() {
        // Check for query-based presence
        if (queries != null && !queries.isEmpty()) {
            return new QueryBasedPresence(queries);
        }

        // Check for label-based presence
        if (label != null && !label.isEmpty()) {
            return new LabelBasedPresence(label);
        }

        // Default to presence always
        if (presence != null) {
            return new PresenceAlways(presence);
        }

        // If nothing is set, default to "always"
        return new PresenceAlways("always");
    }

    /**
     * Get the type of presence detector
     *
     * @return PresenceType
     */
    public LayerPresenceDetector.PresenceType getType() {
        return getDetector().getType();
    }

    public String getPresence() {
        return presence;
    }

    public void setPresence(String presence) {
        this.presence = presence;
    }

    public ArrayList<LayerPresenceQuery> getQueries() {
        return queries;
    }

    public void setQueries(ArrayList<LayerPresenceQuery> queries) {
        this.queries = queries;
    }

    public ArrayList<LabelBasedPresence.LayerPresenceLabel> getLabel() {
        return label;
    }

    public void setLabel(ArrayList<LabelBasedPresence.LayerPresenceLabel> label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "LayerPresence{" +
                "presence='" + presence + '\'' +
                ", queries=" + queries +
                ", label=" + label +
                ", type=" + getType() +
                '}';
    }
}
