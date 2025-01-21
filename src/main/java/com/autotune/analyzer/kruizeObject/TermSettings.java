package com.autotune.analyzer.kruizeObject;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class TermSettings {
    private List<String> terms;
    @SerializedName("term_duration_in_days")
    private Map<String, Integer> termDurations;

    public TermSettings() {}

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public Map<String, Integer> getTermDurations() {
        return termDurations;
    }

    public void setTermDurations(Map<String, Integer> termDurations) {
        this.termDurations = termDurations;
    }

    @Override
    public String toString() {
        return "TermSettings{" +
                "terms=" + terms + 
                ", termDurations=" + termDurations +
                '}';
    }
}
