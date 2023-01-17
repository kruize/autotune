package com.autotune.common.performanceProfiles;

import com.autotune.common.data.result.ContainerResultData;
import com.autotune.common.data.result.DeploymentResultData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.GeneralInfoResult;
import com.autotune.common.k8sObjects.*;
import com.autotune.common.performanceProfiles.perfProfileInterface.PerfProfileInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ResourceOptimizationOpenShift implements PerfProfileInterface {

    private PerformanceProfile performanceProfile;

    @Override
    public boolean validate(KruizeObject kruizeObject) {

        //TODO: Need to update the below code
        boolean proceed = false;
        String errorMsg = "";
        // Get the metrics data from the Performance Profile
        PerformanceProfile performanceProfile = PerformanceProfilesDeployment.performanceProfileMap
                .get(kruizeObject.getPerformanceProfile());
        String query;
        List<String> aggrFunctionsObjects = new ArrayList<>();
        List<String> perfProfileFunctionVariablesList = new ArrayList<>();
        for (Metric metric:performanceProfile.getSloInfo().getFunctionVariables()) {
            query = metric.getQuery();
            metric.getAggregationFunctionsMap().forEach((funcVariable, aggregationFunctionsList) -> {
                perfProfileFunctionVariablesList.add(funcVariable);
                aggregationFunctionsList.forEach(aggregationFunctions -> {
                    aggrFunctionsObjects.add(aggregationFunctions.getFunction());
                });
            });
        }
        System.out.println("\nList of functionVariables: "+perfProfileFunctionVariablesList);
        System.out.println("\nList of agg func objects: "+aggrFunctionsObjects);

        // Get the metrics data from the Kruize Object
        for ( ExperimentResultData experimentResultData : kruizeObject.getResultData()) {
            for (DeploymentResultData deploymentResultData : experimentResultData.getDeployments()) {
                for (ContainerResultData containerResultData : deploymentResultData.getContainers()) {
                    HashMap<String, HashMap<String, HashMap<String, GeneralInfoResult>>> containerMetricsMap =
                            containerResultData.getContainer_metrics();
                    List<String> kruizeFunctionVariablesList = containerMetricsMap.keySet().stream().toList();
                    if (!(perfProfileFunctionVariablesList.size() == kruizeFunctionVariablesList.size() &&
                        perfProfileFunctionVariablesList.containsAll(kruizeFunctionVariablesList) &&
                        kruizeFunctionVariablesList.containsAll(perfProfileFunctionVariablesList))) {

                        errorMsg = errorMsg.concat(String.format("Performance Profile parameters missing for experiment : %s", kruizeObject.getExperimentName()));
                        break;
                    }


                }
            }
        }

        return false;
    }

    @Override
    public void recommend() {

    //TODO: Will be updated once Kusuma's algo is completed

    }

    public PerformanceProfile getPerformanceProfile() {
        return performanceProfile;
    }

    public void setPerformanceProfile(PerformanceProfile performanceProfile) {
        this.performanceProfile = performanceProfile;
    }
}
