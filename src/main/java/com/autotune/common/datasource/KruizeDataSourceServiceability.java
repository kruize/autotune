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

package com.autotune.common.datasource;

import com.autotune.common.utils.CommonUtils;

/**
 * KruizeDataSourceServiceability holds the possible functions of a datasources which can state
 * that the datasource is serviceable
 */
public interface KruizeDataSourceServiceability {

    /**
     * Check if a datasource is reachable, implementation of this function
     * should check and return the reachability status (REACHABLE, NOT_REACHABLE)
     * @return DatasourceReachabilityStatus
     */
    public CommonUtils.DatasourceReachabilityStatus isReachable();

    /**
     * Check if a datasource is reliable to use, implementation of this function
     * should check and return the reachability status (RELIABLE, NOT RELIABLE)
     * @return DatasourceReliabilityStatus
     */
    public CommonUtils.DatasourceReliabilityStatus isReliable(Object... objects);
}
