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
