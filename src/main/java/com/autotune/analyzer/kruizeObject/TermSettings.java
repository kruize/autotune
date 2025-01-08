package com.autotune.analyzer.kruizeObject;

import java.util.List;

public class TermSettings {
    private List<String> terms;
    private String singleTerm;

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public String getSingleTerm() {
        return singleTerm;
    }

    public void setSingleTerm(String singleTerm) {
        this.singleTerm = singleTerm;
    }
    
    @Override
    public String toString() {
        return "TermSettings{" +
                "terms=" + terms + 
                ", singleTerm='" + singleTerm + '\'' +
                '}';
    }
}
