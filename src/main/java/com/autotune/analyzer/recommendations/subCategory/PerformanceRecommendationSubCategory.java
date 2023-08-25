/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.recommendations.subCategory;

import java.util.concurrent.TimeUnit;

public class PerformanceRecommendationSubCategory implements RecommendationSubCategory {
    private String name;
    private int duration;
    private TimeUnit recommendationDurationUnits;
    private double durationUpperBound;
    private double durationLowerBound;

    public PerformanceRecommendationSubCategory(String name,
                                         int duration,
                                         TimeUnit recommendationDurationUnits,
                                         double durationUpperBound,
                                         double durationLowerBound) {
        this.name = name;
        this.duration = duration;
        this.recommendationDurationUnits = recommendationDurationUnits;
        this.durationUpperBound = durationUpperBound;
        this.durationLowerBound = durationLowerBound;
    }

    // Adding private constructor to avoid object creation without passing any attributes
    private PerformanceRecommendationSubCategory() {

    }

    public int getDuration() {
        return this.duration;
    }

    public TimeUnit getRecommendationDurationUnits() {
        return this.recommendationDurationUnits;
    }

    @Override
    public String getSubCategory() {
        return this.name;
    }

    public String getName() {
        return name;
    }

    public double getDurationUpperBound() {
        return durationUpperBound;
    }

    public double getGetDurationLowerBound() {
        return durationLowerBound;
    }
}
