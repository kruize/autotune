/*******************************************************************************
 * Copyright (c) 2020, 2022 Red Hat, IBM Corporation and others.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *    http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.common.data.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AggregationFunctions {

    private String function;
    private String query;
    private List<String> queryParams;
    @SerializedName("result_columns")
    private List<String> resultColumns;
    private String version;

    public AggregationFunctions(String function, String query, String version, List<String> query_params) {
        this.function = function;
        this.query = query;
        this.version = version;
        this.queryParams = query_params;
    }

    public List<String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(List<String> queryParams) {
        this.queryParams = queryParams;
    }
// to support fetching result columns from profile creation payload start 
    public List<String> getResultColumns() {
        return resultColumns;
    }

    public void setResultColumns(List<String> resultColumns) {
        this.resultColumns = resultColumns;
    }
// to support fetching result columns from profile creation payload end 
    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "AggregationFunctions{" +
                "function='" + function + '\'' +
                ", query='" + query + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
