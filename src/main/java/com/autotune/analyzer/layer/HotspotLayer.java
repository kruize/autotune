package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.common.data.experiments.PodContainer;
import org.json.JSONObject;

public class HotspotLayer extends GenericLayer implements Layer {

	@Override
	public void prepTunable(Tunable tunable, JSONObject tunableJSON, PodContainer podContainer) {
		StringBuilder runtimeOptions;
		if (podContainer.getRuntimeOptions() == null) {
			runtimeOptions = new StringBuilder();
		} else {
			runtimeOptions = new StringBuilder(podContainer.getRuntimeOptions());
		}
		runtimeOptions.append(" -server -XX:+UseG1GC -XX:MaxRAMPercentage=70");
		String tunableName = tunable.getName();
		if ("MaxInlineLevel".equals(tunableName)) {
			runtimeOptions.append(" -XX:").append(tunableName).append("=")
					.append(tunableJSON.getLong("tunable_value"));
		}
		podContainer.setRuntimeOptions(runtimeOptions.toString());
	}

	@Override
	public void parseTunableResults(Tunable tunable) {

	}
}
