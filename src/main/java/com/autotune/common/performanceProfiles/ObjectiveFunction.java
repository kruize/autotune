package com.autotune.common.performanceProfiles;

public class ObjectiveFunction {
    private String function_type;
    private String expression;


    public ObjectiveFunction(String function_type, String expression) {
        this.function_type = function_type;
        this.expression = expression;
    }

    public String getFunction_type() {
        return function_type;
    }

    public void setFunction_type(String function_type) {
        this.function_type = function_type;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "ObjectiveFunction{" +
                "function_type='" + function_type + '\'' +
                ", expression='" + expression + '\'' +
                '}';
    }
}
