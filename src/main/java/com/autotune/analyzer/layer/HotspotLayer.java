package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.common.data.experiments.PodContainer;
import org.json.JSONObject;

/**
 *
 */
public class HotspotLayer extends GenericLayer implements Layer {

	/**
	 * Currently the following hotspot tunables are supported
	 * -server -XX:+UseG1GC -XX:MaxRAMPercentage=70
	 * -XX:FreqInlineSize=364 -XX:MaxInlineLevel=35 -XX:MinInliningThreshold=95 -XX:CompileThreshold=3180 -XX:CompileThresholdScaling=6.3
	 * -XX:ConcGCThreads=8 -XX:ParallelGCThreads=8 -XX:InlineSmallCode=1840 -XX:LoopUnrollLimit=138 -XX:LoopUnrollMin=7 -XX:MinSurvivorRatio=18
	 * -XX:NewRatio=2 -XX:TieredStopAtLevel=2 -XX:-TieredCompilation -XX:-AllowParallelDefineClass -XX:-AllowVectorizeOnDemand
	 * -XX:-AlwaysCompileLoopMethods -XX:-AlwaysPreTouch -XX:-AlwaysTenure -XX:+BackgroundCompilation -XX:-DoEscapeAnalysis -XX:+UseInlineCaches
	 * -XX:+UseLoopPredicate -XX:-UseStringDeduplication -XX:-UseSuperWord -XX:-UseTypeSpeculation
	 */

	/**
	 *
	 * @param tunable
	 * @param tunableJSON
	 * @param podContainer
	 */
	@Override
	public void prepTunable(Tunable tunable, JSONObject tunableJSON, PodContainer podContainer) {
		StringBuilder runtimeOptions;
		if (podContainer.getRuntimeOptions() == null
				|| !podContainer.getRuntimeOptions().contains("server")) {
			runtimeOptions = new StringBuilder(" -server -XX:MaxRAMPercentage=70");
		} else {
			runtimeOptions = new StringBuilder(podContainer.getRuntimeOptions());
		}
		String tunableName = tunable.getName();
		switch(tunableName) {
			case "gc":
				runtimeOptions.append(" -XX:").append("+Use")
						.append(tunableJSON.getString("tunable_value"));
				break;
			case "TieredCompilation":
			case "AllowParallelDefineClass":
			case "AllowVectorizeOnDemand":
			case "AlwaysCompileLoopMethods":
			case "AlwaysPreTouch":
			case "AlwaysTenure":
			case "BackgroundCompilation":
			case "DoEscapeAnalysis":
			case "UseInlineCaches":
			case "UseLoopPredicate":
			case "UseStringDeduplication":
			case "UseSuperWord":
			case "UseTypeSpeculation":
				if (tunableJSON.getString("tunable_value").equalsIgnoreCase("true")) {
					runtimeOptions.append(" -XX:").append("+").append(tunableName);
				} else {
					runtimeOptions.append(" -XX:").append("-").append(tunableName);
				}
				break;
			case "CompileThresholdScaling":
				runtimeOptions.append(" -XX:").append(tunableName).append("=")
						.append(tunableJSON.getDouble("tunable_value"));
				break;
			case "MaxInlineLevel":
			case "FreqInlineSize":
			case "MinInliningThreshold":
			case "CompileThreshold":
			case "ConcGCThreads":
			case "ParallelGCThreads":
			case "InlineSmallCode":
			case "LoopUnrollLimit":
			case "LoopUnrollMin":
			case "MinSurvivorRatio":
			case "NewRatio":
			case "TieredStopAtLevel":
			default:
				runtimeOptions.append(" -XX:").append(tunableName).append("=")
						.append(tunableJSON.getLong("tunable_value"));
		}
		podContainer.setRuntimeOptions(runtimeOptions.toString());
	}

	@Override
	public void parseTunableResults(Tunable tunable) {

	}
}
