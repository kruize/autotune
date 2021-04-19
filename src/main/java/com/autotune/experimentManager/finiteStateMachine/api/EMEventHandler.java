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

import com.autotune.experimentManager.finiteStateMachine.api.EMEvent;

/**
 * Top level Handler for Autotune finite state machine
 * Each sub class implementation should implement the handleEvent() method
 * provide the business logic on a particular event.
 * @author Bipin Kumar
 *
 */

public interface EMEventHandler<E extends EMEvent> {
    /**
     * Event Handler method to execute when an event occurs in the experiment manager.
     * @param event the triggered event
     * @throws Exception thrown if a problem occurs during action performing
     */

    void handleEvent(E event) throws Exception;
}
