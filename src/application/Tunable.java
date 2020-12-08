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

public class Tunable
{
    String name;
    String details;
    String upperBound;
    String lowerBound;
    String valueType;

    public Tunable(String name, String details, String upperBound, String lowerBound) {
        this.name = name;
        this.details = details;
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    public Tunable(String name, String details, String upperBound, String lowerBound, String valueType)
    {
        this.name = name;
        this.details = details;
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
        this.valueType = valueType;
    }

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

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
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
                ", details='" + details + '\'' +
                ", upperBound='" + upperBound + '\'' +
                ", lowerBound='" + lowerBound + '\'' +
                '}';
    }
}
