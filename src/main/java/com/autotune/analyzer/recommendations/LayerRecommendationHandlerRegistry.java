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

package com.autotune.analyzer.recommendations;

import com.autotune.analyzer.recommendations.layers.HotspotLayerRecommendationHandler;
import com.autotune.analyzer.recommendations.layers.QuarkusLayerRecommendationHandler;
import com.autotune.analyzer.recommendations.layers.SemeruLayerRecommendationHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of layer recommendation handlers.
 */
public class LayerRecommendationHandlerRegistry {
    private static final LayerRecommendationHandlerRegistry INSTANCE = new LayerRecommendationHandlerRegistry();

    private final Map<String, LayerRecommendationHandler> handlers = new ConcurrentHashMap<>();

    private LayerRecommendationHandlerRegistry() {
        register(HotspotLayerRecommendationHandler.getInstance());
        register(SemeruLayerRecommendationHandler.getInstance());
        register(QuarkusLayerRecommendationHandler.getInstance());
    }

    public static LayerRecommendationHandlerRegistry getInstance() {
        return INSTANCE;
    }

    private void register(LayerRecommendationHandler handler) {
        if (handler != null && handler.getLayerName() != null) {
            handlers.put(handler.getLayerName().toLowerCase(), handler);
        }
    }

    public LayerRecommendationHandler getHandler(String layerName) {
        return layerName != null ? handlers.get(layerName.toLowerCase()) : null;
    }
}
