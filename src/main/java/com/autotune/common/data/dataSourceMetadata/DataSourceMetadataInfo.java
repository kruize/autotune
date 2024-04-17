package com.autotune.common.data.dataSourceMetadata;

import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * DataSourceMetadataInfo represents metadata information about data source(s) and its associated clusters, namespaces, workloads, and containers.
 * It encapsulates a hierarchical structure starting from DataSource to DataSourceContainer, with each nested object containing specific metadata.
 */

public class DataSourceMetadataInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceMetadataInfo.class);

    /**
     * Key: Data Source name
     * Value: Associated DataSource object
     */
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.DATASOURCES)
    private HashMap<String, DataSource> dataSourceHashMap;

    public DataSourceMetadataInfo(HashMap<String, DataSource> dataSourceHashMap) {
        this.dataSourceHashMap = dataSourceHashMap;
    }

    public HashMap<String, DataSource> getDataSourceHashMap() {
        return dataSourceHashMap;
    }

    public void setDataSourceHashMap(HashMap<String, DataSource> dataSourceHashMap) {
        if (null == dataSourceHashMap) {
            LOGGER.debug("No data sources found");
        }
        this.dataSourceHashMap = dataSourceHashMap;
    }
    public DataSource getDataSourceObject(String dataSourceName) {
        if (null != dataSourceHashMap && dataSourceHashMap.containsKey(dataSourceName)) {
            return dataSourceHashMap.get(dataSourceName);
        }
        return null;
    }

    @Override
    public String toString() {
        return "DataSourceMetadataInfo{" +
                "datasources=" + dataSourceHashMap +
                '}';
    }
}
