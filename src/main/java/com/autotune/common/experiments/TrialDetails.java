/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.experiments;

import com.autotune.common.k8sObjects.Metric;
import com.autotune.experimentManager.utils.EMUtil;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;

/**
 * A data util used to hold detailed information about trials.
 */
public class TrialDetails {
    /**
     *  Config related to request, limits , env etc
     */
    @SerializedName("config")
    private ContainerConfigData configData ;

    private String state;
    private String result;
    private String resultInfo;
    private String resultError;
    private Timestamp startTime;
    private Timestamp endTime;
    private EMUtil.EMExpStatus status = EMUtil.EMExpStatus.QUEUED;

    public TrialDetails(ContainerConfigData configData) {
        this.configData = configData;
    }

    public TrialDetails(String state,
                        String result,
                        String resultInfo,
                        String resultError
    ) {

        this.state = state;
        this.result = result;
        this.resultInfo = resultInfo;
        this.resultError = resultError;
    }



    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResultInfo() {
        return resultInfo;
    }

    public void setResultInfo(String resultInfo) {
        this.resultInfo = resultInfo;
    }

    public String getResultError() {
        return resultError;
    }

    public void setResultError(String resultError) {
        this.resultError = resultError;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public ContainerConfigData getConfigData() {
        return configData;
    }

    public EMUtil.EMExpStatus getStatus() {
        if (status == null) status = EMUtil.EMExpStatus.QUEUED;
        return status;
    }

    public void setStatus(EMUtil.EMExpStatus status) {
        if (status == null) status = EMUtil.EMExpStatus.QUEUED;
        this.status = status;
    }

    @Override
    public String toString() {
        return "TrialDetails{" +
                "configData=" + configData +
                ", state='" + state + '\'' +
                ", result='" + result + '\'' +
                ", resultInfo='" + resultInfo + '\'' +
                ", resultError='" + resultError + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
