package com.autotune.database.table.lm;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.UUID;


@Entity
@Table(name = "kruize_bulkjobs")
public class BulkJob {
    @Id
    @Column(name = "job_id")
    private UUID jobId;
    private String status;
    @Column(name = "total_count")
    private int totalExperiments;
    @Column(name = "processed_count")
    private int processedExperiments;
    @Column(name = "start_time")
    private Timestamp jobStartTime;
    @Column(name = "end_time")
    private Timestamp jobEndTime;
    private String webhook;
    
    @Column(columnDefinition = "jsonb")
    private String notifications; // Stored as JSON string

    @Column(columnDefinition = "jsonb")
    private String experiments; // JSONB field for experiments data

    // Getters and Setters
}
