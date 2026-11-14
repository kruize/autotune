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
package com.autotune.database.helper;

import java.sql.Timestamp;

/**
 * Columns needed to rebuild one kruize_results row on the recommendation hydrate path.
 * {@code extendedDataJson} is the jsonb value as text, parsed once with Gson.
 */
public final class RecommendationResultRow {
    private final String experimentName;
    private final Timestamp intervalStartTime;
    private final Timestamp intervalEndTime;
    private final String version;
    private final String extendedDataJson;

    public RecommendationResultRow(String experimentName,
                                   Timestamp intervalStartTime,
                                   Timestamp intervalEndTime,
                                   String version,
                                   String extendedDataJson) {
        this.experimentName = experimentName;
        this.intervalStartTime = intervalStartTime;
        this.intervalEndTime = intervalEndTime;
        this.version = version;
        this.extendedDataJson = extendedDataJson;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public Timestamp getIntervalStartTime() {
        return intervalStartTime;
    }

    public Timestamp getIntervalEndTime() {
        return intervalEndTime;
    }

    public String getVersion() {
        return version;
    }

    public String getExtendedDataJson() {
        return extendedDataJson;
    }
}
