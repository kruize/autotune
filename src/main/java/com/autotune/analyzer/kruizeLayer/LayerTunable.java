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

import java.util.List;
import java.util.Objects;

public class LayerTunable {
    public String layerName;
    public List<Tunable> tunables;

    public LayerTunable(String layerName, List<Tunable> tunables) {
        this.layerName = layerName;
        this.tunables = tunables;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public List<Tunable> getTunables() {
        return tunables;
    }

    public void setTunables(List<Tunable> tunables) {
        this.tunables = tunables;
    }

    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LayerTunable that = (LayerTunable) o;
        return Objects.equals(layerName, that.layerName) &&
                Objects.equals(tunables, that.tunables);
    }

    public int hashCode() {
        return Objects.hash(layerName, tunables);
    }
}
