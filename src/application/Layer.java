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
 * A Layer is defined by its config file, and can contain multiple queries.
 */
public class Layer
{
    String level;
    String name;
    String details;
    ArrayList<Query> queries;

    public Layer(String name, String level, String details) {
        this.name = name;
        this.level = level;
        this.details = details;

        queries = new ArrayList<>();
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Query> getQueries() {
        return queries;
    }

    public void setQueries(ArrayList<Query> queries) {
        this.queries = queries;
    }

    @Override
    public String toString() {
        return "Layer{" +
                "level='" + level + '\'' +
                ", name='" + name + '\'' +
                ", details='" + details + '\'' +
                ", queries=" + queries +
                '}';
    }
}
