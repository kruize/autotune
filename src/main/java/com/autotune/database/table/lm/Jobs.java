package com.autotune.database.table.lm;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "kruize_jobs")
public class Jobs {
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

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Column(name = "meta_data")
    private List<JobMetaData> jobMetaData;

    // Getters and Setters
}
