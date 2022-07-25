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

package com.autotune.common.target.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General exception are handled here which are related to Target handler operation.
 */
public class TargetHandlerException extends Throwable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TargetHandlerException.class);

    public TargetHandlerException(Exception e, final String errorMsg) {
        super(errorMsg);
        LOGGER.error(errorMsg);
        LOGGER.debug("Kubernetes error info", e);
    }
}
