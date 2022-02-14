package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.experiments.PodContainer;
import org.json.JSONObject;

public class QuarkusLayer extends GenericLayer implements Layer {
	@Override
	public void prepTunable(Tunable tunable, JSONObject tunableJSON, PodContainer podContainer) {
		StringBuilder runtimeOptions;
		if (podContainer.getRuntimeOptions() == null) {
			runtimeOptions = new StringBuilder();
		} else {
			runtimeOptions = new StringBuilder(podContainer.getRuntimeOptions());
		}
		String tunableName = tunable.getName();
		if (tunableName.contains("quarkus")) {
			runtimeOptions.append(" -D").append(tunableName).append("=")
					.append(tunableJSON.getLong("tunable_value"));
			podContainer.setRuntimeOptions(runtimeOptions.toString());
		}
	}

	@Override
	public void parseTunableResults(Tunable tunable) {

	}
}
