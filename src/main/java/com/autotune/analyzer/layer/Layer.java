package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.experiments.TrialDetails;
import org.json.JSONObject;

public interface Layer {
	void prepTunable(Tunable tunable, JSONObject tunableJSON, TrialDetails deployment);
	void parseTunableResults(Tunable tunable);
}
