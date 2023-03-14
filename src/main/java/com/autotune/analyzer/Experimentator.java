package com.autotune.analyzer;

import com.autotune.analyzer.application.ApplicationDeployment;
import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.application.Tunable;
import com.autotune.common.experiments.ExperimentSummary;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.k8sObjects.AutotuneConfig;
import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.common.performanceProfiles.ObjectiveFunction;
import com.autotune.common.performanceProfiles.PerformanceProfile;
import com.autotune.common.performanceProfiles.PerformanceProfilesDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.TreeMap;

import static com.autotune.utils.ExperimentMessages.RunExperiment.INITIAL_STATUS;

/**
 *
 */
public class Experimentator implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Experimentator.class);
    private static final int MAX_NUMBER_OF_EXPERIMENTS = 2;
    private static final int MAX_NUMBER_OF_TRIALS = 10;
    private static final int NUMBER_OF_PARALLEL_TRIALS = 1;
    private static int num_experiments = 0;
    public static HashMap<String, AutotuneExperiment> experimentsMap = new HashMap<>();

    public static void start() {
        Experimentator experimentator = new Experimentator();
        Thread expThread = new Thread(experimentator);
        expThread.start();
    }

    @Override
    public void run() {

    }

    /**
     * @param kruizeObject
     * @param applicationDeployment
     */
    public static void startExperiment(KruizeObject kruizeObject,
                                       ApplicationDeployment applicationDeployment) {

        try {
            AutotuneExperiment autotuneExperiment = experimentsMap.get(applicationDeployment.getDeploymentName());
            // If a experiment is already underway, need to update it
            if (null != autotuneExperiment) {
                updateExperiment(autotuneExperiment);
                return;
            }

            // Start a new experiment
            TreeMap<Integer, ExperimentTrial> experimentTrials = new TreeMap<>();
            ExperimentSummary experimentSummary = new ExperimentSummary(MAX_NUMBER_OF_TRIALS,
                    0,
                    0,
                    0,
                    0,
                    0,
                    -1
            );
            autotuneExperiment = new AutotuneExperiment(applicationDeployment.getDeploymentName(),
                    kruizeObject.getExperimentName(),
                    kruizeObject,
                    INITIAL_STATUS,
                    experimentSummary,
                    applicationDeployment,
                    experimentTrials);
            experimentsMap.put(applicationDeployment.getDeploymentName(), autotuneExperiment);

            ApplicationSearchSpace applicationSearchSpace = updateSearchSpace(autotuneExperiment);
            autotuneExperiment.setApplicationSearchSpace(applicationSearchSpace);

            // Autotune can only handle MAX_NUMBER_OF_EXPERIMENTS at any given time
            if (++num_experiments <= MAX_NUMBER_OF_EXPERIMENTS) {
                RunExperiment runExperiment = new RunExperiment(autotuneExperiment);
                autotuneExperiment.setExperimentThread(runExperiment);
                Thread runExp = new Thread(runExperiment);
                runExp.start();
                runExp.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateExperiment(AutotuneExperiment autotuneExperiment) {
    }

    private static ApplicationSearchSpace updateSearchSpace(AutotuneExperiment autotuneExperiment) {

        try {
            ApplicationSearchSpace applicationSearchSpace = autotuneExperiment.getApplicationSearchSpace();
            if (null != applicationSearchSpace) {
                return applicationSearchSpace;
            }

            KruizeObject kruizeObject = autotuneExperiment.getAutotuneObject();
            String experimentName = autotuneExperiment.getExperimentName();
            String experimentId = kruizeObject.getExperimentId();
            PerformanceProfile performanceProfile = PerformanceProfilesDeployment.performanceProfilesMap
                    .get(kruizeObject.getPerformanceProfile());
            ObjectiveFunction objectiveFunction = performanceProfile.getSloInfo().getObjectiveFunction();
            String hpoAlgoImpl = kruizeObject.getHpoAlgoImpl();
            String direction = performanceProfile.getSloInfo().getDirection();
            // TODO: Need to add valueType to the ObjectiveFunction!
            String valueType = "double";

            applicationSearchSpace = new ApplicationSearchSpace(experimentName,
                    experimentId,
                    objectiveFunction,
                    hpoAlgoImpl,
                    direction,
                    valueType,
                    MAX_NUMBER_OF_TRIALS,
                    NUMBER_OF_PARALLEL_TRIALS);

            for (String stackName : autotuneExperiment.getApplicationDeployment().getApplicationServiceStackMap().keySet()) {
                ApplicationServiceStack applicationServiceStack = autotuneExperiment.getApplicationDeployment().getApplicationServiceStackMap().get(stackName);
                for (String layerName : applicationServiceStack.getApplicationServiceStackLayers().keySet()) {
                    AutotuneConfig layer = applicationServiceStack.getApplicationServiceStackLayers().get(layerName);
                    for (Tunable tunable : layer.getTunables()) {
                        StringBuilder tunableFullName = new StringBuilder(stackName)
                                .append("|")
                                .append(layerName)
                                .append("|")
                                .append(tunable.getName());
                        tunable.setStackName(stackName);
                        tunable.setFullName(tunableFullName.toString());
                        applicationSearchSpace.getTunablesMap().put(tunableFullName.toString(), tunable);
                    }
                }
            }

            return applicationSearchSpace;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
