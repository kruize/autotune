package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.experiments.Deployments;
import org.json.JSONObject;

public class Hotspot extends Generic implements Layer {

	@Override
	public void prepTunable(Tunable tunable, JSONObject tunableJSON, Deployments deployment) {
		System.out.println("######: Hotspot Tunable Parsing");
		StringBuilder runtimeOptions = new StringBuilder(deployment.getRuntimeOptions());
		runtimeOptions.append("-server -XX:+UseG1GC -XX:MaxRAMPercentage=70");
		String tunableName = tunableJSON.getString("tunable_name");
		if ("MaxInlineLevel".equals(tunableName)) {
			runtimeOptions.append(" -XX:").append(tunableName).append("=")
					.append(tunableJSON.getLong("tunable_value"));
		}
		deployment.setRuntimeOptions(runtimeOptions.toString());
	}

	@Override
	public void parseTunableResults(Tunable tunable) {

	}
}