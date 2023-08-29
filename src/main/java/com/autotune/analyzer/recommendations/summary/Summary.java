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

package com.autotune.analyzer.recommendations.summary;

import java.sql.Timestamp;
import java.util.HashMap;

/**
 * stores the summarized recommendation data
 */
public class Summary {
    private HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data;

    public HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> getData() {
        return data;
    }

    public void setData(HashMap<Timestamp, HashMap<String, HashMap<String, RecommendationSummary>>> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Summary{" +
                "data=" + data +
                '}';
    }
}
