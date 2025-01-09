package com.autotune.database.dao;

import com.autotune.database.table.lm.Jobs;

import java.util.UUID;

public interface BulkJobDAO {
    void saveJob(Jobs job);
    Jobs getJobById(UUID jobId);
}