/*******************************************************************************
 * Copyright (c) 2020 Red Hat, IBM Corporation and others.
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
package com.autotune.application;

import java.util.ArrayList;

/**
 * Contains the query for a datasource, along with its details, value type and the list of tunables
 * affected by the aforementioned query
 *
 * Example:
 *   - query_details: Current CPU used
 *     datasource:
 *       - name: 'prometheus'
 *         query: '(container_cpu_usage_seconds_total{$CONTAINER_LABEL$!="POD", $POD_LABEL$="$POD$"}[1m])'
 *       - name: 'graphite'
 *         query: ''
 *     value_type: double
 *     tunables:
 *       - name: <Tunable>
 *         value_type: double
 *         upper_bound: '4.0'
 *         lower_bound: '2.0'
 *         sla_class:
 *           - response_time
 *           - throughput
 */
public class Query
{
    String details;
    String query;
    String valueType;
    ArrayList<Tunable> tunables;

    public Query(String details, String query, String valueType)
    {
        this.details = details;
        this.query = query;
        this.valueType = valueType;
        tunables = new ArrayList<>();
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public ArrayList<Tunable> getTunables() {
        return tunables;
    }

    public void setTunables(ArrayList<Tunable> tunables) {
        this.tunables = tunables;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    @Override
    public String toString() {
        return "Query{" +
                "details='" + details + '\'' +
                ", query='" + query + '\'' +
                ", valueType='" + valueType + '\'' +
                ", tunables=" + tunables +
                '}';
    }
}
