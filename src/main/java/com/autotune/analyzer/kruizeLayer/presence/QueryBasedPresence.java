/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.kruizeLayer.presence;

import com.autotune.analyzer.kruizeLayer.presence.LayerPresenceQuery;

import java.util.ArrayList;

/**
 * Implementation for query-based layer presence detection
 */
public class QueryBasedPresence implements LayerPresenceDetector {

    private ArrayList<LayerPresenceQuery> queries;

    public QueryBasedPresence() {
        this.queries = new ArrayList<>();
    }

    public QueryBasedPresence(ArrayList<LayerPresenceQuery> queries) {
        this.queries = queries != null ? queries : new ArrayList<>();
    }

    @Override
    public PresenceType getType() {
        return PresenceType.QUERY;
    }

    public ArrayList<LayerPresenceQuery> getQueries() {
        return queries;
    }

    public void setQueries(ArrayList<LayerPresenceQuery> queries) {
        this.queries = queries;
    }

    @Override
    public String toString() {
        return "QueryBasedPresence{" +
                "queries=" + queries +
                '}';
    }
}
