package com.autotune.analyzer.kruizeObject;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class TermSettings {
    private List<String> terms;

    public TermSettings() {}

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    @Override
    public String toString() {
        return "TermSettings{" +
                "terms=" + terms +
                '}';
    }
}
