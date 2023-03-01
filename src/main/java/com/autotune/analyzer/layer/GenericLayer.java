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
import io.fabric8.kubernetes.api.model.EnvVar;
import org.json.JSONObject;

import java.util.ArrayList;
/**
 * Layer object used to store Generic Container config details.
 */
public class GenericLayer implements Layer {

    public void prepTunable(Tunable tunable, JSONObject tunableJSON, ContainerConfigData containerConfigData) {
        String tunableName = tunable.getName();
        ArrayList<EnvVar> environmentalAmendList = new ArrayList<>();
        environmentalAmendList.add(new EnvVar(tunableName, String.valueOf(tunableJSON.getLong("tunable_value")), null));
        containerConfigData.setEnvList(environmentalAmendList);
    }

    public void parseTunableResults(Tunable tunable) {

    }
}
