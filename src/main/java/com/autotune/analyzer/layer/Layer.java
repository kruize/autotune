package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.experiments.PodContainer;
import org.json.JSONObject;

public interface Layer {
	void prepTunable(Tunable tunable, JSONObject tunableJSON, PodContainer podContainer);
	void parseTunableResults(Tunable tunable);
}
