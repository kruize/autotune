/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.analyzer.kruizeLayer.layers;

import com.autotune.analyzer.application.Tunable;
import com.autotune.common.trials.ContainerConfigData;
import io.fabric8.kubernetes.api.model.EnvVar;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static com.autotune.analyzer.utils.AnalyzerConstants.AutotuneConfigConstants.TRUE;
import static com.autotune.analyzer.utils.AnalyzerConstants.AutotuneConfigConstants.TUNABLE_VALUE;
import static com.autotune.analyzer.utils.AnalyzerConstants.HotspotConstants.*;
import static com.autotune.analyzer.utils.AnalyzerConstants.QuarkusConstants.DOPTION;
import static com.autotune.utils.KruizeConstants.JSONKeys.*;

/**
 * Layer object used to store Container config details like Java ENV variables etc.
 */
public class HotspotLayer extends GenericLayer implements Layer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HotspotLayer.class);
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
     * @param tunable
     * @param tunableJSON
     */
    @Override
    public void prepTunable(Tunable tunable, JSONObject tunableJSON, ContainerConfigData containerConfigData) {
        StringBuilder runtimeOptions = new StringBuilder();
        ArrayList<EnvVar> environmentalAmendList = (ArrayList<EnvVar>) containerConfigData.getEnvList();
        if (environmentalAmendList.size() == 0) {
            runtimeOptions.append(SERVER)
                    .append(XXOPTION)
                    .append(MAX_RAM_PERCENTAGE)
                    .append(EQUALS)
                    .append("70");
            environmentalAmendList.add(new EnvVar(JAVA_OPTIONS, runtimeOptions.toString(), null));
        } else {
            environmentalAmendList.forEach((envVir) -> {
                if (envVir.getName().equalsIgnoreCase(JAVA_OPTIONS)) {
                    runtimeOptions.append(envVir.getValue());
                }
            });
        }
        String tunableName = tunable.getName();
        switch (tunableName) {
            case GC:
                runtimeOptions.append(XXOPTION).append(USE)
                        .append(tunableJSON.getString(TUNABLE_VALUE));
                break;
            case NETTY_BUFFER_CHECK:
                /**
                 * -Dio.netty.buffer.checkBounds=TUNABLE_VALUE
                 * -Dio.netty.buffer.checkAccessible=TUNABLE_VALUE
                 */
                runtimeOptions.append(DOPTION).append(NETTY_BUFFER_CHECKBOUNDS)
                        .append(EQUALS).append(tunableJSON.getString(TUNABLE_VALUE));
                runtimeOptions.append(DOPTION).append(NETTY_BUFFER_CHECKACCESSIBLE)
                        .append(EQUALS).append(tunableJSON.getString(TUNABLE_VALUE));
                break;
            case ALLOW_PARALLEL_DEFINE_CLASS:
            case ALLOW_VECTORIZE_ON_DEMAND:
            case ALWAYS_COMPILE_LOOP_METHODS:
            case ALWAYS_PRE_TOUCH:
            case ALWAYS_TENURE:
            case BACKGROUND_COMPILATION:
            case DO_ESCAPE_ANALYSIS:
            case STACK_TRACE_IN_THROWABLE:
            case TIERED_COMPILATION:
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
            case COMPILE_THRESHOLD:
            case CONC_GC_THREADS:
            case FREQ_INLINE_SIZE:
            case INLINE_SMALL_CODE:
            case LOOP_UNROLL_LIMIT:
            case LOOP_UNROLL_MIN:
            case MAX_INLINE_LEVEL:
            case MIN_INLINING_THRESHOLD:
            case MIN_SURVIVOR_RATIO:
            case NEW_RATIO:
            case PARALLEL_GC_THREADS:
            case TIERED_STOP_AT_LEVEL:
            default:
                runtimeOptions.append(XXOPTION).append(tunableName).append(EQUALS)
                        .append(tunableJSON.getLong(TUNABLE_VALUE));
        }
        environmentalAmendList.forEach((envVir) -> {
            if (envVir.getName().equalsIgnoreCase(JAVA_OPTIONS)) {
                envVir.setValue(runtimeOptions.toString());
            }
        });
        containerConfigData.setEnvList(environmentalAmendList);
    }

    @Override
    public void parseTunableResults(Tunable tunable) {

    }
}
