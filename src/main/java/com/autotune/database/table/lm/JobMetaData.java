package com.autotune.database.table.lm;

import jakarta.persistence.*;

@Entity
@Table(name = "kruize_jobmetadata")
public class JobMetaData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Jobs job;

    @Column(name = "experiment_name")
    private String experimentName;

    @Column(columnDefinition = "jsonb")
    private String notification;

    @Column(name = "recommendation_status")
    private String recommendationsStatus;

    @Column(columnDefinition = "jsonb" , name="recommendation_notifications")
    private String recommendationsNotifications;

    // Getters and Setters
}
