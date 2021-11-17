/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.k8sObjects;

/**
 * Container class for the layer presence information in an AutotuneConfig kubernetes kind.
 *
 * 	Used to detect the presence of the layer in an application. Autotune runs the query, looks for
 * 	the key, and all applications in the query output are matched to the AutotuneConfig object.
 */
public class LayerPresenceInfo
{
    //If true, apply to all autotuneobjects
    private final String presence;

    public final String layerPresenceQuery;
    public final String layerPresenceKey;
    public final String layerPresenceLabel;
    public final String layerPresenceLabelValue;

    public LayerPresenceInfo(String presence, String layerPresenceQuery, String layerPresenceKey, String layerPresenceLabel, String layerPresenceLabelValue) {
        this.presence = presence;
        this.layerPresenceQuery = layerPresenceQuery;
        this.layerPresenceKey = layerPresenceKey;
        this.layerPresenceLabel = layerPresenceLabel;
        this.layerPresenceLabelValue = layerPresenceLabelValue;
    }

    public String getPresence() {
        return presence;
    }

    public String getLayerPresenceQuery() {
        return layerPresenceQuery;
    }

    public String getLayerPresenceKey() {
        return layerPresenceKey;
    }

    public String getLayerPresenceLabel() {
        return layerPresenceLabel;
    }

    public String getLayerPresenceLabelValue() {
        return layerPresenceLabelValue;
    }
}
