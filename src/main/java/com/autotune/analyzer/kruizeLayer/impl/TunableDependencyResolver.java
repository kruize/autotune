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

import com.autotune.analyzer.kruizeLayer.KruizeLayer;
import com.autotune.analyzer.kruizeLayer.Tunable;

import java.util.*;

public class TunableDependencyResolver {
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

        // Attach dependencies from layer implementations
        for (KruizeLayer kruizeLayer : kruizeLayers) {
            Layer layerImpl = LayerRegistry.getInstance().getLayer(kruizeLayer.getLayerName());

            if (layerImpl == null)
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
