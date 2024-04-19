/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.serviceObjects;

import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ListDatasourcesAPIObject {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListDatasourcesAPIObject.class);
    @SerializedName(KruizeConstants.JSONKeys.VERSION)
    private String apiversion = KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.version;

    @SerializedName(KruizeConstants.JSONKeys.DATASOURCES)
    List<DataSourceInfo> dataSourceInfoList;

    public List<DataSourceInfo> getDataSourceInfoList() {
        return dataSourceInfoList;
    }

    public void setDataSourceInfoList(List<DataSourceInfo> dataSourceInfoList) {
        if (null == dataSourceInfoList) {
            LOGGER.error("No datasources found");
            return;
        }
        this.dataSourceInfoList = dataSourceInfoList;
    }
}
