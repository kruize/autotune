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
 * A single Query can affect multiple tunables
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
