package com.autotune.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

/*
      selector:
    matchLabel: "app.kubernetes.io/name"
    matchValue: "acmeair"
    matchRoute: ""
    matchRUI: ""
    matchService: "https"
 */
@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class AutotuneSelector implements KubernetesResource
{
   private String matchLabel;
   private String matchValue;
   private String matchRoute;
   private String matchURI;
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

    @Override
    public String toString() {
        return "AutotuneSelector{" +
                "matchLabel='" + matchLabel + '\'' +
                ", matchValue='" + matchValue + '\'' +
                ", matchRoute='" + matchRoute + '\'' +
                ", matchURI='" + matchURI + '\'' +
                ", matchService='" + matchService + '\'' +
                '}';
    }
}
