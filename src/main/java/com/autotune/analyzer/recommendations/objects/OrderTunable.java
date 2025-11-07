package com.autotune.analyzer.recommendations.objects;

import java.util.Objects;

public class OrderTunable {
    public String name; // metric-name
    public String layer; // layer-name
    public String expression; // tunable expression

    public OrderTunable(String name, String layer, String expression) {
        this.name = name;
        this.layer = layer;
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    public String getLayer() {
        return layer;
    }
    public String getName() {
        return name;
    }

    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderTunable that = (OrderTunable) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(layer, that.layer) &&
                Objects.equals(expression, that.expression);
    }
    public int hashCode() {
        return Objects.hash(name, layer, expression);
    }

}
