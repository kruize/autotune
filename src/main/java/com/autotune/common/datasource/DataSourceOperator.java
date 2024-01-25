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

import java.util.ArrayList;

/**
 * DataSourceOperator is an abstraction which has a generic and implementation,
 * and it can also be implemented by each data source provider type.
 *
 * Currently Supported Implementations:
 *  - Prometheus
 *
 *  The Implementation should have helper functions to perform operations related
 *  to datasource
 */

public interface DataSourceOperator {
    String getDefaultServicePortForProvider();
    DataSourceOperator getOperator(String provider);
    CommonUtils.DatasourceReachabilityStatus isServiceable(String dataSourceUrl);
    Object getValueForQuery(String url, String query);
}
