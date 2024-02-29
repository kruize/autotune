package com.autotune.analyzer.serviceObjects;

import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

public class ImportDSMetadataAPIObject {
    private String version;
    @SerializedName(KruizeConstants.JSONKeys.DATASOURCE_NAME)
    private String dataSourceName;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

}
