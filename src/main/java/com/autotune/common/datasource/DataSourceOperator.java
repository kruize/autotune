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

import com.autotune.utils.KruizeConstants;

public class DataSourceOperator {
    private DataSourceOperator() { }

    public static KruizeDataSourceOperator getOperator(String datasource) {
        if (datasource.equalsIgnoreCase(KruizeConstants.SupportedDatasources.PROMETHEUS)) {
            return PrometheusDataOperator.getInstance();
        }
        return null;
    }
}
