package com.autotune.common.datasource;

import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.dataSourceMetadata.DataSourceCluster;
import com.autotune.common.data.dataSourceMetadata.DataSource;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataInfo;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * DataSourceMetadataValidation class validates the structure and mandatory fields of a DataSourceMetadataInfo object
 * following the json hierarchy
 */
public class DataSourceMetadataValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceMetadataValidation.class);
    private boolean success;
    private String errorMessage;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void markFailed(String message) {
        setSuccess(false);
        setErrorMessage(message);
    }

    /**
     * Validates the given DataSourceMetadataInfo object for mandatory fields like "datasources" and "datasource_name".
     *
     * @param dataSourceMetadataInfo the DataSourceMetadataInfo object to validate.
     */
    public void validate(DataSourceMetadataInfo dataSourceMetadataInfo) {
        List<String> missingMandatoryFields = new ArrayList<>();
        try {
            if (null == dataSourceMetadataInfo || null == dataSourceMetadataInfo.getDataSourceHashMap()) {
                String errorMsg = AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_METADATA_CONNECTION_FAILED;
                markFailed(errorMsg);
                return;
            }

            for (Map.Entry<String, DataSource> entry : dataSourceMetadataInfo.getDataSourceHashMap().entrySet()) {
                DataSource dataSource = entry.getValue();
                if (null == dataSource.getDataSourceName()) {
                    missingMandatoryFields.add(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.DATASOURCE_NAME);
                    String errorMsg = String.format(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_VALIDATION_FAILURE_MSG, missingMandatoryFields);
                    markFailed(errorMsg);
                    return;
                }
                validateDataSourceCluster(dataSource, missingMandatoryFields);
                if (!missingMandatoryFields.isEmpty()) {
                    String errorMsg = String.format(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_VALIDATION_FAILURE_MSG, missingMandatoryFields);
                    markFailed(errorMsg);
                    return;
                }
            }

            setSuccess(true);
        } catch (Exception e) {
            LOGGER.error("Validation error: " + e.getMessage());
            e.printStackTrace();
            markFailed("Validation error: " + e.getMessage());
        }
    }

    /**
     * Validates the given DataSource object for mandatory fields like "clusters" and "cluster_name".
     *
     * @param dataSource the DataSource object to validate.
     * @param missingMandatoryFields the list to which any missing fields will be added.
     */
    private void validateDataSourceCluster(DataSource dataSource, List<String> missingMandatoryFields) {
        if (null == dataSource.getDataSourceClusterHashMap()) {
            missingMandatoryFields.add(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CLUSTERS);
        } else {
            for (Map.Entry<String, DataSourceCluster> entry : dataSource.getDataSourceClusterHashMap().entrySet()) {
                DataSourceCluster dataSourceCluster = entry.getValue();
                if (null == dataSourceCluster.getDataSourceClusterName()) {
                    missingMandatoryFields.add(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CLUSTER_NAME);
                }
            }
        }
    }
}