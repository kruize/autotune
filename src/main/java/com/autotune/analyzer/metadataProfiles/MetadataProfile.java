package com.autotune.analyzer.metadataProfiles;

import com.autotune.common.data.metrics.Metric;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Container class for the MetadataProfile kubernetes kind, which is used to define
 * a metadata queries
 *
 * This class provides a direct representation of MetadataProfile CRD in JSON format,
 * corresponding to the structure of MetadataProfile YAML file. It includes mandatory fields
 * for API version, kind, metadata and additional custom fields - profile_version, k8s_type and query_variables
 */

public class MetadataProfile {
    private String apiVersion;

    private String kind;

    private JsonNode metadata;

    private String name;

    private double profile_version;

    @SerializedName("k8s_type")
    private String k8s_type;

    @SerializedName("query_variables")
    private ArrayList<Metric> metrics;

    public MetadataProfile(String apiVersion, String kind, JsonNode metadata,
                           double profile_version, String k8s_type, ArrayList<Metric> metrics) {

        this.apiVersion = apiVersion;
        this.kind = kind;
        this.metadata = metadata;
        this.profile_version = profile_version;
        this.k8s_type = k8s_type;
        this.metrics = metrics;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public String getName() {
        return name;
    }

    public double getProfile_version() {
        return profile_version;
    }

    public String getK8s_type() {
        return k8s_type;
    }

    public ArrayList<Metric> getQueryVariables() {
        return new ArrayList<>(metrics);
    }

    @Override
    public String toString() {
        return "MetadataProfile{" +
                "apiVersion='" + apiVersion + '\'' +
                ", kind='" + kind + '\'' +
                ", metadata=" + metadata +
                ", name='" + name + '\'' +
                ", profile_version=" + profile_version +
                ", k8s_type='" + k8s_type + '\'' +
                ", query_variables=" + metrics +
                '}';
    }

}
