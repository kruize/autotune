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
package com.autotune.common.validators;

/**
 * Validator class provides the validators for each set of sections which needs validation
 */
public class Validator {
    /**
     * Private constructor as we shouldn't create an instance for Validator
     */
    private Validator() {

    }

    /**
     * Returns the instance of Metrics Validator
     * @return
     */
    public static MetricsValidator getMetricsValidator() {
        return MetricsValidator.getInstance();
    }
}
