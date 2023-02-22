package com.autotune.common.performanceProfiles.PerformanceProfileInterface;

import com.autotune.analyzer.utils.PerformanceProfileValidation;
import com.autotune.common.data.ValidationResultData;
import com.autotune.common.data.result.Containers;
import com.autotune.common.data.result.DeploymentResultData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.Results;
import com.autotune.common.k8sObjects.Metric;
import com.autotune.common.performanceProfiles.PerformanceProfile;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.AnalyzerErrorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class PerfProfileImpl implements PerfProfileInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerfProfileImpl.class);

    @Override
    public String getName(PerformanceProfile performanceProfile) {
        String name = AnalyzerConstants.PerformanceProfileConstants.PerfProfileNames.get(performanceProfile.getName());
        return name;
    }


    /**
     * validates the performance profile fields and the data and then adds it to the map
     * @param performanceProfile
     * @return
     */
    @Override
    public ValidationResultData validateAndAddProfile(Map<String, PerformanceProfile> performanceProfilesMap, PerformanceProfile performanceProfile) {
        ValidationResultData validationResultData = new ValidationResultData(false, null);
        try {
            PerformanceProfileValidation performanceProfileValidation = new PerformanceProfileValidation(performanceProfilesMap);
            performanceProfileValidation.validate(performanceProfile);
            if (performanceProfileValidation.isSuccess()) {
                addPerformanceProfile(performanceProfilesMap, performanceProfile);
                validationResultData.setSuccess(true);
            } else {
                validationResultData.setSuccess(false);
                validationResultData.setMessage("Validation failed due to " + performanceProfileValidation.getErrorMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Validate and add profile falied due to : " + e.getMessage());
            validationResultData.setSuccess(false);
            validationResultData.setMessage("Validation failed due to " + e.getMessage());
        }
        return validationResultData;
    }

    /**
     * @param performanceProfile 
     * @param experimentResultData
     * @return
     */
    @Override
    public String validateResults(PerformanceProfile performanceProfile, ExperimentResultData experimentResultData) {
        String errorMsg = "";
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
        for (DeploymentResultData deploymentResultData : experimentResultData.getDeployments()) {
            for (Containers containers : deploymentResultData.getContainers()) {
                HashMap<AnalyzerConstants.MetricName, HashMap<String, Results>> containerMetricsMap =
                        containers.getContainer_metrics();
                List<String> kruizeFunctionVariablesList = containerMetricsMap.keySet().stream().toList().stream().map(Enum::name).toList();
                if (!(perfProfileFunctionVariablesList.size() == kruizeFunctionVariablesList.size() &&
                        new HashSet<>(perfProfileFunctionVariablesList).containsAll(kruizeFunctionVariablesList) &&
                        new HashSet<>(kruizeFunctionVariablesList).containsAll(perfProfileFunctionVariablesList))) {
                    LOGGER.debug("perfProfileFunctionVariablesList: {}", perfProfileFunctionVariablesList);
                    LOGGER.debug("kruizeFunctionVariablesList: {}", kruizeFunctionVariablesList);
                    perfProfileFunctionVariablesList.removeAll(kruizeFunctionVariablesList);
                    errorMsg = errorMsg.concat(String.format("Following Performance Profile parameters are missing for experiment - %s : %s", experimentResultData.getExperiment_name(), perfProfileFunctionVariablesList));
                    break;
                } else {
                    for (HashMap<String, Results> funcVar : containerMetricsMap.values()) {
                        Map<String, Object> aggrInfoClassAsMap;
                        if (!aggrFunctionsObjects.isEmpty()) {
                            try {
                                aggrInfoClassAsMap = convertObjectToMap(funcVar.get("results").getAggregation_info());
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
                            // TODO: check for query and validate against value in kruize object
                            if (queryList.isEmpty()) {
                                errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.QUERY_FUNCTION_MISSING;
                                break;
                            } else if (null == funcVar.get("results").getValue()) {
                                LOGGER.warn(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_VALUE);
                                //TODO: Need to update the below code later
//                                errorMsg = AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_VALUE;
//                                break;
                            }
                        }


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
        return null;
    }

    public static void addPerformanceProfile(Map<String, PerformanceProfile> performanceProfileMap, PerformanceProfile performanceProfile) {
        performanceProfileMap.put(performanceProfile.getName(), performanceProfile);
        LOGGER.info("Added PerformanceProfile: {} ",performanceProfile.getName());
        LOGGER.info("PerformanceProfile Map: {} ",performanceProfileMap);
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
        List<String> missingAggFunction = new ArrayList<>();
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
