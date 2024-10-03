package com.autotune.database.table;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "kruize_authentication")
public class KruizeAuthenticationEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String authenticationType;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode credentials;

    @Column(name = "service_type")
    private String serviceType; // e.g., "datasource", "cloudwatch", etc.

    public KruizeAuthenticationEntry(String authenticationType, JsonNode credentials, String serviceType) {
        this.authenticationType = authenticationType;
        this.credentials = credentials;
        this.serviceType = serviceType;
    }

    public KruizeAuthenticationEntry() {
    }

    public Long getId() {
        return id;
    }

    // Getters and Setters for all fields
    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
    }

    public JsonNode getCredentials() {
        return credentials;
    }

    public void setCredentials(JsonNode credentials) {
        this.credentials = credentials;
    }
}
