package com.autotune.database.table.lm;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "kruize_lm_layer")
public class KruizeLMLayerEntry {
    private String api_version;
    private String kind;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode metadata;
    @Id
    private String layer_name;
    private Integer layer_level;
    private String details;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode layer_presence;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode tunables;

    // Getters and setters
    public String getApi_version() {
        return api_version;
    }

    public void setApi_version(String api_version) {
        this.api_version = api_version;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    public String getLayer_name() {
        return layer_name;
    }

    public void setLayer_name(String layer_name) {
        this.layer_name = layer_name;
    }

    public Integer getLayer_level() {
        return layer_level;
    }

    public void setLayer_level(Integer layer_level) {
        this.layer_level = layer_level;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public JsonNode getLayer_presence() {
        return layer_presence;
    }

    public void setLayer_presence(JsonNode layer_presence) {
        this.layer_presence = layer_presence;
    }

    public JsonNode getTunables() {
        return tunables;
    }

    public void setTunables(JsonNode tunables) {
        this.tunables = tunables;
    }
}
