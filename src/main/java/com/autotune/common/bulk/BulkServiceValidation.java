package com.autotune.common.bulk;

import com.autotune.analyzer.serviceObjects.BulkInput;
import com.autotune.analyzer.serviceObjects.BulkJobStatus;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.common.utils.CommonUtils;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.database.table.lm.KruizeBulkJobEntry;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public class BulkServiceValidation {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkServiceValidation.class);

    public static ValidationOutputData validate(BulkInput payload, String jobID) throws Exception {

        ValidationOutputData validationOutputData;

        validationOutputData = buildErrorOutput(validateTimeRange(payload.getTime_range()), jobID);
        if (validationOutputData != null) return validationOutputData;

        if (payload.getDatasource() != null) {
            validationOutputData = buildErrorOutput(validateDatasourceConnection(payload.getDatasource()), jobID);
        }

        if (validationOutputData == null) {
            validationOutputData = new ValidationOutputData(true, "", 200);
        }
        return validationOutputData;
    }

    private static ValidationOutputData buildErrorOutput(String errorMsg, String jobID) {
        if (errorMsg != null && !errorMsg.isEmpty()) {
            return new ValidationOutputData(false, errorMsg + " for the jobId: " + jobID, 400);
        }
        return null;
    }


    public static String validateDatasourceConnection(String datasourceName) {
        String errorMessage = "";
        try {
            DataSourceInfo dataSourceInfo = null;
            try {
                dataSourceInfo = new ExperimentDBService().loadDataSourceFromDBByName(datasourceName);
            } catch (Exception e) {
                errorMessage = String.format(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.LOAD_DATASOURCE_FROM_DB_ERROR, datasourceName, e.getMessage());
                LOGGER.error(errorMessage);
                return errorMessage;
            }
            LOGGER.info(KruizeConstants.DataSourceConstants.DataSourceInfoMsgs.VERIFYING_DATASOURCE_REACHABILITY, datasourceName);
            DataSourceOperatorImpl op = DataSourceOperatorImpl.getInstance().getOperator(KruizeConstants.SupportedDatasources.PROMETHEUS);
            if (dataSourceInfo == null || op.isServiceable(dataSourceInfo) == CommonUtils.DatasourceReachabilityStatus.NOT_REACHABLE) {
                errorMessage = KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.DATASOURCE_NOT_SERVICEABLE;
                LOGGER.error(errorMessage);
            }
        } catch (Exception ex) {
            errorMessage = ex.getMessage();
            LOGGER.error(errorMessage);
        }
        return errorMessage;
    }

    public static String validateTimeRange(BulkInput.TimeRange timeRange) {
        String errorMessage = "";
        if (timeRange == null || timeRange.isEmpty()) {
            LOGGER.debug("No time range specified");
            return errorMessage;
        }
        try {
            OffsetDateTime startTime = OffsetDateTime.parse(timeRange.getStart());
            OffsetDateTime endTime = OffsetDateTime.parse(timeRange.getEnd());

            if (startTime.isAfter(endTime)) {
                errorMessage = KruizeConstants.KRUIZE_BULK_API.INVALID_START_TIME;
                return errorMessage;
            }

            Duration duration = Duration.between(startTime, endTime);
            if (duration.toHours() < KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY || duration.toDays() > KruizeConstants.TimeConv.MEASUREMENT_DURATION_THRESHOLD_MINUTES) {
                errorMessage = KruizeConstants.KRUIZE_BULK_API.INVALID_TIME_RANGE;
            }
        } catch (DateTimeParseException ex) {
            errorMessage = KruizeConstants.KRUIZE_BULK_API.INVALID_DATE_FORMAT;
        } catch (Exception e) {
            errorMessage = KruizeConstants.KRUIZE_BULK_API.TIME_RANGE_EXCEPTION;
        }
        return errorMessage;
    }
}
