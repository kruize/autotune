/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.data;

import com.autotune.common.k8sObjects.AutotuneObject;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.Utils;

import java.util.List;
import java.util.Map;

public class ExperimentInterfaceImpl implements ExperimentInterface {

    @Override
    public boolean addExperiments(Map<String, AutotuneObject> mainKruizeExperimentMap, List<AutotuneObject> kruizeExperimentList) {
        kruizeExperimentList.forEach(
                (ao) -> {
                    ao.setStatus(AnalyzerConstants.ExpStatus.QUEUED);
                    ao.setExperimentId(Utils.generateID(toString()));
                    mainKruizeExperimentMap.put(
                            ao.getExperimentName(),
                            ao
                    );
                }
        );
        return true;
    }

}
