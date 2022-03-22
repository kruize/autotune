package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.common.experiments.PodContainer;
import com.autotune.common.experiments.Resources;
import org.json.JSONObject;

import static com.autotune.utils.AnalyzerConstants.AutotuneConfigConstants.TUNABLE_VALUE;
import static com.autotune.utils.AnalyzerConstants.ContainerConstants.CPU_REQUEST;
import static com.autotune.utils.AnalyzerConstants.ContainerConstants.MEM_REQUEST;

public class ContainerLayer extends GenericLayer implements Layer {

	@Override
	public void prepTunable(Tunable tunable, JSONObject tunableJSON, PodContainer podContainer) {
		Resources requests = podContainer.getRequests();
		String tunableName = tunable.getName();
		switch(tunableName) {
			case CPU_REQUEST:
				String cpu = tunableJSON.getDouble(TUNABLE_VALUE) +
						tunable.getBoundUnits();
				System.out.println("CPU Request: " + cpu);
				if (requests == null) {
					requests = new Resources(cpu, null);
				} else {
					requests.setCpu(cpu);
				}
				break;
			case MEM_REQUEST:
				String memory = tunableJSON.getDouble(TUNABLE_VALUE) +
						tunable.getBoundUnits();
				System.out.println("Mem Request: " + memory);
				if (requests == null) {
					requests = new Resources(null, memory);
				} else {
					requests.setMemory(memory);
				}
				break;
			default:
		}

		podContainer.setRequests(requests);
		podContainer.setLimits(requests);
	}

	@Override
	public void parseTunableResults(Tunable tunable) {

	}
}
