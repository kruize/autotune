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

package com.autotune.analyzer.kruizeLayer.presence;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for label-based layer presence detection
 */
public class LabelBasedPresence implements LayerPresenceDetector {

    private List<LayerPresenceLabel> label;

    public LabelBasedPresence() {
        this.label = new ArrayList<>();
    }

    public LabelBasedPresence(List<LayerPresenceLabel> label) {
        this.label = label != null ? label : new ArrayList<>();
    }

    @Override
    public PresenceType getType() {
        return PresenceType.LABEL;
    }

    public List<LayerPresenceLabel> getLabel() {
        return label;
    }

    public void setLabel(List<LayerPresenceLabel> label) {
        this.label = label != null ? label : new ArrayList<>();
    }

    /**
     * Inner class for label name-value pairs
     */
    public static class LayerPresenceLabel {
        private String name;
        private String value;

        public LayerPresenceLabel() {
        }

        public LayerPresenceLabel(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "LayerPresenceLabel{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "LabelBasedPresence{" +
                "label=" + label +
                '}';
    }
}
