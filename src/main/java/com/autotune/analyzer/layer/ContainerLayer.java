package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.common.data.experiments.PodContainer;
import com.autotune.common.data.experiments.Resources;
import org.json.JSONObject;

public class ContainerLayer extends GenericLayer implements Layer {

	@Override
	public void prepTunable(Tunable tunable, JSONObject tunableJSON, PodContainer podContainer) {
		Resources requests = podContainer.getRequests();
		String tunableName = tunable.getName();
		if ("cpuRequest".equals(tunableName)) {
			String cpu = tunableJSON.getDouble("tunable_value") +
					tunable.getBoundUnits();
			System.out.println("CPU Request: " + cpu);
			if (requests == null) {
				requests = new Resources(cpu, null);
			} else {
				requests.setCpu(cpu);
			}
		} else if ("memoryRequest".equals(tunableName)) {
			String memory = tunableJSON.getDouble("tunable_value") +
					tunable.getBoundUnits();
			System.out.println("Mem Request: " + memory);
			if (requests == null) {
				requests = new Resources(null, memory);
			} else {
				requests.setMemory(memory);
			}
		}

		podContainer.setRequests(requests);
		podContainer.setLimits(requests);
	}

	@Override
	public void parseTunableResults(Tunable tunable) {

	}
}
