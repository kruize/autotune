/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Kruize Layer configuration that defines tunable parameters
 * and layer presence detection for application optimization.
 */
public final class KruizeLayer {
    private String apiVersion;
    private String kind;
    private LayerMetadata metadata;
    @SerializedName("layer_name")
    private String layerName;
    @SerializedName("layer_level")
    private int layerLevel;
    private String details;
    @SerializedName("layer_presence")
    private LayerPresence layerPresence;
    private ArrayList<Tunable> tunables;

    public KruizeLayer() {
    }

    public KruizeLayer(String name, String namespace, String apiVersion, String kind,
                       String layerName, int layerLevel, String details,
                       String presence, List<LayerPresenceQuery> queries,
                       String labelName, String labelValue,
                       ArrayList<Tunable> tunables) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.layerName = layerName;
        this.layerLevel = layerLevel;
        this.details = details;
        this.tunables = tunables;

        // Create LayerMetadata
        this.metadata = new LayerMetadata();
        this.metadata.setName(name);

        // Create LayerPresence
        this.layerPresence = new LayerPresence();
        this.layerPresence.setPresence(presence);
        if (queries != null && !queries.isEmpty()) {
            this.layerPresence.setQueries(queries);
        }
        if (labelName != null && labelValue != null) {
            LayerPresenceLabel label = new LayerPresenceLabel(labelName, labelValue);
            this.layerPresence.setLabel(new ArrayList<>(java.util.Arrays.asList(label)));
        }
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public LayerMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(LayerMetadata metadata) {
        this.metadata = metadata;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public int getLayerLevel() {
        return layerLevel;
    }

    public void setLayerLevel(int layerLevel) {
        this.layerLevel = layerLevel;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LayerPresence getLayerPresence() {
        return layerPresence;
    }

    public void setLayerPresence(LayerPresence layerPresence) {
        this.layerPresence = layerPresence;
    }

    public ArrayList<Tunable> getTunables() {
        return tunables;
    }

    public void setTunables(ArrayList<Tunable> tunables) {
        this.tunables = tunables;
    }

    @Override
    public String toString() {
        return "KruizeLayer{" +
                "apiVersion='" + apiVersion + '\'' +
                ", kind='" + kind + '\'' +
                ", layerLevel=" + layerLevel +
                ", layerName='" + layerName + '\'' +
                ", details='" + details + '\'' +
                ", metadata=" + metadata +
                ", layerPresence=" + layerPresence +
                ", tunables=" + tunables +
                '}';
    }
}
