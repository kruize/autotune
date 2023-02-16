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
package com.autotune.common.performanceProfiles.PerformanceProfileInterface;

import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.performanceProfiles.PerformanceProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util class to validate the performance profile metrics with the experiment results metrics.
 */
public class AdvancedClusterMgmtImpl extends PerfProfileImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedClusterMgmtImpl.class);

    @Override
    public String recommend(PerformanceProfile performanceProfile, ExperimentResultData experimentResultData) {

        //TODO: Will be updated once algo is completed

        return null;
    }


}
