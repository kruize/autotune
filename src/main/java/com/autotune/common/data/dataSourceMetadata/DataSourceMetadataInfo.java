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

    /**
     * Key: Data Source name
     * Value: Associated DataSource object
     */
    private HashMap<String, DataSource> datasources;

    public DataSourceMetadataInfo() {
    }

    public DataSourceMetadataInfo(HashMap<String, DataSource> datasources) {
        this.datasources = datasources;
    }

    public HashMap<String, DataSource> getDatasources() {
        return datasources;
    }

    public void setDatasources(HashMap<String, DataSource> datasources) {
        if (null == datasources) {
            LOGGER.debug("No data sources found");
        }
        this.datasources = datasources;
    }

    public DataSource getDataSourceObject(String dataSourceName) {
        if (null != datasources && datasources.containsKey(dataSourceName)) {
            return datasources.get(dataSourceName);
        }
        return null;
    }

    @Override
    public String toString() {
        return "DataSourceMetadataInfo{" +
                "datasources=" + datasources +
                '}';
    }
}
