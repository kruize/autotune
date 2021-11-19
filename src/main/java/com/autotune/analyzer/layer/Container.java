package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.experiments.Deployments;
import com.autotune.analyzer.experiments.Resources;
import org.json.JSONObject;

public class Container extends Generic implements Layer {

	@Override
	public void prepTunable(Tunable tunable, JSONObject tunableJSON, Deployments deployment) {
		Resources requests = deployment.getRequests();
		String tunableName = tunableJSON.getString("tunable_name");
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

		deployment.setRequests(requests);
		deployment.setLimits(requests);
	}

	@Override
	public void parseTunableResults(Tunable tunable) {

	}
}
