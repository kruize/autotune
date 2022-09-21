package com.autotune.experimentManager.handler.eminterface;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.experimentManager.data.result.CycleMetaData;
import com.autotune.experimentManager.data.result.StepsMetaData;

import javax.servlet.ServletContext;

public interface EMHandlerInterface {
    public void execute(ExperimentTrial experimentTrial, TrialDetails trialDetails,
                        CycleMetaData cycleMetaData,
                        StepsMetaData stepsMeatData,
                        AutotuneExecutor autotuneExecutor, ServletContext context);
}
