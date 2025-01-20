/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.kruizeObject;

import com.autotune.utils.KruizeConstants;

import java.util.HashMap;

public class RecommendationSettings {
    private Double threshold;

    /**
     * A map to store the custom minimum duration (in minutes) provided by user for recommendation terms
     * The keys represent the recommendation terms (e.g., "short_term"), and the values are the corresponding minimum durations in minutes.
     */
    private HashMap<String, Double> minDurationInMins;

    public Double getThreshold() {
        return threshold;
    }

    public HashMap<String, Double> getMinDurationInMins() {
        return minDurationInMins;
    }

    public void setMinDurationInMins(HashMap<String, Double> minDurationInMins) {
        this.minDurationInMins = minDurationInMins;
    }

    // converts minimum duration required for terms in minutes to days
    public double getThresholdForTerm(String term) {
        return minDurationInMins.get(term) / (KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY * KruizeConstants.TimeConv.NO_OF_MINUTES_PER_HOUR);
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    @Override
    public String toString() {
        return "RecommendationSettings{" +
                "threshold=" + threshold +
                '}';
    }
}
