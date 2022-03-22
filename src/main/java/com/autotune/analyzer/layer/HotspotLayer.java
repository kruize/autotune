package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.common.experiments.PodContainer;
import org.json.JSONObject;

import static com.autotune.utils.AnalyzerConstants.AutotuneConfigConstants.TRUE;
import static com.autotune.utils.AnalyzerConstants.AutotuneConfigConstants.TUNABLE_VALUE;
import static com.autotune.utils.AnalyzerConstants.HotspotConstants.*;
import static com.autotune.utils.AutotuneConstants.JSONKeys.*;

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
				|| !podContainer.getRuntimeOptions().contains(SERVER)) {
			runtimeOptions = new StringBuilder(SERVER)
					.append(XXOPTION)
					.append(MAX_RAM_PERCENTAGE)
					.append(EQUALS)
					.append("70");
		} else {
			runtimeOptions = new StringBuilder(podContainer.getRuntimeOptions());
		}
		String tunableName = tunable.getName();
		switch(tunableName) {
			case GC:
				runtimeOptions.append(XXOPTION).append(USE)
						.append(tunableJSON.getString(TUNABLE_VALUE));
				break;
			case TIERED_COMPILATION:
			case ALLOW_PARALLEL_DEFINE_CLASS:
			case ALLOW_VECTORIZE_ON_DEMAND:
			case ALWAYS_COMPILE_LOOP_METHODS:
			case ALWAYS_PRE_TOUCH:
			case ALWAYS_TENURE:
			case BACKGROUND_COMPILATION:
			case DO_ESCAPE_ANALYSIS:
			case USE_INLINE_CACHES:
			case USE_LOOP_PREDICATE:
			case USE_STRING_DEDUPLICATION:
			case USE_SUPER_WORD:
			case USE_TYPE_SPECULATION:
				if (tunableJSON.getString(TUNABLE_VALUE).equalsIgnoreCase(TRUE)) {
					runtimeOptions.append(XXOPTION).append(PLUS).append(tunableName);
				} else {
					runtimeOptions.append(XXOPTION).append(MINUS).append(tunableName);
				}
				break;
			case COMPILE_THRESHOLD_SCALING:
				runtimeOptions.append(XXOPTION).append(tunableName).append(EQUALS)
						.append(tunableJSON.getDouble(TUNABLE_VALUE));
				break;
			case MAX_INLINE_LEVEL:
			case FREQ_INLINE_SIZE:
			case MIN_INLINING_THRESHOLD:
			case COMPILE_THRESHOLD:
			case CONC_GC_THREADS:
			case PARALLEL_GC_THREADS:
			case INLINE_SMALL_CODE:
			case LOOP_UNROLL_LIMIT:
			case LOOP_UNROLL_MIN:
			case MIN_SURVIVOR_RATIO:
			case NEW_RATIO:
			case TIERED_STOP_AT_LEVEL:
			default:
				runtimeOptions.append(XXOPTION).append(tunableName).append(EQUALS)
						.append(tunableJSON.getLong(TUNABLE_VALUE));
		}
		podContainer.setRuntimeOptions(runtimeOptions.toString());
	}

	@Override
	public void parseTunableResults(Tunable tunable) {

	}
}
