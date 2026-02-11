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
