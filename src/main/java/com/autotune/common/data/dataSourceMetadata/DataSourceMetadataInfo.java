package com.autotune.common.data.dataSourceMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * DataSourceMetadataInfo represents metadata information about data source(s) and its associated clusters, namespaces, workloads, and containers.
 * It encapsulates a hierarchical structure starting from DataSource to DataSourceContainer, with each nested object containing specific metadata.
 */

public class DataSourceMetadataInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceMetadataInfo.class);
    // TODO - add KruizeConstants for attributes
    /**
     * Key: Data Source name
     * Value: Associated DataSource object
     */
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

    @Override
    public String toString() {
        return "DataSourceMetadataInfo{" +
                "dataSourceHashMap=" + dataSourceHashMap +
                '}';
    }
}
