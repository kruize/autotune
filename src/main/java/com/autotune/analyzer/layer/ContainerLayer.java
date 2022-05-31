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
import com.autotune.utils.AutotuneConstants;
import io.fabric8.kubernetes.api.model.Quantity;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.autotune.utils.AnalyzerConstants.AutotuneConfigConstants.TUNABLE_VALUE;
import static com.autotune.utils.AnalyzerConstants.ContainerConstants.CPU_REQUEST;
import static com.autotune.utils.AnalyzerConstants.ContainerConstants.MEM_REQUEST;

/**
 * Layer object used to store Container config details like CPU,Memory.
 */
public class ContainerLayer extends GenericLayer implements Layer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerLayer.class);

    @Override
    public void prepTunable(Tunable tunable, JSONObject tunableJSON, ContainerConfigData containerConfigData) {
        String tunableName = tunable.getName();
        Map<String, Quantity> requestPropertiesMap = new HashMap<String, Quantity>();
        switch (tunableName) {
            case CPU_REQUEST:
                String cpu = tunableJSON.getDouble(TUNABLE_VALUE) +
                        tunable.getBoundUnits();
                System.out.println("CPU Request: " + cpu);
                requestPropertiesMap.put(AutotuneConstants.JSONKeys.CPU, new Quantity(cpu));
            case MEM_REQUEST:
                String memory = tunableJSON.getDouble(TUNABLE_VALUE) +
                        tunable.getBoundUnits();
                System.out.println("Mem Request: " + memory);
                requestPropertiesMap.put(AutotuneConstants.JSONKeys.MEMORY, new Quantity(memory));
        }
        containerConfigData.setRequestPropertiesMap(requestPropertiesMap);
        containerConfigData.setLimitPropertiesMap(requestPropertiesMap);
    }


    @Override
    public void parseTunableResults(Tunable tunable) {

    }
}
