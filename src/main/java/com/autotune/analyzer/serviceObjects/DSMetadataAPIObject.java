package com.autotune.analyzer.serviceObjects;

import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

public class DSMetadataAPIObject {
    private String version;
    @SerializedName(KruizeConstants.JSONKeys.DATASOURCE)
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
    public boolean validateInputFields() {
        if (version == null || version.isEmpty() || dataSourceName == null || dataSourceName.isEmpty()) {
            throw new IllegalArgumentException("Invalid input fields: version and datasource cannot be null or empty");
        }
        return true;
    }

}
