package com.autotune.dependencyAnalyzer.collection;

import com.autotune.dependencyAnalyzer.SupportedTypes;
import com.autotune.dependencyAnalyzer.exceptions.InvalidValueException;

import java.util.ArrayList;

/**
 * Holds information about the sla key in the autotune object yaml
 *
 * Example:
 * sla:
 *     objective_function: "transaction_response_time"
 *     sla_class: "response_time"
 *     direction: "minimize"
 *     function_variables:
 *     - name: "transaction_response_time"
 *       query: "application_org_acme_microprofile_metrics_PrimeNumberChecker_checksTimer_mean_seconds"
 *       datasource: "prometheus"
 *       value_type: "double"
 *   mode: "show"
 *   selector:
 *     matchLabel: "app.kubernetes.io/name"
 *     matchLabelValue: "petclinic-deployment"
 */
public class SlaInfo
{
    private String slaClass;
    private String objectiveFunction;
    private String direction;
    private ArrayList<FunctionVariable> functionVariables;

    public String getSlaClass() {
        return slaClass;
    }

    public void setSlaClass(String slaClass) {
        this.slaClass = slaClass;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) throws InvalidValueException
    {
        if (SupportedTypes.DIRECTIONS_SUPPORTED.contains(direction))
            this.direction = direction;
        else throw new InvalidValueException("Invalid direction for autotune kind");
    }

    public String getObjectiveFunction()
    {
        return objectiveFunction;
    }

    public void setObjectiveFunction(String objectiveFunction)
    {
        this.objectiveFunction = objectiveFunction;
    }

    public ArrayList<FunctionVariable> getFunctionVariables()
    {
        return functionVariables;
    }

    @Override
    public String toString()
    {
        return "SlaInfo{" +
                "slaClass='" + slaClass + '\'' +
                ", objectiveFunction='" + objectiveFunction + '\'' +
                ", direction='" + direction + '\'' +
                ", functionVariables=" + functionVariables +
                '}';
    }
}
