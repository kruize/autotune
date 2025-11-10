package com.autotune.analyzer.Layer;


import java.util.List;

public class TypeDef {
    private Bounds bounds; // (for 'range' type)

    private List<String> choices; // Optional (for 'categorical' type)

    public TypeDef(Bounds bounds, List<String> choices) {
        this.bounds = bounds;
        this.choices = choices;
    }

    public Bounds getBounds() {
        return bounds;
    }

    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
    }

    public List<String> getChoices() {
        return choices;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }

    @Override
    public String toString() {
        return "TypeDef{" +
                "bounds=" + bounds +
                ", choices=" + choices +
                '}';
    }
}
