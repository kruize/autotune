package com.autotune.analyzer.Layer;


public class Calculation {
    private String target;
    private String expr;
    private Object fallback;

    public Calculation(String target, String expr, Object fallback) {
        this.target = target;
        this.expr = expr;
        this.fallback = fallback;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getExpr() {
        return expr;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }

    public Object getFallback() {
        return fallback;
    }

    public void setFallback(Object fallback) {
        this.fallback = fallback;
    }

    @Override
    public String toString() {
        return "Calculation{" +
                "target='" + target + '\'' +
                ", expr='" + expr + '\'' +
                ", fallback=" + fallback +
                '}';
    }
}
