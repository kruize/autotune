package com.autotune.analyzer.kruizeObject;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class TermSettings {
    private List<String> terms;
    @SerializedName("term_details")
    private Map<String, TermDetails> termDetails;

    public TermSettings() {}

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public Map<String, TermDetails> getTermDetails() {
        return termDetails;
    }

    public void setTermDetails(Map<String, TermDetails> termDetails) {
        this.termDetails = termDetails;
    }

    @Override
    public String toString() {
        return "TermSettings{" +
                "terms=" + terms + 
                ", termDetails=" + termDetails +
                '}';
    }
}
