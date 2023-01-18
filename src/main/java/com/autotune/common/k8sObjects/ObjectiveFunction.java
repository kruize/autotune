package com.autotune.common.k8sObjects;

public class ObjectiveFunction {
    private String type;
    private String expression;


    public ObjectiveFunction(String type, String expression) {
        this.type = type;
        this.expression = expression;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
                "type='" + type + '\'' +
                ", expression='" + expression + '\'' +
                '}';
    }
}
