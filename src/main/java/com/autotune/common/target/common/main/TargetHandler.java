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

package com.autotune.common.target.common.main;

import com.autotune.common.target.common.exception.TargetHandlerException;
import org.json.JSONObject;

import java.util.List;

/**
 * TargetHandler interface helps in handling both kubernetes and
 * non kubernetes environment using some common function.
 * At present, Only kubernetes environment implemented.
 *
 * @param <T>
 */

public interface TargetHandler<T> {
    void deployApplication(JSONObject deploymentDetails) throws TargetHandlerException;

    List<T> collectMetrics(List<T> results) throws TargetHandlerException;

    Object getService();
}
