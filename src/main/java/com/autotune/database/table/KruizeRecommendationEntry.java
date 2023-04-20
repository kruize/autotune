package com.autotune.database.table;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

@Entity
@Table(name = "kruize_recommendations")
public class KruizeRecommendationEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendation_id;

    private String experiment_name;
    private Timestamp interval_end_time;
    private String cluster_name;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode extended_data;

    public String getExperiment_name() {
        return experiment_name;
    }

    public void setExperiment_name(String experiment_name) {
        this.experiment_name = experiment_name;
    }

    public Timestamp getInterval_end_time() {
        return interval_end_time;
    }

    public void setInterval_end_time(Timestamp interval_end_time) {
        this.interval_end_time = interval_end_time;
    }

    public String getCluster_name() {
        return cluster_name;
    }

    public void setCluster_name(String cluster_name) {
        this.cluster_name = cluster_name;
    }

    public JsonNode getExtended_data() {
        return extended_data;
    }

    public void setExtended_data(JsonNode extended_data) {
        this.extended_data = extended_data;
    }
}
