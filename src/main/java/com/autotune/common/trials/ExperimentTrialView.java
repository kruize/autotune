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

package com.autotune.common.trials;

import com.autotune.experimentManager.data.result.TrialMetaData;
import com.autotune.experimentManager.utils.EMUtil;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Model class for listTrialStatus view API
 */
public class ExperimentTrialView {
    private EMUtil.EMExpStatus status;
    private Timestamp creationDate;
    private Timestamp beginTimeStamp;
    private Timestamp endTimeStamp;
    private transient LinkedList<String> steps;
    private LinkedHashMap<String, TrialMetaData> trialDetails;

    public EMUtil.EMExpStatus getStatus() {
        return status;
    }

    public void setStatus(EMUtil.EMExpStatus status) {
        this.status = status;
    }

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

    public Timestamp getBeginTimeStamp() {
        return beginTimeStamp;
    }

    public void setBeginTimeStamp(Timestamp beginTimeStamp) {
        this.beginTimeStamp = beginTimeStamp;
    }

    public LinkedList<String> getSteps() {
        return steps;
    }

    public void setSteps(LinkedList<String> steps) {
        this.steps = steps;
    }

    public LinkedHashMap<String, TrialMetaData> getTrialDetails() {
        return trialDetails;
    }

    public void setTrialDetails(LinkedHashMap<String, TrialMetaData> trialDetails) {
        this.trialDetails = trialDetails;
    }

    public Timestamp getEndTimeStamp() {
        return endTimeStamp;
    }

    public void setEndTimeStamp(Timestamp endTimeStamp) {
        this.endTimeStamp = endTimeStamp;
    }

}
