package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.common.experiments.PodContainer;
import org.json.JSONObject;

import static com.autotune.utils.AnalyzerConstants.AutotuneConfigConstants.TUNABLE_VALUE;
import static com.autotune.utils.AnalyzerConstants.QuarkusConstants.DOPTION;
import static com.autotune.utils.AnalyzerConstants.QuarkusConstants.QUARKUS;
import static com.autotune.utils.AutotuneConstants.JSONKeys.EQUALS;

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
		if (tunableName.contains(QUARKUS)) {
			runtimeOptions.append(DOPTION).append(tunableName).append(EQUALS)
					.append(tunableJSON.getLong(TUNABLE_VALUE));
			podContainer.setRuntimeOptions(runtimeOptions.toString());
		}
	}

	@Override
	public void parseTunableResults(Tunable tunable) {

	}
}
