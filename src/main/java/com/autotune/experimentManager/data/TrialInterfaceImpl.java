/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.experimentManager.data;

import com.autotune.common.trials.ExperimentTrial;
import com.autotune.common.trials.TrialDetails;
import com.autotune.experimentManager.data.result.ExperimentMetaData;
import com.autotune.experimentManager.data.result.TrialMetaData;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Experiment Trials Local storage Data access implementation .
 */
public class TrialInterfaceImpl implements TrialInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrialInterfaceImpl.class);
    private final ExperimentDetailsMap<String, ExperimentTrial> EMExperimentTrialMap;

    private String errorMessage;
    private int httpResponseCode;

    public TrialInterfaceImpl(ExperimentDetailsMap<String, ExperimentTrial> EMExperimentTrialMap) {
        this.EMExperimentTrialMap = EMExperimentTrialMap;
    }

    /**
     * * AddExperiment into local storage
     * * Set Error if duplicate trials found.
     * * Set Error if duplicate deployments found.
     */
    @Override
    public void addExperiments(List<ExperimentTrial> experimentTrialList) {
        List<ExperimentTrial> validExpTrialList = new ArrayList<>();
        List<ExperimentTrial> successExperimentTrials = new ArrayList<>();
        HashMap<String, HashMap<String, TrialDetails>> successTrialDetailsList = new HashMap<>();
        experimentTrialList.forEach(
                (experimentTrial) -> {
                    Object obj = this.EMExperimentTrialMap.get(experimentTrial.getExperimentName());
                    if (obj == null)
                        successExperimentTrials.add(experimentTrial);
                    else {
                        HashMap<String, TrialDetails> trialDetailsHashMap = experimentTrial.getTrialDetails();
                        if (null != trialDetailsHashMap) {
                            ExperimentTrial existingExpTrial = (ExperimentTrial) obj;
                            HashMap<String, TrialDetails> existingTrialDetailsMap = existingExpTrial.getTrialDetails();
                            Set<String> commonTrials = existingTrialDetailsMap.keySet().stream().filter(trialDetailsHashMap.keySet()::contains).collect(Collectors.toSet());
                            if (commonTrials.size() == 0) {
                                trialDetailsHashMap.forEach((trialNumber, trailDetails) -> {
                                    if (null == successTrialDetailsList.get(experimentTrial.getExperimentName()))
                                        successTrialDetailsList.put(experimentTrial.getExperimentName(), new HashMap<>());
                                    successTrialDetailsList.get(experimentTrial.getExperimentName()).put(trialNumber, trailDetails);
                                });
                            } else {
                                this.setHttpResponseCode(400);
                                this.setErrorMessage("Following trials : " + commonTrials + " Already exist for Experiment :  " + experimentTrial.getExperimentName());
                                return;
                            }
                        } else {
                            this.setHttpResponseCode(400);
                            this.setErrorMessage("Experiment already exist");
                            return;
                        }
                    }
                }
        );
        if (null == this.getErrorMessage()) {
            successExperimentTrials.forEach(
                    (expTrial) -> {
                        expTrial.setStatus(EMUtil.EMExpStatus.QUEUED);
                        ExperimentMetaData experimentMetaData = new ExperimentMetaData();
                        experimentMetaData.setCreationDate(new Timestamp(System.currentTimeMillis()));
                        expTrial.setExperimentMetaData(experimentMetaData);
                        expTrial.getTrialDetails().forEach((trialNumber, trailDetails) -> {
                            TrialMetaData trialMetaData = new TrialMetaData();
                            trialMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                            trialMetaData.setCreationDate(new Timestamp(System.currentTimeMillis()));
                            trailDetails.setTrialMetaData(trialMetaData);
                            trailDetails.setTrialNumber(trialNumber);
                        });
                        this.EMExperimentTrialMap.put(expTrial.getExperimentName(), expTrial);
                    }
            );
            successTrialDetailsList.forEach(
                    (expName, trialDetails) -> {
                        ExperimentTrial existingExpTrial = (ExperimentTrial) this.EMExperimentTrialMap.get(expName);
                        trialDetails.forEach((trialNumber, trailDetails) -> {
                            trailDetails.setTrialNumber(trialNumber);
                            TrialMetaData trialMetaData = new TrialMetaData();
                            trialMetaData.setCreationDate(new Timestamp(System.currentTimeMillis()));
                            existingExpTrial.setStatus(EMUtil.EMExpStatus.QUEUED);
                            trialMetaData.setStatus(EMUtil.EMExpStatus.QUEUED);
                            trailDetails.setTrialMetaData(trialMetaData);
                            existingExpTrial.getTrialDetails().put(trialNumber, trailDetails);
                        });
                    }
            );
        }
    }

    @Override
    public ExperimentDetailsMap<String, ExperimentTrial> listExperiments() {
        return this.EMExperimentTrialMap;
    }

    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public int getHttpResponseCode() {
        return this.httpResponseCode;
    }

    public void setHttpResponseCode(int httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
    }
}
