/*******************************************************************************
 * Copyright (c) 2020, 2025 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.application.Tunable;
import com.google.gson.annotations.SerializedName;
import io.fabric8.kubernetes.api.model.ObjectReference;

import java.util.ArrayList;

/**
 * Container class for the KruizeLayer kubernetes kind, which is used to tune
 * a layer (container, runtime, framework or application)
 *
 * Refer to examples dir for a reference KruizeLayer yaml.
 */
public final class KruizeLayer
{
    private String apiVersion;
    private String kind;
    private LayerMetadata metadata;
    @SerializedName("layer_name")
    private String layerName;
    @SerializedName("layer_level")
    private int level;
	private String details;
    @SerializedName("layer_presence")
    private LayerPresence layerPresence;
    private ArrayList<Tunable> tunables;

    public KruizeLayer() {

    }

	public int getLevel() {
		return level;
	}

	public String getDetails() {
		return details;
	}

	public String getLayerName() {
		return layerName;
	}

	public ArrayList<Tunable> getTunables() {
		return new ArrayList<>(tunables);
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

    public LayerPresence getLayerPresence() {
        return layerPresence;
    }

    public void setLayerPresence(LayerPresence layerPresence) {
        this.layerPresence = layerPresence;
    }

    public LayerMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(LayerMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
	public String toString() {
		return "KruizeLayer{" +
                "apiVersion='" + apiVersion + '\'' +
                ", kind='" + kind + '\'' +
                ", level=" + level +
                ", layerName='" + layerName + '\'' +
                ", details='" + details + '\'' +
                ", metadata=" + metadata +
                ", layerPresence=" + layerPresence +
                ", tunables=" + tunables +
                '}';
	}
}
