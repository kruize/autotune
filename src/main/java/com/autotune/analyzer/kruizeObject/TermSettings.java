package com.autotune.analyzer.kruizeObject;

import java.util.List;

public class TermSettings {
    private List<String> terms;
    private String termDuration;

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public String getTermDuration() {
        return termDuration;
    }

    public void setTermDuration(String termDuration) {
        this.termDuration = termDuration;
    }

    @Override
    public String toString() {
        return "TermSettings{" +
                "terms=" + terms + 
                ", singleTerm='" + termDuration + '\'' +
                '}';
    }
}
