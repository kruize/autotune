package com.autotune.experimentManager.data.result;

import com.autotune.experimentManager.utils.EMUtil;

import java.util.Date;
import java.util.LinkedHashMap;

public class TrialIterationMetaData {
    private Date beginTimestamp;
    private Date endTimestamp;
    private EMUtil.EMExpStatus status;
    private int iterationNumber;
    private LinkedHashMap<String, StepsMetaData> workFlow;

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

    public LinkedHashMap<String, StepsMetaData> getWorkFlow() {
        return workFlow;
    }

    public void setWorkFlow(LinkedHashMap<String, StepsMetaData> workFlow) {
        this.workFlow = workFlow;
    }

    public int getIterationNumber() {
        return iterationNumber;
    }

    public void setIterationNumber(int iterationNumber) {
        this.iterationNumber = iterationNumber;
    }
}
