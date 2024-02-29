package com.autotune.analyzer.serviceObjects;

import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;

public class ListDataSourcesAPIObject {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListDataSourcesAPIObject.class);
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
        }
        this.dataSourceInfoList = dataSourceInfoList;
    }

}
