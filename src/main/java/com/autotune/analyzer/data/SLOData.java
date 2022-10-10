package com.autotune.analyzer.model;

import java.util.List;

public class SLOData {
    private String objective_function;
    private String slo_class;
    private String direction;
    private List<QueryData> function_variables;

    public SLOData(String objective_function, String slo_class, String direction, List<QueryData> function_variables) {
        this.objective_function = objective_function;
        this.slo_class = slo_class;
        this.direction = direction;
        this.function_variables = function_variables;
    }

    public String getObjective_function() {
        return objective_function;
    }

    public void setObjective_function(String objective_function) {
        this.objective_function = objective_function;
    }

    public String getSlo_class() {
        return slo_class;
    }

    public void setSlo_class(String slo_class) {
        this.slo_class = slo_class;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public List<QueryData> getFunction_variables() {
        return function_variables;
    }

    public void setFunction_variables(List<QueryData> function_variables) {
        this.function_variables = function_variables;
    }
}
