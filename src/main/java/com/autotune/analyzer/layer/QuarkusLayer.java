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
package com.autotune.analyzer.layer;

import com.autotune.analyzer.application.Tunable;
import com.autotune.common.experiments.ContainerConfigData;
import com.autotune.common.experiments.PodContainer;
import io.fabric8.kubernetes.api.model.EnvVar;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static com.autotune.utils.AnalyzerConstants.AutotuneConfigConstants.TUNABLE_VALUE;
import static com.autotune.utils.AnalyzerConstants.HotspotConstants.*;
import static com.autotune.utils.AnalyzerConstants.QuarkusConstants.DOPTION;
import static com.autotune.utils.AnalyzerConstants.QuarkusConstants.QUARKUS;
import static com.autotune.utils.AutotuneConstants.JSONKeys.EQUALS;
import static com.autotune.utils.AutotuneConstants.JSONKeys.JAVA_OPTIONS;
/**
 * Layer object used to store Container config details like Java ENV variables etc.
 */
public class QuarkusLayer extends GenericLayer implements Layer {
	private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusLayer.class);
	@Override
	public void prepTunable(Tunable tunable, JSONObject tunableJSON, ContainerConfigData containerConfigData) {
		LOGGER.debug(tunableJSON.toString());
		StringBuilder runtimeOptions = new StringBuilder();
		ArrayList<EnvVar> environmentalAmendList = (ArrayList<EnvVar>) containerConfigData.getEnvList();
		if( environmentalAmendList.size() == 0) {
			runtimeOptions.append(tunable.getName()).append(EQUALS).append(tunableJSON.get("tunable_value") ).append(" ");
			environmentalAmendList.add(new EnvVar(JAVA_OPTIONS, runtimeOptions.toString(), null));
		}else {
			environmentalAmendList.forEach((envVir) -> {
				if (envVir.getName().equalsIgnoreCase(JAVA_OPTIONS)) {
					runtimeOptions.append(envVir.getValue()).append(tunable.getName()).append(EQUALS).append(tunableJSON.get("tunable_value") ).append(" ");
					envVir.setValue(runtimeOptions.toString());
				}
			});
		}
		LOGGER.debug(runtimeOptions.toString());
		containerConfigData.setEnvList(environmentalAmendList);
	}

	@Override
	public void parseTunableResults(Tunable tunable) {

	}
}
