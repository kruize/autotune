package com.autotune.collection;

/**
 * Holds information about the fields in selector tag in the autotune object yaml
 */
public class SelectorInfo
{
    private String matchLabel;
    private String matchValue;
    private String matchURI;
    private String matchRoute;
    private String matchService;

    public String getMatchLabel() {
        return matchLabel;
    }

    public void setMatchLabel(String matchLabel) {
        this.matchLabel = matchLabel;
    }

    public String getMatchValue() {
        return matchValue;
    }

    public void setMatchValue(String matchValue) {
        this.matchValue = matchValue;
    }

    public String getMatchURI() {
        return matchURI;
    }

    public void setMatchURI(String matchURI) {
        this.matchURI = matchURI;
    }

    public String getMatchRoute() {
        return matchRoute;
    }

    public void setMatchRoute(String matchRoute) {
        this.matchRoute = matchRoute;
    }

    public String getMatchService() {
        return matchService;
    }

    public void setMatchService(String matchService) {
        this.matchService = matchService;
    }

    @Override
    public String toString() {
        return "SelectorInfo{" +
                "matchLabel='" + matchLabel + '\'' +
                ", matchValue='" + matchValue + '\'' +
                ", matchURI='" + matchURI + '\'' +
                ", matchRoute='" + matchRoute + '\'' +
                ", matchService='" + matchService + '\'' +
                '}';
    }
}
