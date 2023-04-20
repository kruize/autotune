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

import com.autotune.common.utils.CommonUtils.AutotuneDatasourceTypes;

/**
 * KruizeDataSource is an abstraction which needs to be implemented
 * by every datasource type which is used in Autotune
 *
 * Currently Supported Implementations:
 *  - Prometheus
 *
 *  The Implementation should maintain 3 variables : Name, Type and Source and
 *  should be set or get using these API's
 *
 *  var name:   Type String
 *  var type:   Type AutotuneDatasourceTypes
 *  var source: Type String
 *
 *  Example for prometheus implementation:
 *
 *  - name: Prometheus
 *  - type: AutotuneDatasourceTypes.QUERYABLE
 *  - source: http://10.98.45.63:9090
 *
 */

public interface KruizeDataSource {

    /**
     * Function to return the name of the datasource
     * @return name of the datasource
     */
    public String getName();

    /**
     * Fucntion to set the name of the datasource
     * @param name Name of the datasource
     */
    public void setName(String name);

    /**
     * Return the type of KruizeDataSource
     * @return Type of KruizeDataSource
     */
    public AutotuneDatasourceTypes getType();

    /**
     * Set the type of KruizeDataSource
     * @param type
     */
    public void setType(AutotuneDatasourceTypes type);

    /**
     * Return the source
     * The source can be an entity or a way how we access the datasource
     * Example:
     *
     *  For promethues the source would be the instance URL
     *  For a file based datasource teh source would be the location/path to the file
     * @return the source
     */
    public String getSource();

    /**
     * Set the source
     * The source can be an entity or a way how we access the datasource
     * Example:
     *
     *  For promethues the source would be the instance URL
     *  For a file based datasource teh source would be the location/path to the file
     * @param source
     */
    public void setSource(String source);
}
