/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

public class LayerTunable {
    public String layerName;
    public String metricName;

    public LayerTunable(String layerName, String metricName) {
        this.layerName = layerName;
        this.metricName = metricName;
    }

    public String getLayerName() {
        return layerName;
    }

    public String getMetricName() {
        return metricName;
    }

    @Override
    public String toString() {
        return "LayerTunable{" +
                "layerName='" + layerName + '\'' +
                ", metricName='" + metricName + '\'' +
                '}';
    }
}
