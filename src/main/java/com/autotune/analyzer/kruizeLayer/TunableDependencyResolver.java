package com.autotune.analyzer.kruizeLayer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.kruizeLayer.layers.ContainerLayer;
import com.autotune.analyzer.kruizeLayer.layers.HotspotLayer;
import com.autotune.analyzer.kruizeLayer.layers.Layer;
import com.autotune.analyzer.kruizeLayer.layers.QuarkusLayer;
import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.*;

public final class TunableDependencyResolver {

    private TunableDependencyResolver() {}

    /**
     * Returns tunables ordered such that all dependencies are resolved first.
     */
    public static List<Tunable> resolve(List<KruizeLayer> kruizeLayers) {

        Map<TunableSpec, Tunable> tunableMap = new HashMap<>();

        Map<TunableSpec, List<TunableSpec>> graph = new HashMap<>();

        for (KruizeLayer kruizeLayer : kruizeLayers) {
            String layerName = kruizeLayer.getLayerName();

            for (Tunable tunable : kruizeLayer.getTunables()) {
                TunableSpec spec =
                        new TunableSpec(layerName, tunable.getName());

                tunableMap.put(spec, tunable);
                graph.putIfAbsent(spec, new ArrayList<>());
            }
        }

        // 2. Attach dependencies from layer implementations
        for (KruizeLayer kruizeLayer : kruizeLayers) {
            Layer layerImpl = null;

            if (kruizeLayer.getLayerName().equalsIgnoreCase(AnalyzerConstants.LayerConstants.CONTAINER_LAYER))
                layerImpl = new ContainerLayer();
            else if (kruizeLayer.getLayerName().equalsIgnoreCase(AnalyzerConstants.LayerConstants.HOTSPOT_LAYER))
                layerImpl = new HotspotLayer();
            else if (kruizeLayer.getLayerName().equalsIgnoreCase(AnalyzerConstants.LayerConstants.QUARKUS_LAYER))
                layerImpl = new QuarkusLayer();
            else
                continue;

            Map<String, List<TunableSpec>> deps =
                    layerImpl.getTunableDependencies();

            String layerName = kruizeLayer.getLayerName();

            for (Map.Entry<String, List<TunableSpec>> entry : deps.entrySet()) {
                TunableSpec source =
                        new TunableSpec(layerName, entry.getKey());

                if (!graph.containsKey(source)) {
                    continue;
                }

                for (TunableSpec dependency : entry.getValue()) {
                    if (tunableMap.containsKey(dependency)) {
                        graph.get(source).add(dependency);
                    }
                }
            }
        }

        return topologicalSort(tunableMap, graph);
    }

    private static List<Tunable> topologicalSort(
            Map<TunableSpec, Tunable> tunableMap,
            Map<TunableSpec, List<TunableSpec>> graph) {

        Map<TunableSpec, Integer> inDegree = new HashMap<>();

        for (TunableSpec node : graph.keySet()) {
            inDegree.put(node, 0);
        }

        for (List<TunableSpec> deps : graph.values()) {
            for (TunableSpec dep : deps) {
                inDegree.put(dep, inDegree.get(dep) + 1);
            }
        }

        Deque<TunableSpec> queue = new ArrayDeque<>();
        for (Map.Entry<TunableSpec, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Tunable> ordered = new ArrayList<>();

        while (!queue.isEmpty()) {
            TunableSpec current = queue.poll();
            ordered.add(tunableMap.get(current));

            for (TunableSpec dep : graph.getOrDefault(current, List.of())) {
                inDegree.put(dep, inDegree.get(dep) - 1);
                if (inDegree.get(dep) == 0) {
                    queue.add(dep);
                }
            }
        }

        if (ordered.size() != tunableMap.size()) {
            throw new IllegalStateException(
                    "Circular or unresolved tunable dependencies detected"
            );
        }

        return ordered;
    }
}

