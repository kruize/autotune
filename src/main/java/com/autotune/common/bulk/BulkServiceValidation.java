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

        validationOutputData = buildErrorOutput(validateTimeRange(
                payload.getTime_range().getStart(),
                payload.getTime_range().getEnd()), jobID);
        if (validationOutputData != null) return validationOutputData;

        validationOutputData = buildErrorOutput(validateRequestId(payload.getRequestId()), jobID);
        if (validationOutputData != null) return validationOutputData;

        validationOutputData = buildErrorOutput(validateOneToOneMapping(jobID, payload), jobID);
        if (validationOutputData != null) return validationOutputData;

        validationOutputData = buildErrorOutput(validateDatasourceConnection(payload.getDatasource()), jobID);

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



    public static String validateOneToOneMapping(String jobID, BulkInput currentPayloadJson) throws Exception {
        String errorMessage = "";
        BulkInput existingPayload;
        String existingPayloadRequestId;
        String currentPayloadRequestId;
        KruizeBulkJobEntry kruizeBulkJobEntry = new ExperimentDAOImpl().findBulkJobById(jobID);
        if (null != kruizeBulkJobEntry) {
            BulkJobStatus jobDetails = kruizeBulkJobEntry.getBulkJobStatus();
            existingPayload = jobDetails.getSummary().getInput();
            existingPayloadRequestId = jobDetails.getSummary().getInput().getRequestId();
            currentPayloadRequestId = currentPayloadJson.getRequestId();

            if (currentPayloadRequestId.equals(existingPayloadRequestId)) {
                if (existingPayload != null && existingPayload.equals(currentPayloadJson)) {
                    errorMessage = String.format(KruizeConstants.KRUIZE_BULK_API.DUPLICATE_REQ_ID_WITH_SAME_PAYLOAD, currentPayloadJson.getRequestId());
                    LOGGER.error(errorMessage);
                }
            }
        }
        return errorMessage;
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
            if (op.isServiceable(dataSourceInfo) == CommonUtils.DatasourceReachabilityStatus.NOT_REACHABLE) {
                errorMessage = KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.DATASOURCE_NOT_SERVICEABLE;
                LOGGER.error(errorMessage);
            }
        } catch (Exception ex) {
            errorMessage = ex.getMessage();
            LOGGER.error(errorMessage);
        }
        return errorMessage;
    }

    public static String validateRequestId(String requestId) {
        String errorMessage = "";
        if (requestId == null) {
            errorMessage = KruizeConstants.KRUIZE_BULK_API.MISSING_REQUEST_ID;
        } else if (!requestId.matches("^[a-zA-Z0-9]{36}$")) {
            errorMessage = KruizeConstants.KRUIZE_BULK_API.INVALID_REQUEST_ID;
        }
        return errorMessage;
    }

    public static String validateTimeRange(String start, String end) {
        String errorMessage = "";
        try {
            OffsetDateTime startTime = OffsetDateTime.parse(start);
            OffsetDateTime endTime = OffsetDateTime.parse(end);

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
