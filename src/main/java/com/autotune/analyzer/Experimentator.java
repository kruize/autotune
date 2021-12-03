package com.autotune.analyzer;

import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.experiments.ExperimentTrial;
import com.autotune.analyzer.k8sObjects.AutotuneConfig;
import com.autotune.analyzer.k8sObjects.AutotuneObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Experimentator implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(Experimentator.class);
	private static final int MAX_NUMBER_OF_EXPERIMENTS = 1;
	private static int num_experiments = 0;
	public static HashMap<String, ApplicationSearchSpace> applicationSearchSpaceMap = new HashMap<>();
	public static HashMap<String, AutotuneExperiment> experimentsMap = new HashMap<>();
	public static Map<String, Map <String, Double>> tunablesMap = new HashMap<>();

	public static void start() {
		Experimentator experimentator = new Experimentator();
		Thread expThread = new Thread(experimentator);
		expThread.start();
	}

	@Override
	public void run() {

	}

	public static void startExperiment(AutotuneObject autotuneObject,
									   ApplicationServiceStack applicationServiceStack) {

		try {
			ApplicationSearchSpace applicationSearchSpace = updateSearchSpace(autotuneObject, applicationServiceStack);
			applicationServiceStack.setApplicationSearchSpace(applicationSearchSpace);

			AutotuneExperiment autotuneExperiment = experimentsMap.get(autotuneObject.getExperimentName());
			// If a experiment is already underway, need to update it
			if (null != autotuneExperiment) {
				updateExperiment(autotuneExperiment);
				return;
			}

			// Start a new experiment
			ArrayList<ExperimentTrial> experimentTrials = new ArrayList<>();
			autotuneExperiment = new AutotuneExperiment(autotuneObject.getExperimentName(),
					autotuneObject.getExperimentId(),
					autotuneObject,
					"Pending Provisioning",
					applicationServiceStack,
					experimentTrials);
			experimentsMap.put(autotuneObject.getExperimentName(), autotuneExperiment);

			// Autotune can only handle MAX_NUMBER_OF_EXPERIMENTS at any given time
			if (++num_experiments <= MAX_NUMBER_OF_EXPERIMENTS) {
				RunExperiment runExperiment = new RunExperiment(autotuneExperiment);
				Thread runExp = new Thread(runExperiment);
				autotuneExperiment.setExperimentThread(runExp);
				runExp.start();
			} else {
				// TODO: Need to push the experiments to a queue
				// Do nothing for now
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void updateExperiment(AutotuneExperiment autotuneExperiment) {
	}

	private static ApplicationSearchSpace updateSearchSpace(AutotuneObject autotuneObject,
															ApplicationServiceStack applicationServiceStack) {

		try {
			ApplicationSearchSpace applicationSearchSpace = applicationSearchSpaceMap.get(autotuneObject.getExperimentName());
			if (null != applicationSearchSpace) {
				return applicationSearchSpace;
			}

			String experimentName = autotuneObject.getExperimentName();
			String experimentId = autotuneObject.getExperimentId();
			String objectiveFunction = autotuneObject.getSloInfo().getObjectiveFunction();
			String hpoAlgoImpl = autotuneObject.getSloInfo().getHpoAlgoImpl();
			String direction = autotuneObject.getSloInfo().getDirection();
			// TODO: Need to add valueType to the ObjectiveFunction!
			String valueType = "double";

			applicationSearchSpace = new ApplicationSearchSpace(experimentName,
					experimentId,
					objectiveFunction,
					hpoAlgoImpl,
					direction,
					valueType);

			for (String layerName : applicationServiceStack.getApplicationServiceStackLayers().keySet()) {
				AutotuneConfig layer = applicationServiceStack.getApplicationServiceStackLayers().get(layerName);
				for (Tunable tunable : layer.getTunables()) {
					applicationSearchSpace.getTunablesMap().put(tunable.getName(), tunable);
				}
			}

			applicationSearchSpaceMap.put(experimentName, applicationSearchSpace);

			return applicationSearchSpace;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
