package com.autotune.analyzer.Layer;


public class Bounds {

    private double lower;
    private double upper;
    private double step;

    public Bounds(double lower, double upper, double step) {
        this.lower = lower;
        this.upper = upper;
        this.step = step;
    }

    public double getLower() {
        return lower;
    }

    public void setLower(double lower) {
        this.lower = lower;
    }

    public double getUpper() {
        return upper;
    }

    public void setUpper(double upper) {
        this.upper = upper;
    }

    public double getStep() {
        return step;
    }

    public void setStep(double step) {
        this.step = step;
    }

    @Override
    public String toString() {
        return "Bounds{" +
                "lower=" + lower +
                ", upper=" + upper +
                ", step=" + step +
                '}';
    }
}
