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
        dailyTerm.setDurationInDays(1.0);
        dailyTerm.setDurationThreshold("30 min");
        dailyTerm.setPlotsDatapoint(4);
        dailyTerm.setPlotsDatapointDeltaInDays(0.25);
        defaultMap.put("daily", dailyTerm);

        // Weekly Term (~29% threshold)
        TermDefinition weeklyTerm = new TermDefinition();
        weeklyTerm.setDurationInDays(7.0);
        weeklyTerm.setDurationThreshold("2 days");
        weeklyTerm.setPlotsDatapoint(7);
        weeklyTerm.setPlotsDatapointDeltaInDays(1.0);
        defaultMap.put("weekly", weeklyTerm);

        // 15 days Term (~50% threshold)
        TermDefinition fifteenDayTerm = new TermDefinition();
        fifteenDayTerm.setDurationInDays(15.0);
        fifteenDayTerm.setDurationThreshold("7 days");
        fifteenDayTerm.setPlotsDatapoint(15);
        fifteenDayTerm.setPlotsDatapointDeltaInDays(1.0);
        defaultMap.put("15 days", fifteenDayTerm);

        // Monthly Term (~70% threshold, rationalized)
        TermDefinition monthlyTerm = new TermDefinition();
        monthlyTerm.setDurationInDays(30.0);
        monthlyTerm.setDurationThreshold("21 days"); // 30 days * 0.70 = 21 days
        monthlyTerm.setPlotsDatapoint(30);
        monthlyTerm.setPlotsDatapointDeltaInDays(1.0);
        defaultMap.put("monthly", monthlyTerm);

        // Quarterly Term (~70% threshold, rationalized)
        TermDefinition quarterlyTerm = new TermDefinition();
        quarterlyTerm.setDurationInDays(90.0);
        quarterlyTerm.setDurationThreshold("63 days"); // 90 days * 0.70 = 63 days
        quarterlyTerm.setPlotsDatapoint(90);
        quarterlyTerm.setPlotsDatapointDeltaInDays(1.0);
        defaultMap.put("quarterly", quarterlyTerm);

        // Half-Yearly Term (~70% threshold, rationalized)
        TermDefinition halfYearlyTerm = new TermDefinition();
        halfYearlyTerm.setDurationInDays(180.0);
        halfYearlyTerm.setDurationThreshold("126 days"); // 180 days * 0.70 = 126 days
        halfYearlyTerm.setPlotsDatapoint(180);
        halfYearlyTerm.setPlotsDatapointDeltaInDays(1.0);
        defaultMap.put("half_yearly", halfYearlyTerm);

        // Yearly Term (~70% threshold, rationalized)
        TermDefinition yearlyTerm = new TermDefinition();
        yearlyTerm.setDurationInDays(365.0);
        yearlyTerm.setDurationThreshold("256 days"); // 365 days * 0.70 = 255.5, rounded up
        yearlyTerm.setPlotsDatapoint(365);
        yearlyTerm.setPlotsDatapointDeltaInDays(1.0);
        defaultMap.put("yearly", yearlyTerm);

        defaultMap.put("short_term", dailyTerm);
        defaultMap.put("medium_term", weeklyTerm);
        defaultMap.put("long_term", fifteenDayTerm);

        // Make the final map unmodifiable to prevent changes at runtime
        DEFAULTS = Collections.unmodifiableMap(defaultMap);
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private TermDefaults() {}
}