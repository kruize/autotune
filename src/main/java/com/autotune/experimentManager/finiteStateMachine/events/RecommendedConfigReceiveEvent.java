/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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

package com.autotune.experimentManager.finiteStateMachine.events;

import com.autotune.experimentManager.finiteStateMachine.api.EMAbstractEvent;
import com.autotune.experimentManager.finiteStateMachine.objects.ExperimentTrialObject;

/**
 * RecommendedConfigReceiveEvent trigger when recommendation manager send the recommended tunnables configuration
 * to experiment manager.
 */

public class RecommendedConfigReceiveEvent extends EMAbstractEvent {
    private ExperimentTrialObject data;

    public RecommendedConfigReceiveEvent(ExperimentTrialObject data) {
        super("ConfigurationReceiveEvent");
        this.data = data;

    }

    public RecommendedConfigReceiveEvent(String name) {
        super(name);
    }

    @Override
    public ExperimentTrialObject getData() {
        return this.data;
    }
}
