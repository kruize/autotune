package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.experiments.PodContainer;
import org.json.JSONObject;

public class GenericLayer implements Layer {

	public void prepTunable(Tunable tunable, JSONObject tunableJSON, PodContainer podContainer) {
		StringBuilder runtimeOptions;
		if (podContainer.getRuntimeOptions() == null) {
			runtimeOptions = new StringBuilder();
		} else {
			runtimeOptions = new StringBuilder(podContainer.getRuntimeOptions());
		}
		String tunableName = tunable.getName();
		runtimeOptions.append(" ").append(tunableName).append("=")
				.append(tunableJSON.getLong("tunable_value"));
		podContainer.setRuntimeOptions(runtimeOptions.toString());
	}
	public void parseTunableResults(Tunable tunable) {

	}
}
