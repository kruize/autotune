package com.autotune.experimentManager.handler.util;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.experimentManager.data.result.CycleMetaData;
import com.autotune.experimentManager.data.result.StepsMetaData;
import com.autotune.experimentManager.handler.PreValidationHandler;
import com.autotune.experimentManager.utils.EMUtil;
import com.autotune.utils.HttpUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.autotune.experimentManager.core.ExperimentTrialHandler.getDummyMetricJson;

public class EMStatusUpdateHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreValidationHandler.class);

    public static void  updateCycleMetaDataStatus(CycleMetaData cycleMetaData) {
        // Check if all iterationcount's stepsMeta data is Queue if yes set beginTimestamp and update status to Inprogress
        // Check if all Iteration's stepsMeta data is Complete or Failed if yes set EndTimestamp and update status to Complete or Failed
        LinkedHashMap<Integer, LinkedHashMap<String, StepsMetaData>> workflow = cycleMetaData.getIterationWorkflow();
        AtomicInteger queuedCount = new AtomicInteger(0);
        AtomicInteger stepsCount = new AtomicInteger(0);
        AtomicInteger completedOrFailedCount = new AtomicInteger(0);
        workflow.forEach((iterationCount,iterationFlowMetaData) -> {
            LinkedHashMap<String, StepsMetaData> smd =  iterationFlowMetaData;
            stepsCount.set(smd.size());
            smd.forEach((stepsName,stepsMetaData)->{
                if(stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED)){
                    queuedCount.set(queuedCount.get() + 1);
                }else if (stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.COMPLETED)){
                    completedOrFailedCount.set(completedOrFailedCount.get() + 1);
                }else if (stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.FAILED)){
                    completedOrFailedCount.set(completedOrFailedCount.get() + 1);
                }
            });
        });
        int iterationCount = workflow.size() ;
        int stepsCountInt = stepsCount.intValue();
        int total = iterationCount * stepsCountInt ;
        if (queuedCount.intValue() == total){
            cycleMetaData.setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
            cycleMetaData.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
        }else if(completedOrFailedCount.intValue() == total){
            cycleMetaData.setEndTimestamp(new Timestamp(System.currentTimeMillis()));
            cycleMetaData.setStatus(EMUtil.EMExpStatus.COMPLETED);
        }else {
            LOGGER.debug("Cycle Iterations still in progress {} / {} ", completedOrFailedCount.intValue() , total);
        }
    }

    public static void updateTrialMetaDataStatus(ExperimentTrial experimentTrial,TrialDetails trialDetails) {
        AtomicInteger queuedCount = new AtomicInteger(0);
        AtomicInteger completedOrFailedCount = new AtomicInteger(0);
        trialDetails.getTrialMetaData().getCycles().forEach((cycleName,cycleMetaData) -> {
            if(cycleMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED)){
                queuedCount.set(queuedCount.get() + 1);
            }else if (cycleMetaData.getStatus().equals(EMUtil.EMExpStatus.COMPLETED)){
                completedOrFailedCount.set(completedOrFailedCount.get() + 1);
            }else if (cycleMetaData.getStatus().equals(EMUtil.EMExpStatus.FAILED)){
                completedOrFailedCount.set(completedOrFailedCount.get() + 1);
            }
        });
        trialDetails.getTrialMetaData().getTrialWorkflow().forEach((stepName,stepsMetaData) -> {
            if(stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.QUEUED)) queuedCount.set(queuedCount.get() + 1);
            else if(stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.COMPLETED)) completedOrFailedCount.set(completedOrFailedCount.get() + 1);
            else if(stepsMetaData.getStatus().equals(EMUtil.EMExpStatus.FAILED)) completedOrFailedCount.set(completedOrFailedCount.get() + 1);
        });
        int totalSteps = trialDetails.getTrialMetaData().getCycles().size() + trialDetails.getTrialMetaData().getTrialWorkflow().size() ;
        if(queuedCount.intValue() == totalSteps){
            trialDetails.getTrialMetaData().setBeginTimestamp(new Timestamp(System.currentTimeMillis()));
            trialDetails.getTrialMetaData().setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
        }
        else if(completedOrFailedCount.intValue() == totalSteps){
            trialDetails.getTrialMetaData().setEndTimestamp(new Timestamp(System.currentTimeMillis()));
            trialDetails.getTrialMetaData().setStatus(EMUtil.EMExpStatus.COMPLETED);
        }else {
            LOGGER.debug("Cycle still in progress {} / {} ", completedOrFailedCount.intValue() , trialDetails.getTrialMetaData().getCycles().size());
        }
    }

    public static void updateExperimentTrialMetaDataStatus(ExperimentTrial experimentTrial) {
        AtomicInteger completedOrFailedCount = new AtomicInteger(0);
        experimentTrial.getTrialDetails().forEach((trialNum,trialDetails)->{
            if (trialDetails.getTrialMetaData().getStatus().equals(EMUtil.EMExpStatus.COMPLETED) ||
                    trialDetails.getTrialMetaData().getStatus().equals(EMUtil.EMExpStatus.FAILED) ){
                completedOrFailedCount.set(completedOrFailedCount.get() + 1);
            }
        });
        if (completedOrFailedCount.intValue() == experimentTrial.getTrialDetails().size()){
            experimentTrial.getExperimentMetaData().setEndTimestamp(new Timestamp(System.currentTimeMillis()));
            experimentTrial.setStatus(EMUtil.EMExpStatus.COMPLETED);
        }else{
            LOGGER.debug("Trials still in progress {} / {} ", completedOrFailedCount.intValue() , experimentTrial.getTrialDetails().size());
        }
    }
}
