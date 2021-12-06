package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.experiments.Deployments;
import org.json.JSONObject;

public class Quarkus extends Generic implements Layer {
	@Override
	public void prepTunable(Tunable tunable, JSONObject tunableJSON, Deployments deployment) {
		StringBuilder runtimeOptions = new StringBuilder(deployment.getRuntimeOptions());
		String tunableName = tunableJSON.getString("tunable_name");
		if (tunableName.contains("quarkus")) {
			runtimeOptions.append(" -D").append(tunableName).append("=")
					.append(tunableJSON.getLong("tunable_value"));
			deployment.setRuntimeOptions(runtimeOptions.toString());
		}
	}

	@Override
	public void parseTunableResults(Tunable tunable) {

	}
}
