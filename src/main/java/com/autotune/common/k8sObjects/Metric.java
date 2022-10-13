/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.k8sObjects;

import com.autotune.common.annotations.json.Exclude;
import com.autotune.common.data.metrics.EMMetricResult;

import java.util.LinkedHashMap;

/**
 * Holds the variables used in the objective_function for the autotune object
 * objective_function: "transaction_response_time"
 * function_variables:
 * - name: "transaction_response_time"
 * query: "application_org_acme_microprofile_metrics_PrimeNumberChecker_checksTimer_mean_seconds"
 * datasource: "prometheus"
 * value_type: "double"
 */
public final class Metric {
    private final String name;
    private final String query;
    private final String datasource;
    private final String valueType;
    private EMMetricResult emMetricResult;
    @Exclude
    private LinkedHashMap<String, LinkedHashMap<Integer, EMMetricResult>> cycleDataMap;

    public Metric(String name,
                  String query,
                  String datasource,
                  String valueType) {
        this.name = name;
        this.query = query;
        this.datasource = datasource;
        this.valueType = valueType;
        this.cycleDataMap = new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getQuery() {
        return query;
    }

    public String getDatasource() {
        return datasource;
    }

    public String getValueType() {
        return valueType;
    }

    public EMMetricResult getEmMetricResult() {
        return emMetricResult;
    }

    public void setEmMetricResult(EMMetricResult emMetricResult) {
        this.emMetricResult = emMetricResult;
    }

    public LinkedHashMap<String, LinkedHashMap<Integer, EMMetricResult>> getCycleDataMap() {
        if (null == cycleDataMap)
            cycleDataMap = new LinkedHashMap<>();
        return cycleDataMap;
    }

    @Override
    public String toString() {
        return "Metric{" +
                "name='" + name + '\'' +
                ", query='" + query + '\'' +
                ", datasource='" + datasource + '\'' +
                ", valueType='" + valueType + '\'' +
                ", emMetricResult=" + emMetricResult +
                '}';
    }
}
