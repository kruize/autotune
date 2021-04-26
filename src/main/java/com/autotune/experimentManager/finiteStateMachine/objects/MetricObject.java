/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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

package com.autotune.experimentManager.finiteStateMachine.objects;

/**
 * MetricObject hold the properties of the experiment result.
 */

public class MetricObject {
    private String name;
    private String query;
    private String dataSource;
    private float score;
    private float error;
    private float mean;
    private float mode;
    private float percentile95;
    private float percentile99;
    private float percentile99Point9;
    private float percentile99Point99;
    private float percentile99Point999;
    private float percentile99Point9999;
    private float percentile100;
    private float spike;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getQuery() {
        return query;
    }
    public void setQuery(String query) {
        this.query = query;
    }
    public String getDataSource() {
        return dataSource;
    }
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
    public float getScore() {
        return score;
    }
    public void setScore(float score) {
        this.score = score;
    }
    public float getError() {
        return error;
    }
    public void setError(float error) {
        this.error = error;
    }
    public float getMean() {
        return mean;
    }
    public void setMean(float mean) {
        this.mean = mean;
    }
    public float getMode() {
        return mode;
    }
    public void setMode(float mode) {
        this.mode = mode;
    }
    public float getPercentile95() {
        return percentile95;
    }
    public void setPercentile95(float percentile95) {
        this.percentile95 = percentile95;
    }
    public float getPercentile99() {
        return percentile99;
    }
    public void setPercentile99(float percentile99) {
        this.percentile99 = percentile99;
    }
    public float getPercentile99Point9() {
        return percentile99Point9;
    }
    public void setPercentile99Point9(float percentile99Point9) {
        this.percentile99Point9 = percentile99Point9;
    }
    public float getPercentile99Point99() {
        return percentile99Point99;
    }
    public void setPercentile99Point99(float percentile99Point99) {
        this.percentile99Point99 = percentile99Point99;
    }
    public float getPercentile99Point999() {
        return percentile99Point999;
    }
    public void setPercentile99Point999(float percentile99Point999) {
        this.percentile99Point999 = percentile99Point999;
    }
    public float getPercentile99Point9999() {
        return percentile99Point9999;
    }
    public void setPercentile99Point9999(float percentile99Point9999) {
        this.percentile99Point9999 = percentile99Point9999;
    }
    public float getPercentile100() {
        return percentile100;
    }
    public void setPercentile100(float percentile100) {
        this.percentile100 = percentile100;
    }
    public float getSpike() {
        return spike;
    }
    public void setSpike(float spike) {
        this.spike = spike;
    }
}
