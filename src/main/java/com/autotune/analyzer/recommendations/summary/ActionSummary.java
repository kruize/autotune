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

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class ActionSummary {
    @SerializedName(KruizeConstants.JSONKeys.IDLE)
    private HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> idle;
    @SerializedName(KruizeConstants.JSONKeys.OPTIMIZED)
    private HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> optimized;
    @SerializedName(KruizeConstants.JSONKeys.CRITICAL)
    private HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> critical;
    @SerializedName(KruizeConstants.JSONKeys.OPTIMIZABLE)
    private HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> optimizable;
    @SerializedName(KruizeConstants.JSONKeys.ERROR)
    private HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> error;
    @SerializedName(KruizeConstants.JSONKeys.INFO)
    private HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> info;
    @SerializedName(KruizeConstants.JSONKeys.TOTAL)
    private HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> total;

    public ActionSummary() {
        this.idle = new HashMap<>();
        this.optimized = new HashMap<>();
        this.critical = new HashMap<>();
        this.optimizable = new HashMap<>();
        this.error = new HashMap<>();
        this.info = new HashMap<>();
        this.total = new HashMap<>();
    }

    public HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> getIdle() {
        return idle;
    }

    public void setIdle(HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> idle) {
        this.idle = idle;
    }

    public HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> getOptimized() {
        return optimized;
    }

    public void setOptimized(HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> optimized) {
        this.optimized = optimized;
    }

    public HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> getCritical() {
        return critical;
    }

    public void setCritical(HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> critical) {
        this.critical = critical;
    }

    public HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> getOptimizable() {
        return optimizable;
    }

    public void setOptimizable(HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> optimizable) {
        this.optimizable = optimizable;
    }

    public HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> getError() {
        return error;
    }

    public void setError(HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> error) {
        this.error = error;
    }

    public HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> getInfo() {
        return info;
    }

    public void setInfo(HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> info) {
        this.info = info;
    }

    public HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> getTotal() {
        return total;
    }

    public void setTotal(HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> total) {
        this.total = total;
    }

    public ActionSummary merge(ActionSummary other) {
        mergeMap(idle, other.getIdle());
        mergeMap(optimized, other.getOptimized());
        mergeMap(critical, other.getCritical());
        mergeMap(optimizable, other.getOptimizable());
        mergeMap(error, other.getError());
        mergeMap(info, other.getInfo());
        mergeMap(total, other.getTotal());
        return this;
    }

    private void mergeMap(HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> original, HashMap<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> toMerge) {
        for (HashMap.Entry<AnalyzerConstants.ActionSummaryRecommendationItem, ResourceInfo> entry : toMerge.entrySet()) {
            AnalyzerConstants.ActionSummaryRecommendationItem key = entry.getKey();
            ResourceInfo value = entry.getValue();

            ResourceInfo originalValue = original.get(key);
            if (originalValue == null) {
                original.put(key, value);
            } else {
                originalValue.setCount(originalValue.getCount() + value.getCount());
                originalValue.getWorkloadNames().addAll(value.getWorkloadNames());
            }
        }
    }
    @Override
    public String toString() {
        return "ActionSummary{" +
                "idle=" + idle +
                ", optimized=" + optimized +
                ", critical=" + critical +
                ", optimizable=" + optimizable +
                ", error=" + error +
                ", info=" + info +
                ", total=" + total +
                '}';
    }
}