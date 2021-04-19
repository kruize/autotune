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

package com.autotune.experimentManager.finiteStateMachine.api;
/**
 * Experiment manager Finite state is used to create to uniquely identify the sate.
 * @author Bipin Kumar
 *
 */
public class EMState {

    /*
     * States have unique name within a Easy States FSM instance
     */
    private final String name;

    /**
     * Create a new {@link EMState}.
     *
     * @param name of the state
     */
    public EMState(final String name) {
        this.name = name;
    }

    /**
     * Get state name.
     * @return state name
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EMState state = (EMState) o;

        return name.equals(state.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
