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
 * Contains the tunable to optimize, along with its upper and lower bounds, value type
 * and the list of sla_class (throughput, response_time, right_size, etc.) for which it is applicable.
 *
 * Example:
 * - name: <Tunable>
 *   value_type: double
 *   upper_bound: '4.0'
 *   lower_bound: '2.0'
 *   sla_class:
 *   - response_time
 *   - throughput
 */
public class Tunable
{
    String name;
    String upperBound;
    String lowerBound;
    String valueType;

    public ArrayList<String> slaClassList;

    public Tunable(String name,
                   String upperBound,
                   String lowerBound,
                   String valueType,
                   ArrayList<String> slaClassList) {
        this.name = name;
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
        this.valueType = valueType;
        this.slaClassList = slaClassList;
    }

    public Tunable() { }

    public Tunable(String name)
    {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(String upperBound) {
        this.upperBound = upperBound;
    }

    public String getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(String lowerBound) {
        this.lowerBound = lowerBound;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    @Override
    public String toString() {
        return "Tunable{" +
                "name='" + name + '\'' +
                ", upperBound='" + upperBound + '\'' +
                ", lowerBound='" + lowerBound + '\'' +
                ", valueType='" + valueType + '\'' +
                ", slaClassList=" + slaClassList +
                '}';
    }
}
