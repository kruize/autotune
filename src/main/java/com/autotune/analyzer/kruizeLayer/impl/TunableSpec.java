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

package com.autotune.analyzer.kruizeLayer.impl;

import com.autotune.analyzer.utils.AnalyzerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record TunableSpec(String layerName, String tunableName) {

    public TunableSpec {
        if (layerName == null && tunableName == null) {
            throw new IllegalArgumentException(AnalyzerConstants.LayerConstants.LogMessages.LAYER_NAME_N_TUNABLE_NAME_NOT_NULL);
        }
        if (layerName == null || layerName.isBlank()) {
            throw new IllegalArgumentException(AnalyzerConstants.LayerConstants.LogMessages.LAYER_NAME_NOT_NULL);
        }
        if (tunableName == null || tunableName.isBlank()) {
            throw new IllegalArgumentException(AnalyzerConstants.LayerConstants.LogMessages.TUNABLE_NAME_NOT_NULL);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TunableSpec)) return false;
        TunableSpec that = (TunableSpec) o;
        return Objects.equals(layerName, that.layerName) &&
                Objects.equals(tunableName, that.tunableName);
    }

    @Override
    public @NotNull String toString() {
        return layerName + ":" + tunableName;
    }
}
