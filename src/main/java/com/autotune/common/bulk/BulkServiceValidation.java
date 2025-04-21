package com.autotune.common.bulk;

import com.autotune.analyzer.serviceObjects.BulkInput;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.common.utils.CommonUtils;
import com.autotune.database.dao.ExperimentDAO;
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

        validationOutputData = buildErrorOutput(validateRefId(payload.getRequestId()), jobID);
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
            return new ValidationOutputData(false, errorMsg + " jobId: " + jobID, 400);
        }
        return null;
    }



    public static String validateOneToOneMapping(String jobID, BulkInput currentPayloadJson) throws Exception {
        String errorMsg = "";
        BulkInput existingPayload;
        ExperimentDAO experimentDAO = new ExperimentDAOImpl();
        KruizeBulkJobEntry kruizeBulkJobEntry = experimentDAO.findBulkJobById(jobID);
        // TODO: convert this back to BulkInput object to compare the values
//        if (null != kruizeBulkJobEntry) {
//            existingPayload = kruizeBulkJobEntry.getPayload();
//
//            if (existingPayload != null && !existingPayload.equals(currentPayloadJson)) {
//                LOGGER.error("Duplicate ref_id found with different payload: {}", refId);
//                throw new DuplicateReferenceIDException("ref_id already used with a different payload.");
//            }
//        }
        return errorMsg;
    }

    public static String validateDatasourceConnection(String datasourceName) {
        String errorMessage = "";
        try {
            DataSourceInfo dataSourceInfo = null;
            try {
                dataSourceInfo = new ExperimentDBService().loadDataSourceFromDBByName(datasourceName);
            } catch (Exception e) {
                errorMessage = String.format("Loading saved datasource %s details from db failed: %s", datasourceName, e.getMessage());
                LOGGER.error(errorMessage);
                return errorMessage;
            }
            LOGGER.info(KruizeConstants.DataSourceConstants.DataSourceInfoMsgs.VERIFYING_DATASOURCE_REACHABILITY, datasourceName);
            DataSourceOperatorImpl op = DataSourceOperatorImpl.getInstance().getOperator(KruizeConstants.SupportedDatasources.PROMETHEUS);
            if (op.isServiceable(dataSourceInfo) == CommonUtils.DatasourceReachabilityStatus.NOT_REACHABLE) {
                errorMessage = String.format("Connection failed for datasource: %s", datasourceName);
                LOGGER.error(errorMessage);
            }
        } catch (Exception ex) {
            errorMessage = ex.getMessage();
            LOGGER.error(errorMessage);
        }
        return errorMessage;
    }

    public static String validateRefId(String refId) {
        String errorMessage = "";
        if (refId == null || !refId.matches("^[a-zA-Z0-9]{36}$")) {
            errorMessage = "Invalid ref_id format. Must be 36-character alphanumeric.";
        }
        return errorMessage;
    }

    public static String validateTimeRange(String start, String end) {
        String errorMessage = "";
        try {
            OffsetDateTime startTime = OffsetDateTime.parse(start);
            OffsetDateTime endTime = OffsetDateTime.parse(end);

            if (startTime.isAfter(endTime)) {
                errorMessage = "Start time is after end time. Must be after start time.";
                return errorMessage;
            }

            Duration duration = Duration.between(startTime, endTime);
            if (duration.toHours() < KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY || duration.toDays() > KruizeConstants.TimeConv.MEASUREMENT_DURATION_THRESHOLD_MINUTES) {
                errorMessage = "Time range must be between 24 hours and 15 days.";
            }
        } catch (DateTimeParseException ex) {
            errorMessage = "Invalid date format. Must be ISO 8601.";
        } catch (Exception e) {
            errorMessage = "Exception occurred while validating the time range";
        }
        return errorMessage;
    }
}
