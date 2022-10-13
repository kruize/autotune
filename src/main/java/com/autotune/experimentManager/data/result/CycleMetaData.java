/*******************************************************************************
 * Copyright (c)  2022 Red Hat, IBM Corporation and others.
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
package com.autotune.experimentManager.data.result;

import com.autotune.common.data.metrics.EMMetricResult;
import com.autotune.experimentManager.utils.EMUtil;

import java.util.Date;

/**
 * Warmup and MeasurementCycle details like status and timestamp
 */
public class CycleMetaData {
    private String cycleName;
    private Date beginTimestamp;
    private Date endTimestamp;
    private EMUtil.EMExpStatus status;
    private EMMetricResult emMetricResult;


    public Date getBeginTimestamp() {
        return beginTimestamp;
    }

    public void setBeginTimestamp(Date beginTimestamp) {
        this.beginTimestamp = beginTimestamp;
    }

    public Date getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(Date endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public EMUtil.EMExpStatus getStatus() {
        return status;
    }

    public void setStatus(EMUtil.EMExpStatus status) {
        this.status = status;
    }

    public String getCycleName() {
        return cycleName;
    }

    public void setCycleName(String cycleName) {
        this.cycleName = cycleName;
    }

    public EMMetricResult getEmMetricResult() {
        return emMetricResult;
    }

    public void setEmMetricResult(EMMetricResult emMetricResult) {
        this.emMetricResult = emMetricResult;
    }
}
