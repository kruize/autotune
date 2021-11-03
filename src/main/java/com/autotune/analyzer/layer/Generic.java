package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.experiments.Deployments;
import org.json.JSONObject;

public class Generic implements Layer {

	public void prepTunable(Tunable tunable, JSONObject tunableJSON, Deployments deployment) {
		System.out.println("######: Generic Tunable Parsing");
		StringBuilder runtimeOptions = new StringBuilder(deployment.getRuntimeOptions());
		String tunableName = tunableJSON.getString("tunable_name");
		runtimeOptions.append(" -XX:").append(tunableName).append("=")
				.append(tunableJSON.getLong("tunable_value"));
		deployment.setRuntimeOptions(runtimeOptions.toString());
	}
	public void parseTunableResults(Tunable tunable) {

	}
}