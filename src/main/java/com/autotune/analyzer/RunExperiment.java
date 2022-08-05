package com.autotune.analyzer;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.utils.ServerContext;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

import static com.autotune.analyzer.loop.EMInterface.SendTrialToEM;
import static com.autotune.analyzer.loop.HPOInterface.getNewTrialFromHPO;
import static com.autotune.analyzer.loop.EMInterface.SendTrialToEM;
import static com.autotune.analyzer.loop.HPOInterface.postTrialResultToHPO;
import static com.autotune.utils.AutotuneConstants.HpoOperations.*;
import static com.autotune.utils.AutotuneConstants.JSONKeys.URL;
import static com.autotune.utils.AutotuneConstants.JSONKeys.*;
import static com.autotune.utils.AutotuneConstants.JSONKeys.DEPLOYMENT_NAME;
import static com.autotune.utils.AutotuneConstants.JSONKeys.URL;
import static com.autotune.utils.ServerContext.OPTUNA_TRIALS_END_POINT;

/**
 *
 */
public class RunExperiment {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunExperiment.class);
    private final AutotuneExperiment autotuneExperiment;

    public RunExperiment(AutotuneExperiment autotuneExperiment) {
        this.autotuneExperiment = autotuneExperiment;
    }

    /**
     *
     */
    public synchronized void receive() {
        while (true) {
            try {
                wait();
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.info("Thread Interrupted");
            }
        }
    }

    /**
     *
     */
    public synchronized void send() {
        notify();
    }

    /**
     *
     */
    //@Override
    public void run() {
        try {
            String experimentId = autotuneExperiment.getAutotuneObject().getExperimentId();
            StringBuilder searchSpaceUrl = new StringBuilder(ServerContext.SEARCH_SPACE_END_POINT)
                    .append(QUESTION_MARK).append(DEPLOYMENT_NAME)
                    .append(EQUALS).append(autotuneExperiment.getDeploymentName());
            JSONObject hpoTrial = new JSONObject();
            hpoTrial.put(ID, experimentId);
            hpoTrial.put(URL, searchSpaceUrl.toString());
            URL experimentTrialsURL = null;
            try {
                experimentTrialsURL = new URL(OPTUNA_TRIALS_END_POINT);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (null == autotuneExperiment.getHPOoperation()) {
                hpoTrial.put(OPERATION, EXP_TRIAL_GENERATE_NEW);
            } else {
                hpoTrial.put(OPERATION, EXP_TRIAL_GENERATE_SUBSEQUENT);
            }

            ExperimentTrial experimentTrial = getNewTrialFromHPO(autotuneExperiment, experimentTrialsURL, hpoTrial);
            if (null != experimentTrial)
                SendTrialToEM(autotuneExperiment, experimentTrial);

        } catch (Exception e) {
            LOGGER.debug(e.toString());
            e.printStackTrace();
        }
    }
}
