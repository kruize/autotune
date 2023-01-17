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
package com.autotune.common.data.result;

import java.util.HashMap;

public class PodResultData {

    private String name;
    private String datasource;
    private HashMap<String, GeneralInfoResult> summary_results;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public HashMap<String, GeneralInfoResult> getSummary_results() {
        return summary_results;
    }

    public void setSummary_results(HashMap<String, GeneralInfoResult> summary_results) {
        this.summary_results = summary_results;
    }

    @Override
    public String toString() {
        return "PodResultData{" +
                "name='" + name + '\'' +
                ", datasource='" + datasource + '\'' +
                ", pod_metrics=" + summary_results +
                '}';
    }
}
