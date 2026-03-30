package com.autotune.analyzer.kruizeLayer.impl;

import com.autotune.analyzer.kruizeLayer.impl.framework.QuarkusLayer;
import com.autotune.analyzer.kruizeLayer.impl.infra.ContainerLayer;
import com.autotune.analyzer.kruizeLayer.impl.runtime.HotspotLayer;
import com.autotune.analyzer.kruizeLayer.impl.runtime.SemeruLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LayerRegistry {
    private static final LayerRegistry INSTANCE = new LayerRegistry();

    private final Map<String, Layer> layers = new ConcurrentHashMap<>();
    private LayerRegistry() {
        registerLayer(ContainerLayer.getInstance());
        registerLayer(HotspotLayer.getInstance());
        registerLayer(QuarkusLayer.getInstance());
        registerLayer(SemeruLayer.getInstance());
    }

    public static LayerRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a layer as a singleton using its own name.
     */
    private void registerLayer(Layer layer) {
        if (layer == null) {
            throw new IllegalArgumentException("layer must not be null");
        }
        String name = layer.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("layer name must not be null or blank");
        }
        layers.putIfAbsent(name, layer);
    }

    /**
     * Returns the layer by name, or null if not found.
     */
    public Layer getLayer(String layerName) {
        return layers.containsKey(layerName) ? layers.get(layerName) : null;
    }
}
