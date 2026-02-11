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
package com.autotune.analyzer.kruizeLayer.impl.runtime;

import com.autotune.analyzer.kruizeLayer.impl.Layer;
import com.autotune.analyzer.kruizeLayer.impl.TunableSpec;
import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.List;
import java.util.Map;

public class SemeruLayer implements Layer {
    private static final SemeruLayer INSTANCE = new SemeruLayer();

    private SemeruLayer() {
    }

    public static SemeruLayer getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return AnalyzerConstants.LayerConstants.SEMERU_LAYER;
    }

    @Override
    public Map<String, List<TunableSpec>> getTunableDependencies() {
        return Map.of(
                AnalyzerConstants.LayerConstants.TunablesConstants.MAX_RAM_PERC,
                List.of(
                        new TunableSpec(
                                AnalyzerConstants.LayerConstants.CONTAINER_LAYER, AnalyzerConstants.LayerConstants.TunablesConstants.MEMORY_LIMIT
                        )
                ),
                AnalyzerConstants.LayerConstants.TunablesConstants.GC_POLICY,
                List.of(
                        new TunableSpec(
                                AnalyzerConstants.LayerConstants.CONTAINER_LAYER, AnalyzerConstants.LayerConstants.TunablesConstants.CPU_LIMIT
                        ),
                        new TunableSpec(
                                AnalyzerConstants.LayerConstants.CONTAINER_LAYER, AnalyzerConstants.LayerConstants.TunablesConstants.MEMORY_LIMIT
                        )
                )
        );
    }
}
