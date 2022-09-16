package com.autotune.experimentManager.workerimpl;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;
import com.autotune.experimentManager.utils.EMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;

public class PreValidationHandler implements AutotuneWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreValidationHandler.class);
    @Override
    public void execute(Object o, AutotuneExecutor autotuneExecutor, ServletContext context) {
        ExperimentTrial experimentTrial = (ExperimentTrial) o;
        try {
            experimentTrial.setStatus(EMUtil.EMExpStatus.IN_PROGRESS);
            autotuneExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            AutotuneWorker theWorker = new CallableFactory().create(autotuneExecutor.getWorker());
                            theWorker.execute(experimentTrial, autotuneExecutor, context);
                        }
                    }
            );
        }catch (Exception e){
            e.printStackTrace();
            LOGGER.error("Failed to execute PreValidate step for Experiment name :{} due to: {}"
                    ,experimentTrial.getExperimentName(),e.getMessage());
        }
    }
}
