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

import com.autotune.common.exceptions.KruizeDatasourceAlreadyExists;
import com.autotune.common.exceptions.KruizeDatasourceDoesNotExist;
import com.autotune.common.utils.CommonUtils;

import java.util.Map;

/**
 * KruizeDataSourceCollection is an abstraction which needs to be implemented
 * by every entity which needs a datasource collection used in Autotune
 *
 * Currently Supported Implementations:
 *  - ExperimentDatasourceCollection
 */

public interface KruizeDataSourceCollection {
    /**
     * Adds datasource to the collection map using its name as key
     * @param kruizeDatasource KruizeDataSource Object which has the datasource details
     * @return Appropriate value of AddDataSourceStatus
     *          SUCCESS on successfully adding the datasource
     *          FAILURE on failing to add datasource
     *          DATASOURCE_NOT_REACHABLE if the added datasource is not reachable
     * @throws KruizeDatasourceAlreadyExists
     */
    public CommonUtils.AddDataSourceStatus addDataSource(KruizeDataSource kruizeDatasource) throws KruizeDatasourceAlreadyExists;

    /**
     * Return a particular datasource with the name passed
     * @param datasourceName name of the datasource to be returned
     * @return the particular KruizeDataSource object
     */
    public KruizeDataSource getDatasource(String datasourceName);

    /**
     * Returns the datasource collection map
     * @return Map of AutotuneDatasources
     */
    public Map<String, KruizeDataSource> getDatasourceMap();

    /**
     * Remove a particular datasource matching the passed name
     * @param datasourceName the name of the datasource which needs to be removed
     * @throws KruizeDatasourceDoesNotExist
     */
    public void removeDataSource (String datasourceName) throws KruizeDatasourceDoesNotExist;
}
