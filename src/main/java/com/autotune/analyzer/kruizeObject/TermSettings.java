package com.autotune.analyzer.kruizeObject;

import java.util.List;

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
