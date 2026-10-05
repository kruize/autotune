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
package com.autotune.analyzer.recommendations;

import com.autotune.analyzer.adapters.MultiResourceRecommendationAdapter;
import com.google.gson.annotations.JsonAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents multiple resource recommendations (Accelerators array)
 * Custom adapter ensures it serializes as array, not object
 */
@JsonAdapter(MultiResourceRecommendationAdapter.class)
public final class MultiResourceRecommendation implements ResourceRecommendation {
    private List<AcceleratorRecommendationItem> acceleratorRecommendationItems;

    public MultiResourceRecommendation() {
        this.acceleratorRecommendationItems = new ArrayList<>();
    }

    public MultiResourceRecommendation(List<AcceleratorRecommendationItem> acceleratorRecommendationItems) {
        this.acceleratorRecommendationItems = acceleratorRecommendationItems;
    }

    public List<AcceleratorRecommendationItem> getAcceleratorRecommendationItems() {
        return acceleratorRecommendationItems;
    }

    public void setAcceleratorRecommendationItems(List<AcceleratorRecommendationItem> acceleratorRecommendationItems) {
        this.acceleratorRecommendationItems = acceleratorRecommendationItems;
    }

    public void addAcceleratorRecommendationItem(AcceleratorRecommendationItem acceleratorRecommendationItem) {
        if (null != acceleratorRecommendationItem)
            acceleratorRecommendationItems.add(acceleratorRecommendationItem);
    }
}
