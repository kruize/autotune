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


import java.sql.Timestamp;

/**
 * Holds experiment level details like
 * status and timestamps etc
 */

public class ExperimentMetaData {
    private Timestamp creationDate;
    private Timestamp beginTimestamp;
    private Timestamp endTimestamp;
    private transient AutoTuneWorkFlow autoTuneWorkFlow;

    public Timestamp getBeginTimestamp() {
        return beginTimestamp;
    }

    public void setBeginTimestamp(Timestamp beginTimestamp) {
        this.beginTimestamp = beginTimestamp;
    }

    public Timestamp getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(Timestamp endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public AutoTuneWorkFlow getAutoTuneWorkFlow() {
        return autoTuneWorkFlow;
    }

    public void setAutoTuneWorkFlow(AutoTuneWorkFlow autoTuneWorkFlow) {
        this.autoTuneWorkFlow = autoTuneWorkFlow;
    }

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public String toString() {
        return "ExperimentMetaData{" +
                ", beginTimestamp=" + beginTimestamp +
                ", endTimestamp=" + endTimestamp +
                ", autoTuneWorkFlow=" + autoTuneWorkFlow +
                '}';
    }
}
