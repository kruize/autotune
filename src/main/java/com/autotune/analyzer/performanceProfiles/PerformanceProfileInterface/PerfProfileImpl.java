package com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfileValidation;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.metrics.Metric;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.k8sObjects.K8sObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class PerfProfileImpl implements PerfProfileInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerfProfileImpl.class);

    @Override
    public String getName(PerformanceProfile performanceProfile) {
        return AnalyzerConstants.PerformanceProfileConstants.PerfProfileNames.get(performanceProfile.getName());
    }


    /**
     * validates the performance profile fields and the data and then adds it to the map
     * @param performanceProfile
     * @return
     */
    @Override
    public ValidationOutputData validateAndAddProfile(Map<String, PerformanceProfile> performanceProfilesMap, PerformanceProfile performanceProfile) {
        ValidationOutputData validationOutputData;
        try {
            validationOutputData = new PerformanceProfileValidation(performanceProfilesMap).validate(performanceProfile);
            if (validationOutputData.isSuccess()) {
                addPerformanceProfile(performanceProfilesMap, performanceProfile);
            } else {
                validationOutputData.setMessage("Validation failed: " + validationOutputData.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Validate and add profile failed: " + e.getMessage());
            validationOutputData = new ValidationOutputData(false, "Validation failed: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return validationOutputData;
    }

    /**
     * @param performanceProfile
     * @param experimentResultData
     * @return
     */
    @Override
    public String validateResults(PerformanceProfile performanceProfile, ExperimentResultData experimentResultData) {
        String errorMsg = "";
        List<String> mandatoryFields = new ArrayList<>(Arrays.asList(
                AnalyzerConstants.MetricNameConstants.CPU_USAGE,
                AnalyzerConstants.MetricNameConstants.MEMORY_USAGE,
                AnalyzerConstants.MetricNameConstants.MEMORY_RSS
        ));
        // Get the metrics data from the Performance Profile
        List<String> aggrFunctionsObjects = new ArrayList<>();
        List<String> queryList = new ArrayList<>();
        List<String> perfProfileFunctionVariablesList = new ArrayList<>();
        for (Metric metric : performanceProfile.getSloInfo().getFunctionVariables()) {
            perfProfileFunctionVariablesList.add(metric.getName());
            if (null != metric.getAggregationFunctions()) {
                metric.getAggregationFunctions().forEach(aggregationFunctions ->
                        aggrFunctionsObjects.add(aggregationFunctions.getFunction()));
            }
            if (null != metric.getQuery())
                queryList.add(metric.getQuery());
        }
        LOGGER.debug(String.format("List of functionVariables: %s", perfProfileFunctionVariablesList));
        LOGGER.debug(String.format("List of agg func objects: %s", aggrFunctionsObjects));

        // Get the metrics data from the Kruize Object
        for (K8sObject k8sObject : experimentResultData.getKubernetes_objects()) {
            for (ContainerData containerData : k8sObject.getContainerDataList()) {
                List<Metric> metrics = containerData.getMetrics();
                List<String> kruizeFunctionVariablesList = metrics.stream().map(Metric::getName)
                        .collect(Collectors.toCollection(ArrayList::new));
                LOGGER.debug("perfProfileFunctionVariablesList: {}", perfProfileFunctionVariablesList);
                LOGGER.debug("kruizeFunctionVariablesList: {}", kruizeFunctionVariablesList);
                if (!kruizeFunctionVariablesList.containsAll(mandatoryFields)) {
                    errorMsg = errorMsg.concat(String.format("Missing one of the following mandatory parameters for experiment - %s : %s", experimentResultData.getExperiment_name(), mandatoryFields));
                    break;
                }
                for (Metric metric : metrics) {
                    Map<String, Object> aggrInfoClassAsMap;
                    if (!aggrFunctionsObjects.isEmpty()) {
                        try {
                            aggrInfoClassAsMap = convertObjectToMap(metric.getMetricResult().getAggregationInfoResult());
                            errorMsg = validateAggFunction(aggrInfoClassAsMap.keySet(), aggrFunctionsObjects);
                            if (!errorMsg.isBlank()) {
                                errorMsg = errorMsg.concat(String.format("for the experiment : %s"
                                        , experimentResultData.getExperiment_name()));
                                break;
                            }
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        // check if query is also absent
                        if (queryList.isEmpty()) {
                            errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.QUERY_FUNCTION_MISSING;
                            break;
                        }
                    }
                    // check if the 'value' is present in the result JSON
                    if (null == metric.getMetricResult().getValue()) {
                        LOGGER.debug(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_VALUE.concat(metric.getName()));
                    }
                }
            }
        }
        return errorMsg;
    }

    /**
     * @param performanceProfile
     * @param experimentResultData
     * @return
     */
    @Override
    public String recommend(PerformanceProfile performanceProfile, ExperimentResultData experimentResultData) {
        return "";
    }

    public static void addPerformanceProfile(Map<String, PerformanceProfile> performanceProfileMap, PerformanceProfile performanceProfile) {
        performanceProfileMap.put(performanceProfile.getName(), performanceProfile);
        LOGGER.info("Added PerformanceProfile: {} ",performanceProfile.getName());
    }

    /**
     * Validates the aggregation function objects against the aggregationInfoResult metrics
     *
     * @param keySet
     * @param aggrFunctionsObjects
     * @return
     */
    private String validateAggFunction(Set<String> keySet, List<String> aggrFunctionsObjects) {

        List<String> aggrInfoObjects = keySet.stream().toList();
        Set<String> missingAggFunction = new HashSet<>();
        String errorMsg = "";
        // check if none of the aggrfunctions are present in the aggrInfoObjects List
        if (aggrInfoObjects.stream().noneMatch(aggrFunctionsObjects::contains)) {
            LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_AGG_FUNCTION);
            errorMsg = errorMsg.concat(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_AGG_FUNCTION);
        } else {
            // check if some or all the values are present or not and respond accordingly
            for (String aggFuncObj : aggrFunctionsObjects) {
                if (!aggrInfoObjects.contains(aggFuncObj)) {
                    missingAggFunction.add(aggFuncObj);
                }
            }
            if (!missingAggFunction.isEmpty()) {
                LOGGER.warn("Missing Aggregation Functions: {}", missingAggFunction);
            }
        }
        return errorMsg;
    }

    /**
     * Converts the aggregationInfoResult class into Map to extract values for validation
     *
     * @param obj
     * @return
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static Map<String, Object> convertObjectToMap(Object obj) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Method[] methods = obj.getClass().getMethods();
        Map<String, Object> map = new HashMap<>();
        for (Method m : methods) {
            if (m.getName().startsWith("get") && !m.getName().startsWith("getClass")) {
                Object value = m.invoke(obj);
                if (value instanceof Double)
                    map.put(m.getName().substring(3).toLowerCase(), value);
            }
        }
        return map;
    }
}
