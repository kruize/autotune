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

package com.autotune.experimentManager.handler.eminterface;

import com.autotune.experimentManager.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class to identify handler and trigger execute.
 */
public final class EMHandlerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMHandlerFactory.class);

    public <T extends EMHandlerInterface> EMHandlerInterface create(String className) {
        EMHandlerInterface toReturn = null;
        if (PreValidationHandler.class.getName().equals(className)) {
            toReturn = new PreValidationHandler();
        } else if (DeploymentHandler.class.getName().equals(className)) {
            toReturn = new DeploymentHandler();
        } else if (PostValidationHandler.class.getName().equals(className)) {
            toReturn = new PostValidationHandler();
        } else if (LoadValidationHandler.class.getName().equals(className)) {
            toReturn = new LoadValidationHandler();
        } else if (MetricCollectionHandler.class.getName().equals(className)) {
            toReturn = new MetricCollectionHandler();
        } else if (SummarizerHandler.class.getName().equals(className)) {
            toReturn = new SummarizerHandler();
        } else if (PostResultsHandler.class.getName().equals(className)) {
            toReturn = new PostResultsHandler();
        } else {
            LOGGER.error("Factory class not updated for Hander : {} ", className);
        }
        return toReturn;
    }
}
