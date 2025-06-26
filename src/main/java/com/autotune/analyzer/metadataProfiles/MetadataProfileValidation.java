/*******************************************************************************
 * Copyright (c) 2024, 2024 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.analyzer.metadataProfiles;

import com.autotune.analyzer.metadataProfiles.utils.MetadataProfileUtil;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.metrics.AggregationFunctions;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.datasource.DataSourceCollection;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.KruizeSupportedTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.autotune.analyzer.metadataProfiles.utils.MetadataProfileUtil.validateResultIdentifiers;
import static com.autotune.analyzer.metadataProfiles.utils.MetadataProfileUtil.matchSumByClause;

/**
 *  This class validates MetadataProfile fields and object
 */
public class MetadataProfileValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataProfileValidation.class);
    private boolean success;
    private String errorMessage;
    private final Map<String, MetadataProfile> metadataProfilesMap;

    //Mandatory fields for MetadataProfile
    private final List<String> mandatoryMetadataFields = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.API_VERSION,
            AnalyzerConstants.KIND,
            AnalyzerConstants.AutotuneObjectConstants.METADATA,
            AnalyzerConstants.AutotuneObjectConstants.DATASOURCE,
            AnalyzerConstants.MetadataProfileConstants.QUERY_VARIABLES
    ));

    private final List<String> mandatoryQueryVariables = new ArrayList<>(Arrays.asList(
            AnalyzerConstants.AutotuneObjectConstants.NAME,
            AnalyzerConstants.MetadataProfileConstants.VALUE_TYPE
    ));

    public MetadataProfileValidation(Map<String, MetadataProfile> metadataProfilesMap) {
        this.metadataProfilesMap = metadataProfilesMap;
    }

    /**
     * Validates query variables
     *
     * @param metadataProfile Metadata Profile Object to be validated
     * @return Returns the ValidationOutputData containing the response based on the validation
     */
    public ValidationOutputData validate(MetadataProfile metadataProfile) {

        return validateMetadataProfileData(metadataProfile);
    }

    /**
     * Validates the data present in the metadata profile object before adding it to the map
     * @param metadataProfile Metadata Profile Object to be validated
     * @return Returns the ValidationOutputData containing the response based on the validation
     */
    private ValidationOutputData validateMetadataProfileData(MetadataProfile metadataProfile) {
        ValidationOutputData validationOutputData;
        StringBuilder errorString = new StringBuilder();

        validationOutputData = commonMetadataProfileValidation(metadataProfile, errorString);

        if (validationOutputData.isSuccess() && errorString.isEmpty()) {
            try {
                new ExperimentDBService().loadAllMetadataProfiles(metadataProfilesMap);
            } catch (Exception e) {
                LOGGER.error(KruizeConstants.MetadataProfileConstants.MetadataProfileErrorMsgs.LOAD_METADATA_PROFILES_FROM_DB_FAILURE, e.getMessage());
            }

            // Check if the metadata profile already exists
            if (null != metadataProfile.getMetadata() && null != metadataProfilesMap.get(metadataProfile.getMetadata().get(KruizeConstants.JSONKeys.NAME).asText())) {
                errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.DUPLICATE_METADATA_PROFILE)
                        .append(metadataProfile.getMetadata().get(KruizeConstants.JSONKeys.NAME).asText());
                return new ValidationOutputData(false, errorString.toString(), HttpServletResponse.SC_CONFLICT);
            }
        }

        if (!errorString.toString().isEmpty()) {
            validationOutputData.setSuccess(false);
            validationOutputData.setMessage(errorString.toString());
            validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
        } else if (validationOutputData.isSuccess()) {
            validationOutputData.setSuccess(true);
        }
        return validationOutputData;
    }

    /**
     * Check if all mandatory values are present.
     *
     * @param metadataObj Mandatory fields of this Metadata Profile Object will be validated
     * @return ValidationOutputData object containing status of the validations
     */
    public ValidationOutputData validateMandatoryMetadataProfileFieldsAndData(MetadataProfile metadataObj) {
        List<String> missingMandatoryFields = new ArrayList<>();
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        String errorMsg;
        errorMsg = "";
        mandatoryMetadataFields.forEach(
                mField -> {
                    String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                    try {
                        LOGGER.debug(AnalyzerConstants.CommonProfileMsgs.METHOD_NAME,methodName);
                        Method getNameMethod = metadataObj.getClass().getMethod(methodName);
                        if (null == getNameMethod.invoke(metadataObj) || getNameMethod.invoke(metadataObj).toString().isEmpty())
                            missingMandatoryFields.add(mField);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        LOGGER.error(AnalyzerConstants.CommonProfileMsgs.INVALID_METHOD_NAME, mField);
                    }
                }
        );
        if (missingMandatoryFields.isEmpty()) {
            try {
                String mandatoryMetadataPerf = AnalyzerConstants.MetadataProfileConstants.METADATA_PROFILE_NAME;
                try {
                    JsonNode metadata = metadataObj.getMetadata();
                    String metadataProfileName = metadata.get(mandatoryMetadataPerf).asText();
                    if (null == metadataProfileName || metadataProfileName.isEmpty() || metadataProfileName.equals("null")) {
                        missingMandatoryFields.add(mandatoryMetadataPerf);
                    }
                } catch (Exception e) {
                    LOGGER.error(AnalyzerConstants.CommonProfileMsgs.INVALID_METHOD_NAME, mandatoryMetadataPerf);
                }

                if (missingMandatoryFields.isEmpty()) {
                    mandatoryQueryVariables.forEach(
                            mField -> {
                                String methodName = "get" + mField.substring(0, 1).toUpperCase() + mField.substring(1);
                                try {
                                    LOGGER.debug(AnalyzerConstants.CommonProfileMsgs.METHOD_NAME, methodName);
                                    Method getNameMethod = metadataObj.getQueryVariables().get(0)
                                            .getClass().getMethod(methodName);
                                    if (getNameMethod.invoke(metadataObj.getQueryVariables().get(0)) == null)
                                        missingMandatoryFields.add(mField);
                                } catch (NoSuchMethodException | IllegalAccessException |
                                         InvocationTargetException e) {
                                    LOGGER.error(AnalyzerConstants.CommonProfileMsgs.INVALID_METHOD_NAME, mField);
                                }

                            });
                    validationOutputData.setSuccess(true);

                }

                if (!missingMandatoryFields.isEmpty()) {
                    errorMsg = errorMsg.concat(String.format(AnalyzerConstants.CommonProfileMsgs.MISSING_MANDATORY_PARAMETERS, missingMandatoryFields));
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage(errorMsg);
                    validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
                    LOGGER.error(AnalyzerConstants.CommonProfileMsgs.VALIDATION_ERROR_MSG, errorMsg);
                }
            } catch (Exception e) {
                validationOutputData.setSuccess(false);
                errorMsg = errorMsg.concat(e.getMessage());
                validationOutputData.setMessage(errorMsg);
                validationOutputData.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            errorMsg = errorMsg.concat(String.format(AnalyzerConstants.CommonProfileMsgs.MISSING_MANDATORY_PARAMETERS, missingMandatoryFields));
            validationOutputData.setSuccess(false);
            validationOutputData.setMessage(errorMsg);
            validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
            LOGGER.error(AnalyzerConstants.CommonProfileMsgs.VALIDATION_ERROR_MSG, errorMsg);
        }

        return validationOutputData;
    }

    /**
     *  Validates fields like k8s_type and query_variables
     * @param metadataProfile Metadata Profile object to be validated
     * @param errorString   StringBuilder to collect error messages during validation of multiple fields
     */
    private void validateCommonProfileFields(MetadataProfile metadataProfile, StringBuilder errorString){
        // Check if k8s type is supported
        String k8sType = metadataProfile.getK8s_type();
        if (!KruizeSupportedTypes.K8S_TYPES_SUPPORTED.contains(k8sType)) {
            errorString.append(AnalyzerConstants.MetadataProfileConstants.K8S_TYPE).append(k8sType)
                    .append(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED);
        }


        // Check if query_variables is empty
        if (metadataProfile.getQueryVariables().isEmpty())
            errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.QUERY_VARIABLES_EMPTY);


        for (Metric queryVariable : metadataProfile.getQueryVariables()) {
            // Check if datasource is supported
            if (!KruizeSupportedTypes.MONITORING_AGENTS_SUPPORTED.contains(queryVariable.getDatasource().toLowerCase())) {
                errorString.append(AnalyzerConstants.AutotuneObjectConstants.QUERY_VARIABLE)
                        .append(queryVariable.getName())
                        .append(AnalyzerErrorConstants.AutotuneObjectErrors.DATASOURCE_NOT_SUPPORTED);
            }

            // Check if value_type is supported
            if (!KruizeSupportedTypes.VALUE_TYPES_SUPPORTED.contains(queryVariable.getValueType().toLowerCase())) {
                errorString.append(AnalyzerConstants.AutotuneObjectConstants.QUERY_VARIABLE)
                        .append(queryVariable.getName())
                        .append(AnalyzerErrorConstants.AutotuneObjectErrors.VALUE_TYPE_NOT_SUPPORTED);
            }

            // Check if kubernetes_object type is supported, set default to 'container' if it's absent.
            String kubernetes_object = queryVariable.getKubernetesObject();
            if (null == kubernetes_object)
                queryVariable.setKubernetesObject(KruizeConstants.JSONKeys.CONTAINER);
            else {
                if (!KruizeSupportedTypes.KUBERNETES_OBJECTS_SUPPORTED.contains(kubernetes_object.toLowerCase()))
                    errorString.append(AnalyzerConstants.KUBERNETES_OBJECTS).append(" ").append(kubernetes_object)
                            .append(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED);
            }
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
    @Override
    public String toString() {
        return "MetadataProfileValidation{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

    /**
     * Validates the fields of the metadata profile object before updating the existing profile
     * @param metadataProfile Metadata Profile Object to be validated
     * @return Returns the ValidationOutputData containing the response based on the validation
     */
    public ValidationOutputData validateProfileDataBeforeUpdating(MetadataProfile metadataProfile) {
        return validateMetadataProfileToBeUpdated(metadataProfile);
    }

    /**
     * Validates the metadata profile object before updating the existing profile
     * After performing the common validation of mandatory fields has an additional step of Query validations before
     * updating the existing queries
     *
     * @param metadataProfile Metadata Profile Object to be validated
     * @return Returns the ValidationOutputData containing the response based on the validation
     */
    private ValidationOutputData validateMetadataProfileToBeUpdated(MetadataProfile metadataProfile) {
        ValidationOutputData validationOutputData;
        StringBuilder errorString = new StringBuilder();

        // Perform common validations first
        validationOutputData = commonMetadataProfileValidation(metadataProfile, errorString);

        if (validationOutputData.isSuccess() && errorString.isEmpty()) {
            // Validate Metric names and expected query patterns
            validateMetricQueries(metadataProfile, errorString);
        }

        if (!errorString.toString().isEmpty()) {
            validationOutputData.setSuccess(false);
            validationOutputData.setMessage(errorString.toString());
            validationOutputData.setErrorCode(HttpServletResponse.SC_BAD_REQUEST);
        } else if (validationOutputData.isSuccess()) {
            validationOutputData.setSuccess(true);
        }
        return validationOutputData;
    }


    /**
     * Validates Metric names and expected query patterns before updating the metadata profile
     *
     * @param metadataProfile MetadataProfile object to be updated
     * @param errorString     StringBuilder to collect error messages during validation of multiple fields
     */
    public void validateMetricQueries(MetadataProfile metadataProfile, StringBuilder errorString) {
        for (Metric queryVariable : metadataProfile.getQueryVariables()) {
            String metricName = queryVariable.getName();
            String datasource = queryVariable.getDatasource();
            DataSourceInfo dataSourceInfo = null;

            try {
                dataSourceInfo = DataSourceCollection.getInstance().getDataSourcesCollection().values().iterator().next();
                if (null == dataSourceInfo) {
                    errorString.append(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.INVALID_DATASOURCE_INFO);
                    return;
                }
            } catch (Exception e) {
                LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.DATASOURCE_ACCESS_ERROR_MESSAGE, datasource, e.getMessage());
            }

            if (!AnalyzerConstants.validateMetricQueryName(metricName)) {
                errorString.append(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.INVALID_METRIC_NAME).
                        append(metricName).append(". ").append(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.SUPPORTED_QUERY_NAME_PREFIXES);
            }

            HashMap<String, AggregationFunctions> aggr_funcs = queryVariable.getAggregationFunctionsMap();

            for (Map.Entry<String, AggregationFunctions> aggregationFunctionsMap: aggr_funcs.entrySet()) {
                AggregationFunctions aggr_func = aggregationFunctionsMap.getValue();
                String query = aggr_func.getQuery();
                validateMetricQueryPattern(query, metricName, errorString, dataSourceInfo);
            }
        }
    }


    /**
     * Validates input metric query to be updated by matching the 'sum by' clause of the input query against the expected query patterns
     *
     * @param metricQuery Query to be updated
     * @param metricName  Name of the Metric to be updated
     * @param errorString StringBuilder to collect error messages during validation of multiple fields
     * @param dataSourceInfo Datasource object with datasource details to run the queries
     */
    public void validateMetricQueryPattern(String metricQuery, String metricName, StringBuilder errorString, DataSourceInfo dataSourceInfo) {
        AnalyzerConstants.MetadataProfileQueryIdentifier matchedIdentifier = matchSumByClause(metricQuery);
        if (matchedIdentifier == null) {
            errorString.append(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.INVALID_SUM_BY_CLAUSE)
                    .append(metricName)
                    .append(". ")
                    .append(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.EXPECTED_IDENTIFIERS);
            for (AnalyzerConstants.MetadataProfileQueryIdentifier pattern : AnalyzerConstants.MetadataProfileQueryIdentifier.values()) {
                errorString.append(" - ").append(pattern.getExpectedIdentifiers());
            }
        } else {
            queryValidator(metricQuery, metricName, errorString, dataSourceInfo, matchedIdentifier);
        }
    }

    /**
     * Validates the metric query by executing the query for `kruize` workload, container and verify for any syntax errors or unexpected query output
     *
     * @param metricQuery Query output to be validated
     * @param metricName  Name of the metric to be validated
     * @param errorString  StringBuilder to collect error messages during validation of multiple fields
     * @param dataSourceInfo Datasource object with datasource details to run the queries
     * @param queryIdentifier  Expected query identifier to be present in the query output
     */
    public void queryValidator(String metricQuery, String metricName, StringBuilder errorString, DataSourceInfo dataSourceInfo,
                               AnalyzerConstants.MetadataProfileQueryIdentifier queryIdentifier) {
        DataSourceOperatorImpl op = DataSourceOperatorImpl.getInstance().getOperator(dataSourceInfo.getProvider());

        metricQuery = metricQuery.replace(AnalyzerConstants.MetadataProfileConstants.ADDITIONAL_LABEL, "")
                .replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, String.valueOf(AnalyzerConstants.DEFAULT_MEASUREMENT_DURATION_INT));

        try {
            String metricIdentifier = MetadataProfileUtil.getMetricIdentifier(metricName);
            String filterString = MetadataProfileUtil.formatMetricFilterString(metricIdentifier);
            String updatedQuery = MetadataProfileUtil.appendFiltersToQuery(metricQuery, filterString);

            JsonArray resultArray = op.getResultArrayForQuery(dataSourceInfo, updatedQuery);

            if (!validateResultIdentifiers(resultArray, queryIdentifier)) {
                String errorMessage = String.format(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.INVALID_QUERY_NO_RESULT, metricName);
                errorString.append(errorMessage);
            }
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            LOGGER.error(e.getMessage());
            errorString.append(e.getMessage())
                    .append(String.format(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.QUERY_SYNTAX_ERROR_MESSAGE, metricName));
        }
    }

    /**
     * Common validation logic for MetadataProfile objects.
     * Handles mandatory field validation, metadata existence, and common profile field validation.
     *
     * @param metadataProfile The MetadataProfile object to validate.
     * @param errorString     A StringBuilder to append validation errors to.
     * @return A ValidationOutputData object indicating the initial success/failure of common validations.
     */
    private ValidationOutputData commonMetadataProfileValidation (MetadataProfile metadataProfile, StringBuilder errorString) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        try {
            // Validate mandatory fields first
            validationOutputData = validateMandatoryMetadataProfileFieldsAndData(metadataProfile);

            // If mandatory values are present, proceed for further validation
            if (validationOutputData.isSuccess()) {
                // Check if profile metadata exists
                JsonNode metadata = metadataProfile.getMetadata();
                if (null == metadata) {
                    errorString.append(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_METADATA_PROFILE_METADATA);
                }

                // Validates fields like k8s_type and slo object
                validateCommonProfileFields(metadataProfile, errorString);
            }
        } catch (Exception e) {
            validationOutputData.setSuccess(false);
            validationOutputData.setMessage(errorString.toString());
            validationOutputData.setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return validationOutputData;
    }

}
