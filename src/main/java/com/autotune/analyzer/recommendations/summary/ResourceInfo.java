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

import java.util.HashSet;
import java.util.Set;

public class ResourceInfo {
    private int count;
    private Set<String> workloadNames;


    public ResourceInfo(int count, Set<String> workloadNames) {
        this.count = count;
        this.workloadNames = workloadNames;
    }

    public ResourceInfo() {
        this.count = 0;
        this.workloadNames = new HashSet<>();
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Set<String> getWorkloadNames() {
        return workloadNames;
    }

    public void setWorkloadNames(Set<String> workloadNames) {
        this.workloadNames = workloadNames;
    }

    @Override
    public String toString() {
        return "ResourceInfo{" +
                "count=" + count +
                ", workloadNames=" + workloadNames +
                '}';
    }
}
