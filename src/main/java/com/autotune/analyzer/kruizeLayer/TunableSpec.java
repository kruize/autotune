package com.autotune.analyzer.kruizeLayer;

import java.util.Objects;

public final class TunableSpec {

    private final String layerName;
    private final String tunableName;

    public TunableSpec(String layerName, String tunableName) {
        this.layerName = layerName;
        this.tunableName = tunableName;
    }

    public String getLayerName() {
        return layerName;
    }

    public String getTunableName() {
        return tunableName;
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
    public int hashCode() {
        return Objects.hash(layerName, tunableName);
    }

    @Override
    public String toString() {
        return layerName + ":" + tunableName;
    }
}

