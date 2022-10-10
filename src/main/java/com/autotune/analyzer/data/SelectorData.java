package com.autotune.analyzer.model;

public class SelectorData {
    private String matchLabel;
    private String matchLabelValue;
    private String matchRoute;
    private String matchURI;
    private String matchService;

    public SelectorData(String matchLabel, String matchLabelValue, String matchRoute, String matchURI, String matchService) {
        this.matchLabel = matchLabel;
        this.matchLabelValue = matchLabelValue;
        this.matchRoute = matchRoute;
        this.matchURI = matchURI;
        this.matchService = matchService;
    }

    public String getMatchLabel() {
        return matchLabel;
    }

    public void setMatchLabel(String matchLabel) {
        this.matchLabel = matchLabel;
    }

    public String getMatchLabelValue() {
        return matchLabelValue;
    }

    public void setMatchLabelValue(String matchLabelValue) {
        this.matchLabelValue = matchLabelValue;
    }

    public String getMatchRoute() {
        return matchRoute;
    }

    public void setMatchRoute(String matchRoute) {
        this.matchRoute = matchRoute;
    }

    public String getMatchURI() {
        return matchURI;
    }

    public void setMatchURI(String matchURI) {
        this.matchURI = matchURI;
    }

    public String getMatchService() {
        return matchService;
    }

    public void setMatchService(String matchService) {
        this.matchService = matchService;
    }
}
