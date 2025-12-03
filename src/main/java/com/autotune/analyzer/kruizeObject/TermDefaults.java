/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.autotune.analyzer.kruizeObject;

import static com.autotune.utils.KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.DurationAmount.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class that holds the default configurations for all predefined recommendation terms.
 * This provides a single source of truth for term settings like duration, data thresholds, and plotting parameters.
 */
public final class TermDefaults {

    /**
     * An unmodifiable, static map containing the default TermDefinition for each standard term.
     * The keys are the lowercase names of the user-facing terms (e.g., "daily", "weekly").
     */
    public static final Map<String, TermDefinition> DEFAULTS;

    static {
        Map<String, TermDefinition> defaultMap = new HashMap<>();

        // Daily Term (~2% threshold)
        TermDefinition dailyTerm = new TermDefinition();
        dailyTerm.setDurationInDays(DURATION_DAILY);
        dailyTerm.setDurationThreshold(THRESHOLD_DAILY);
        dailyTerm.setPlotsDatapoint(PLOTS_DATAPOINT_DAILY);
        dailyTerm.setPlotsDatapointDeltaInDays(PLOTS_DELTA_DAILY);
        defaultMap.put(TERM_DAILY, dailyTerm);

        // Weekly Term (~29% threshold)
        TermDefinition weeklyTerm = new TermDefinition();
        weeklyTerm.setDurationInDays(DURATION_WEEKLY);
        weeklyTerm.setDurationThreshold(THRESHOLD_WEEKLY);
        weeklyTerm.setPlotsDatapoint(PLOTS_DATAPOINT_WEEKLY);
        weeklyTerm.setPlotsDatapointDeltaInDays(PLOTS_DELTA_GENERIC);
        defaultMap.put(TERM_WEEKLY, weeklyTerm);

        // 15 days Term (~50% threshold)
        TermDefinition fifteenDayTerm = new TermDefinition();
        fifteenDayTerm.setDurationInDays(DURATION_15_DAYS);
        fifteenDayTerm.setDurationThreshold(THRESHOLD_15_DAYS);
        fifteenDayTerm.setPlotsDatapoint(PLOTS_DATAPOINT_15_DAYS);
        fifteenDayTerm.setPlotsDatapointDeltaInDays(PLOTS_DELTA_GENERIC);
        defaultMap.put(TERM_15_DAYS, fifteenDayTerm);

        // Monthly Term (~70% threshold, rationalized)
        TermDefinition monthlyTerm = new TermDefinition();
        monthlyTerm.setDurationInDays(DURATION_MONTHLY);
        monthlyTerm.setDurationThreshold(THRESHOLD_MONTHLY); // 30 days * 0.70 = 21 days
        monthlyTerm.setPlotsDatapoint(PLOTS_DATAPOINT_MONTHLY);
        monthlyTerm.setPlotsDatapointDeltaInDays(PLOTS_DELTA_GENERIC);
        defaultMap.put(TERM_MONTHLY, monthlyTerm);

        // Quarterly Term (~70% threshold, rationalized)
        TermDefinition quarterlyTerm = new TermDefinition();
        quarterlyTerm.setDurationInDays(DURATION_QUARTERLY);
        quarterlyTerm.setDurationThreshold(THRESHOLD_QUARTERLY); // 90 days * 0.70 = 63 days
        quarterlyTerm.setPlotsDatapoint(PLOTS_DATAPOINT_QUARTERLY);
        quarterlyTerm.setPlotsDatapointDeltaInDays(PLOTS_DELTA_GENERIC);
        defaultMap.put(TERM_QUARTERLY, quarterlyTerm);

        // Half-Yearly Term (~70% threshold, rationalized)
        TermDefinition halfYearlyTerm = new TermDefinition();
        halfYearlyTerm.setDurationInDays(DURATION_HALF_YEARLY);
        halfYearlyTerm.setDurationThreshold(THRESHOLD_HALF_YEARLY); // 180 days * 0.70 = 126 days
        halfYearlyTerm.setPlotsDatapoint(PLOTS_DATAPOINT_HALF_YEARLY);
        halfYearlyTerm.setPlotsDatapointDeltaInDays(PLOTS_DELTA_GENERIC);
        defaultMap.put(TERM_HALF_YEARLY, halfYearlyTerm);

        // Yearly Term (~70% threshold, rationalized)
        TermDefinition yearlyTerm = new TermDefinition();
        yearlyTerm.setDurationInDays(DURATION_YEARLY);
        yearlyTerm.setDurationThreshold(THRESHOLD_YEARLY); // 365 days * 0.70 = 255.5, rounded up
        yearlyTerm.setPlotsDatapoint(PLOTS_DATAPOINT_YEARLY);
        yearlyTerm.setPlotsDatapointDeltaInDays(PLOTS_DELTA_GENERIC);
        defaultMap.put(TERM_YEARLY, yearlyTerm);

        defaultMap.put(TERM_SHORT, dailyTerm);
        defaultMap.put(TERM_MEDIUM, weeklyTerm);
        defaultMap.put(TERM_LONG, fifteenDayTerm);

        // Make the final map unmodifiable to prevent changes at runtime
        DEFAULTS = Collections.unmodifiableMap(defaultMap);
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private TermDefaults() {}
}