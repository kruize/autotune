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

import com.autotune.experimentManager.utils.EMUtil;

import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * Holds trial level details like status and Timestamp
 * Trial contains Cycle workflows and trial level workflows
 * Cycle are Warmup and measurement
 * and each cycle will have iterations
 * and each Iteration will perform steps like pre/post Validation , Deployment,Metric collections , post results etc.
 */
public class TrialMetaData {
    private Timestamp creationDate;
    private Date beginTimestamp;
    private Date endTimestamp;
    private LinkedHashMap<Integer, TrialIterationMetaData> iterations;
    private EMUtil.EMExpStatus status;
    private LinkedHashMap<String, StepsMetaData> trialWorkflow;

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

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

    public EMUtil.EMExpStatus getStatus() {
        if (status == null) status = EMUtil.EMExpStatus.QUEUED;
        return status;
    }

    public void setStatus(EMUtil.EMExpStatus status) {
        if (status == null) status = EMUtil.EMExpStatus.QUEUED;
        this.status = status;
    }

    public LinkedHashMap<String, StepsMetaData> getTrialWorkflow() {
        return trialWorkflow;
    }

    public void setTrialWorkflow(LinkedHashMap<String, StepsMetaData> trialWorkflow) {
        this.trialWorkflow = trialWorkflow;
    }

    public LinkedHashMap<Integer, TrialIterationMetaData> getIterations() {
        return iterations;
    }

    public void setIterations(LinkedHashMap<Integer, TrialIterationMetaData> iterations) {
        this.iterations = iterations;
    }
}
