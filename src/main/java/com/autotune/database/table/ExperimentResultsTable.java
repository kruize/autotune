package com.autotune.database.table;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

@Entity
@Table(name = "experiment_results",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"experiment_name", "start_timestamp", "end_timestamp"})})
public class ExperimentResultsTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String experiment_name;
    private Timestamp start_timestamp;
    private Timestamp end_timestamp;
    private double duration_minutes;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode extended_data;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode meta_data;

    public String getExperiment_name() {
        return experiment_name;
    }

    public void setExperiment_name(String experiment_name) {
        this.experiment_name = experiment_name;
    }

    public Timestamp getStart_timestamp() {
        return start_timestamp;
    }

    public void setStart_timestamp(Timestamp start_timestamp) {
        this.start_timestamp = start_timestamp;
    }

    public Timestamp getEnd_timestamp() {
        return end_timestamp;
    }

    public void setEnd_timestamp(Timestamp end_timestamp) {
        this.end_timestamp = end_timestamp;
    }

    public double getDuration_minutes() {
        return duration_minutes;
    }

    public void setDuration_minutes(double duration_minutes) {
        this.duration_minutes = duration_minutes;
    }

    public JsonNode getExtended_data() {
        return extended_data;
    }

    public void setExtended_data(JsonNode extended_data) {
        this.extended_data = extended_data;
    }

    public JsonNode getMeta_data() {
        return meta_data;
    }

    public void setMeta_data(JsonNode meta_data) {
        this.meta_data = meta_data;
    }
}
