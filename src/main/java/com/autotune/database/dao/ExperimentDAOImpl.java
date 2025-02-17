/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.database.dao;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.KubernetesAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.helper.DBConstants;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.database.table.*;
import com.autotune.database.table.lm.BulkJob;
import com.autotune.database.table.lm.KruizeLMExperimentEntry;
import com.autotune.database.table.lm.KruizeLMMetadataProfileEntry;
import com.autotune.database.table.lm.KruizeLMRecommendationEntry;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import static com.autotune.database.helper.DBConstants.DB_MESSAGES.DUPLICATE_KEY;
import static com.autotune.database.helper.DBConstants.DB_MESSAGES.DUPLICATE_KEY_ALT;
import static com.autotune.database.helper.DBConstants.SQLQUERY.*;
import static com.autotune.utils.KruizeConstants.JSONKeys.CLUSTER_NAME;

public class ExperimentDAOImpl implements ExperimentDAO {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentDAOImpl.class);

    @Override
    public synchronized ValidationOutputData addExperimentToDB(KruizeExperimentEntry kruizeExperimentEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        String statusValue = "failure";
        Timer.Sample timerAddExpDB = Timer.start(MetricsConfig.meterRegistry());
        try {
            try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
                try {
                    tx = session.beginTransaction();
                    session.persist(kruizeExperimentEntry);
                    tx.commit();
                    // TODO: remove native sql query and transient
                    //updateExperimentTypeInKruizeExperimentEntry(kruizeExperimentEntry);  #Todo this function no more required and see if it can applied without using update sql
                    validationOutputData.setSuccess(true);
                    statusValue = "success";
                } catch (HibernateException e) {
                    LOGGER.error("Not able to save experiment due to {}", e.getMessage());
                    if (tx != null) tx.rollback();
                    e.printStackTrace();
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage(e.getMessage());
                    //TODO: save error to API_ERROR_LOG
                }
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save experiment due to {}", e.getMessage());
            validationOutputData.setMessage(e.getMessage());
        } finally {
            if (null != timerAddExpDB) {
                MetricsConfig.timerAddExpDB = MetricsConfig.timerBAddExpDB.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerAddExpDB.stop(MetricsConfig.timerAddExpDB);
            }
        }
        return validationOutputData;
    }

    @Override
    public ValidationOutputData addExperimentToDB(KruizeLMExperimentEntry kruizeLMExperimentEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        String statusValue = "failure";
        Timer.Sample timerAddExpDB = Timer.start(MetricsConfig.meterRegistry());
        try {
            try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
                try {
                    tx = session.beginTransaction();
                    session.persist(kruizeLMExperimentEntry);
                    tx.commit();
                    // TODO: remove native sql query and transient
                    //updateExperimentTypeInKruizeExperimentEntry(kruizeLMExperimentEntry);
                    validationOutputData.setSuccess(true);
                    statusValue = "success";
                } catch (HibernateException e) {
                    LOGGER.error("Not able to save experiment due to {}", e.getMessage());
                    if (tx != null) tx.rollback();
                    e.printStackTrace();
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage(e.getMessage());
                    //TODO: save error to API_ERROR_LOG
                }
            }
        } catch (Exception e) {
            LOGGER.debug("kruizeLMExperimentEntry={}", kruizeLMExperimentEntry);
            LOGGER.error("Not able to save experiment due to {}", e.getMessage());
            validationOutputData.setMessage(e.getMessage());
        } finally {
            if (null != timerAddExpDB) {
                MetricsConfig.timerAddExpDB = MetricsConfig.timerBAddExpDB.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerAddExpDB.stop(MetricsConfig.timerAddExpDB);
            }
        }
        return validationOutputData;
    }


    /**
     * Deletes database partitions based on a specified threshold day count.
     * <p>
     * This method iterates through all Kruize tables in the database, extracts the date part
     * from their names, and compares it with a cutoff date = currentDate - thresholdDaysCount. Tables with dates before the cutoff
     * date are eligible for deletion, and the method executes SQL statements to drop these tables.
     *
     * @param thresholdDaysCount The number of days to be used as the threshold for partition deletion.
     *                           Tables with dates older than this threshold will be deleted.
     * @throws RuntimeException if any exception occurs during the deletion process. The exception
     *                          details are logged, and the deletion process continues for other tables.
     */
    @Override
    public void deletePartitions(int thresholdDaysCount) {
        LOGGER.info("Threshold is set to {}", thresholdDaysCount);
        // Calculate the date 'daysCount' days ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -thresholdDaysCount);
        Date cutoffDate = calendar.getTime();
        String yyyyMMdd = "yyyyMMdd";
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            List<String> tablenames = session.createNativeQuery(SELECT_ALL_KRUIZE_TABLES).getResultList();
            for (String tableName : tablenames) {   // Since tableName cannot be null, there is no need to implement null handling; it can be skipped.
                String datePart = null;
                if (tableName.startsWith(DBConstants.TABLE_NAMES.KRUIZE_RESULTS + "_")) {
                    datePart = tableName.substring((DBConstants.TABLE_NAMES.KRUIZE_RESULTS + "_").length());
                } else if (tableName.startsWith(DBConstants.TABLE_NAMES.KRUIZE_RECOMMENDATIONS + "_")) {
                    datePart = tableName.substring((DBConstants.TABLE_NAMES.KRUIZE_RECOMMENDATIONS + "_").length());
                }
                if (null != datePart) {
                    Date tableDate = new SimpleDateFormat(yyyyMMdd).parse(datePart);
                    // Compare the date part with the cutoffDate  (cutoffDate = todaysDate - thresholdDaysCount)
                    if (tableDate.after(cutoffDate)) {
                        LOGGER.debug("Table not eligible for deletion: " + tableName);
                    } else {
                        LOGGER.debug("Table found for deletion: " + tableName);
                        try {
                            Transaction tx = session.beginTransaction();
                            session.createNativeQuery("DROP TABLE " + tableName).executeUpdate();
                            tx.commit();
                        } catch (Exception ignored) {
                            LOGGER.error("Exception occurred while deleting the partition: {}", ignored.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while deleting the partition: {}", e.getMessage());
        }
    }

    @Override
    public void addPartitions(String tableName, String month, String year, int dayOfTheMonth, String partitionType) {
        Transaction tx;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            // Create a YearMonth object
            YearMonth yearMonth = YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));

            // check the partition type and create corresponding query
            if (partitionType.equalsIgnoreCase(DBConstants.PARTITION_TYPES.BY_MONTH)) {
                // Get the last day of the month
                int lastDayOfMonth = yearMonth.lengthOfMonth();
                IntStream.range(dayOfTheMonth, lastDayOfMonth + 1).forEach(i -> {
                    String daterange = String.format(DB_PARTITION_DATERANGE, tableName, year, month, String.format("%02d", i), tableName,
                            year, month, String.format("%02d", i), year, month, String.format("%02d", i));
                    session.createNativeQuery(daterange).executeUpdate();
                });
            } else if (partitionType.equalsIgnoreCase(DBConstants.PARTITION_TYPES.BY_15_DAYS)) {
                IntStream.range(1, 16).forEach(i -> {
                    String daterange = String.format(DB_PARTITION_DATERANGE, tableName, year, month, String.format("%02d", i), tableName,
                            year, month, String.format("%02d", i), year, month, String.format("%02d", i));
                    session.createNativeQuery(daterange).executeUpdate();
                });
            } else if (partitionType.equalsIgnoreCase(DBConstants.PARTITION_TYPES.BY_DAY)) {  //ROS not calling this condition
                String daterange = String.format(DB_PARTITION_DATERANGE, tableName, year, month, dayOfTheMonth, tableName,
                        year, month, dayOfTheMonth, year, month, dayOfTheMonth);
                session.createNativeQuery(daterange).executeUpdate();
            } else {
                LOGGER.error(DBConstants.DB_MESSAGES.INVALID_PARTITION_TYPE);
                throw new Exception(DBConstants.DB_MESSAGES.INVALID_PARTITION_TYPE);
            }

            tx.commit();
        } catch (Exception e) {
            LOGGER.error("Exception occurred while adding the partition: {}", e.getMessage());
        }
    }

    @Override
    public ValidationOutputData addResultsToDB(KruizeResultsEntry resultsEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        String statusValue = "failure";
        Timer.Sample timerAddResultsDB = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                session.persist(resultsEntry);
                tx.commit();
                validationOutputData.setSuccess(true);
                statusValue = "success";
            } catch (PersistenceException ex) {
                if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage(
                            String.format(DBConstants.DB_MESSAGES.RECORD_ALREADY_EXISTS, resultsEntry.getExperiment_name(),
                                    resultsEntry.getInterval_start_time(), resultsEntry.getInterval_end_time())
                    );
                } else {
                    throw new Exception(ex.getMessage());
                }
            } catch (Exception e) {
                LOGGER.error("Not able to save experiment due to {}", e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save experiment due to {}", e.getMessage());
        } finally {
            if (null != timerAddResultsDB) {
                MetricsConfig.timerAddResultsDB = MetricsConfig.timerBAddResultsDB.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerAddResultsDB.stop(MetricsConfig.timerAddResultsDB);
            }
        }
        return validationOutputData;
    }


    @Override
    public List<KruizeResultsEntry> addToDBAndFetchFailedResults(List<KruizeResultsEntry> kruizeResultsEntries) {
        List<KruizeResultsEntry> failedResultsEntries = new ArrayList<>();
        Transaction tx = null;
        String statusValue = "failure";
        Timer.Sample timerAddBulkResultsDB = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            for (KruizeResultsEntry entry : kruizeResultsEntries) {
                tx = session.beginTransaction();
                try {
                    session.persist(entry);
                    session.flush();
                } catch (PersistenceException e) {
                    ConstraintViolationException constraintViolationException = null;
                    String message = "";
                    if (null != e.getCause()) {
                        constraintViolationException = (ConstraintViolationException) e.getCause();
                        message = constraintViolationException.getCause().getMessage();
                    } else {
                        message = e.getMessage();
                    }
                    LOGGER.debug(message);
                    if (message.contains(DUPLICATE_KEY) || message.contains(DUPLICATE_KEY_ALT)) {
                        entry.setErrorReasons(List.of(AnalyzerErrorConstants.APIErrors.updateResultsAPI.RESULTS_ALREADY_EXISTS));
                        failedResultsEntries.add(entry);
                    } else if (message.contains(DBConstants.DB_MESSAGES.NO_PARTITION_RELATION)) {
                        try {
                            LOGGER.debug(DBConstants.DB_MESSAGES.CREATE_PARTITION_RETRY);
                            tx.commit();
                            tx = session.beginTransaction();
                            // create partitions based on entry object
                            synchronized (new Object()) {
                                createPartitions(entry);
                            }
                            session.persist(entry);
                            session.flush();
                        } catch (Exception partitionException) {
                            LOGGER.error(partitionException.getMessage());
                            entry.setErrorReasons(List.of(partitionException.getMessage()));
                            failedResultsEntries.add(entry);
                        }
                    } else {
                        entry.setErrorReasons(List.of(e.getMessage()));
                        failedResultsEntries.add(entry);
                    }
                } catch (Exception e) {
                    entry.setErrorReasons(List.of(e.getMessage()));
                    failedResultsEntries.add(entry);
                } finally {
                    tx.commit();
                }
            }
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Not able to save experiment due to {}", e.getMessage());
            failedResultsEntries.addAll(kruizeResultsEntries);
            failedResultsEntries.forEach((entry) -> {
                entry.setErrorReasons(List.of(e.getMessage()));
            });
        } finally {
            if (null != timerAddBulkResultsDB) {
                MetricsConfig.timerAddBulkResultsDB = MetricsConfig.timerBAddBulkResultsDB.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerAddBulkResultsDB.stop(MetricsConfig.timerAddBulkResultsDB);
            }
        }
        return failedResultsEntries;
    }

    private void createPartitions(KruizeResultsEntry entry) {
        try {
            LocalDateTime localDateTime = entry.getInterval_end_time().toLocalDateTime();
            LocalDateTime newDateTime;
            int dayOfTheMonth = localDateTime.getDayOfMonth();
            // Subtract 15 days from the current date
            newDateTime = localDateTime.minus(DBConstants.PARTITION_TYPES.LAST_N_DAYS, ChronoUnit.DAYS);
            // Check if the start date is not within the same month and adjust the date accordingly
            if (newDateTime.getMonth() != localDateTime.getMonth()) {
                newDateTime = localDateTime.minusDays(DBConstants.PARTITION_TYPES.LAST_N_DAYS);
                LOGGER.debug("newDateTime: {}", newDateTime);
            }
            // create partition for the previous 15 days
            addPartitions(DBConstants.TABLE_NAMES.KRUIZE_RESULTS, String.format("%02d", newDateTime.getMonthValue()), String.valueOf(newDateTime.getYear()), newDateTime.getDayOfMonth(), DBConstants.PARTITION_TYPES.BY_MONTH);
            addPartitions(DBConstants.TABLE_NAMES.KRUIZE_RECOMMENDATIONS, String.format("%02d", newDateTime.getMonthValue()), String.valueOf(newDateTime.getYear()), newDateTime.getDayOfMonth(), DBConstants.PARTITION_TYPES.BY_MONTH);

            // check the dayOfTheMonth and create partitions accordingly
            if (dayOfTheMonth < DBConstants.PARTITION_TYPES.PARTITION_DAY) {
                addPartitions(DBConstants.TABLE_NAMES.KRUIZE_RESULTS, String.format("%02d", localDateTime.getMonthValue()), String.valueOf(localDateTime.getYear()), 1, DBConstants.PARTITION_TYPES.BY_MONTH);
                addPartitions(DBConstants.TABLE_NAMES.KRUIZE_RECOMMENDATIONS, String.format("%02d", localDateTime.getMonthValue()), String.valueOf(localDateTime.getYear()), 1, DBConstants.PARTITION_TYPES.BY_MONTH);
            } else {
                // create the partitions for the rest of the days for the current month
                // Fixing the partition type to 'by_month'
                addPartitions(DBConstants.TABLE_NAMES.KRUIZE_RESULTS, String.format("%02d", localDateTime.getMonthValue()), String.valueOf(localDateTime.getYear()), dayOfTheMonth, DBConstants.PARTITION_TYPES.BY_MONTH);
                addPartitions(DBConstants.TABLE_NAMES.KRUIZE_RECOMMENDATIONS, String.format("%02d", localDateTime.getMonthValue()), String.valueOf(localDateTime.getYear()), dayOfTheMonth, DBConstants.PARTITION_TYPES.BY_MONTH);

                // create the partitions for the next month
                YearMonth yearMonth = buildDateForNextMonth(YearMonth.of(localDateTime.getYear(), localDateTime.getMonthValue()));
                addPartitions(DBConstants.TABLE_NAMES.KRUIZE_RESULTS, String.format("%02d", yearMonth.getMonthValue()), String.valueOf(yearMonth.getYear()), 1, DBConstants.PARTITION_TYPES.BY_MONTH);
                addPartitions(DBConstants.TABLE_NAMES.KRUIZE_RECOMMENDATIONS, String.format("%02d", yearMonth.getMonthValue()), String.valueOf(yearMonth.getYear()), 1, DBConstants.PARTITION_TYPES.BY_MONTH);
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while creating partitions: ");
            e.printStackTrace();
        }
    }

    @Override
    public ValidationOutputData addRecommendationToDB(KruizeRecommendationEntry recommendationEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        String statusValue = "failure";
        Timer.Sample timerAddRecDB = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                KruizeRecommendationEntry existingRecommendationEntry = loadRecommendationsByExperimentNameAndDate(recommendationEntry.getExperiment_name(), recommendationEntry.getCluster_name(), recommendationEntry.getInterval_end_time());
                if (null == existingRecommendationEntry) {
                    tx = session.beginTransaction();
                    session.persist(recommendationEntry);
                    tx.commit();
                    validationOutputData.setSuccess(true);
                    statusValue = "success";
                } else {
                    tx = session.beginTransaction();
                    existingRecommendationEntry.setExtended_data(recommendationEntry.getExtended_data());
                    session.merge(existingRecommendationEntry);
                    tx.commit();
                    validationOutputData.setSuccess(true);
                    statusValue = "success";
                }
            } catch (Exception e) {
                LOGGER.error("Not able to save recommendation due to {}", e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save recommendation due to {}", e.getMessage());
        } finally {
            if (null != timerAddRecDB) {
                MetricsConfig.timerAddRecDB = MetricsConfig.timerBAddRecDB.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerAddRecDB.stop(MetricsConfig.timerAddRecDB);
            }
        }
        return validationOutputData;
    }

    @Override
    public ValidationOutputData addRecommendationToDB(KruizeLMRecommendationEntry recommendationEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        String statusValue = "failure";
        Timer.Sample timerAddRecDB = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                KruizeLMRecommendationEntry existingRecommendationEntry = loadLMRecommendationsByExperimentNameAndDate(recommendationEntry.getExperiment_name(), recommendationEntry.getCluster_name(), recommendationEntry.getInterval_end_time());
                if (null == existingRecommendationEntry) {
                    tx = session.beginTransaction();
                    session.persist(recommendationEntry);
                    tx.commit();
                    validationOutputData.setSuccess(true);
                    statusValue = "success";
                } else {
                    tx = session.beginTransaction();
                    existingRecommendationEntry.setExtended_data(recommendationEntry.getExtended_data());
                    session.merge(existingRecommendationEntry);
                    tx.commit();
                    validationOutputData.setSuccess(true);
                    statusValue = "success";
                }
            } catch (Exception e) {
                LOGGER.error("Not able to save recommendation due to {}", e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save recommendation due to {}", e.getMessage());
        } finally {
            if (null != timerAddRecDB) {
                MetricsConfig.timerAddRecDB = MetricsConfig.timerBAddRecDB.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerAddRecDB.stop(MetricsConfig.timerAddRecDB);
            }
        }
        return validationOutputData;
    }

    @Override
    public ValidationOutputData addPerformanceProfileToDB(KruizePerformanceProfileEntry kruizePerformanceProfileEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        String statusValue = "failure";
        Timer.Sample timerAddPerfProfileDB = Timer.start(MetricsConfig.meterRegistry());
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                session.persist(kruizePerformanceProfileEntry);
                tx.commit();
                validationOutputData.setSuccess(true);
                statusValue = "success";
            } catch (HibernateException e) {
                LOGGER.error("Not able to save performance profile due to {}", e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save performance profile due to {}", e.getMessage());
            validationOutputData.setMessage(e.getMessage());
        } finally {
            if (null != timerAddPerfProfileDB) {
                MetricsConfig.timerAddPerfProfileDB = MetricsConfig.timerBAddPerfProfileDB.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerAddPerfProfileDB.stop(MetricsConfig.timerAddPerfProfileDB);
            }
        }
        return validationOutputData;
    }

    /**
     * Adds MetricProfile to database
     *
     * @param kruizeMetricProfileEntry Metric Profile Database object to be added
     * @return validationOutputData contains the status of the DB insert operation
     */
    public ValidationOutputData addMetricProfileToDB(KruizeMetricProfileEntry kruizeMetricProfileEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        String statusValue = "failure";
        Timer.Sample timerAddMetricProfileDB = Timer.start(MetricsConfig.meterRegistry());
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                session.persist(kruizeMetricProfileEntry);
                tx.commit();
                validationOutputData.setSuccess(true);
                statusValue = "success";
            } catch (HibernateException e) {
                LOGGER.error("Not able to save metric profile due to {}", e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save metric profile due to {}", e.getMessage());
            validationOutputData.setMessage(e.getMessage());
        }
        return validationOutputData;
    }

    /**
     * Add MetadataProfile to database
     *
     * @param kruizeMetadataProfileEntry Metadata Profile Database object to be added
     * @return validationOutputData contains the status of the DB insert operation
     */
    @Override
    public ValidationOutputData addMetadataProfileToDB(KruizeLMMetadataProfileEntry kruizeMetadataProfileEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        String statusValue = "failure";
        Timer.Sample timerAddMetadataProfileDB = Timer.start(MetricsConfig.meterRegistry());
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                session.persist(kruizeMetadataProfileEntry);
                tx.commit();
                validationOutputData.setSuccess(true);
            } catch (HibernateException e) {
                LOGGER.error("Not able to save metadata profile due to {}", e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save metadata profile source due to {}", e.getMessage());
            validationOutputData.setMessage(e.getMessage());
        } finally {
            if (null != timerAddMetadataProfileDB) {
                MetricsConfig.timerAddMetadataProfileDB = MetricsConfig.timerBAddMetadataProfileDB.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerAddMetadataProfileDB.stop(MetricsConfig.timerAddMetadataProfileDB);
            }
        }

        return validationOutputData;
    }

    /**
     * @param kruizeDataSourceEntry
     * @param validationOutputData
     * @return validationOutputData contains the status of the DB insert operation
     */
    @Override
    public ValidationOutputData addDataSourceToDB(KruizeDataSourceEntry kruizeDataSourceEntry, ValidationOutputData validationOutputData) {
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                KruizeAuthenticationEntry kruizeAuthenticationEntry = session.get(KruizeAuthenticationEntry.class, validationOutputData.getAuthEntryId());
                kruizeDataSourceEntry.setKruizeAuthenticationEntry(kruizeAuthenticationEntry);
                session.persist(kruizeDataSourceEntry);
                tx.commit();
                validationOutputData.setSuccess(true);
            } catch (HibernateException e) {
                LOGGER.error("Not able to save data source due to {}", e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save data source due to {}", e.getMessage());
            validationOutputData.setMessage(e.getMessage());
        }
        return validationOutputData;
    }

    /**
     * @param kruizeDSMetadataEntry
     * @return
     */
    @Override
    public ValidationOutputData addMetadataToDB(KruizeDSMetadataEntry kruizeDSMetadataEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                session.persist(kruizeDSMetadataEntry);
                tx.commit();
                validationOutputData.setSuccess(true);
            } catch (HibernateException e) {
                LOGGER.error("Not able to save metadata due to {}", e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save metadata source due to {}", e.getMessage());
            validationOutputData.setMessage(e.getMessage());
        }
        return validationOutputData;
    }

    /**
     * @param kruizeAuthenticationEntry
     * @return
     */
    @Override
    public ValidationOutputData addAuthenticationDetailsToDB(KruizeAuthenticationEntry kruizeAuthenticationEntry) {
        LOGGER.info("kruizeAuthenticationEntry: {}", kruizeAuthenticationEntry);
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        Long authId;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                session.persist(kruizeAuthenticationEntry);
                tx.commit();
                validationOutputData.setSuccess(true);
                validationOutputData.setAuthEntryId(kruizeAuthenticationEntry.getId());
            } catch (HibernateException e) {
                LOGGER.error("Unable to save auth details: {}", e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Unable to save auth details: {}", e.getMessage());
            validationOutputData.setMessage(e.getMessage());
        }
        return validationOutputData;
    }

    @Override
    public ValidationOutputData bulkJobSave(BulkJob bulkJob) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        String statusValue = "failure";
        Timer.Sample timerSaveBulkJobDB = Timer.start(MetricsConfig.meterRegistry());
        try {
            try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
                try {
                    tx = session.beginTransaction();
                    session.saveOrUpdate(bulkJob);
                    tx.commit();
                    // TODO: remove native sql query and transient
                    validationOutputData.setSuccess(true);
                    statusValue = "success";
                } catch (HibernateException e) {
                    LOGGER.error("Not able to save experiment due to {}", e.getMessage());
                    if (tx != null) tx.rollback();
                    e.printStackTrace();
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage(e.getMessage());
                    //TODO: save error to API_ERROR_LOG
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Bulk JOB Save ={}", bulkJob);
            LOGGER.error("Not able to save BulkJob due to {}", e.getMessage());
            validationOutputData.setMessage(e.getMessage());
        } finally {
            if (null != timerSaveBulkJobDB) {
                MetricsConfig.timerSaveBulkJobDB = MetricsConfig.timerBSaveBulkJobDB.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerSaveBulkJobDB.stop(MetricsConfig.timerSaveBulkJobDB);
            }
        }
        return validationOutputData;
    }

    @Override
    public BulkJob findBulkJobById(String jobId) throws Exception {
        BulkJob bulkJob = null;
        String statusValue = "failure";
        Timer.Sample timerGetBulkJobDB = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            bulkJob = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_BULKJOBS_BY_JOB_ID, BulkJob.class)
                    .setParameter("jobId", jobId).getSingleResult();
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Not able to load bulk JOB {} due to {}", jobId, e.getMessage());
            throw new Exception("Error while loading BulkJob from database due to : " + e.getMessage());
        } finally {
            if (null != timerGetBulkJobDB) {
                MetricsConfig.timerLoadBulkJobId = MetricsConfig.timerBLoadBulkJobId.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerGetBulkJobDB.stop(MetricsConfig.timerLoadExpName);
            }
        }
        return bulkJob;
    }

    @Override
    public ValidationOutputData updateBulkJobByExperiment(String jobId, String experimentName, String notification, String recommendationJson) throws Exception {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        String statusValue = "failure";
        Timer.Sample timerGetBulkJobDB = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            // Construct JSON paths for notification and recommendations fields
            String notificationPath = "{experiments,\"" + experimentName + "\",notification}";
            String recommendationPath = "{experiments,\"" + experimentName + "\",recommendations}";

            // Native SQL query using jsonb_set for partial updates
            String sql = UPDATE_BULKJOB_BY_ID;

            // Execute the query
            session.createNativeQuery(sql)
                    .setParameter("notificationPath", notificationPath)
                    .setParameter("newNotification", notification == null ? "null" : "\"" + notification + "\"") // Handle null value
                    .setParameter("recommendationPath", recommendationPath)
                    .setParameter("newRecommendation", recommendationJson)
                    .setParameter("jobId", jobId)
                    .executeUpdate();
            validationOutputData.setSuccess(true);
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Not able to load bulk JOB {} due to {}", jobId, e.getMessage());
            validationOutputData.setMessage(e.getMessage());
            throw new Exception("Error while loading BulkJob from database due to : " + e.getMessage());
        } finally {
            if (null != timerGetBulkJobDB) {
                MetricsConfig.timerUpdateBulkJobId = MetricsConfig.timerBUpdateBulkJobId.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerGetBulkJobDB.stop(MetricsConfig.timerLoadExpName);
            }
        }
        return validationOutputData;
    }

    @Override
    public void deleteBulkJobByID(String jobId) {
        //todo
    }


    @Override
    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status) {
        kruizeObject.setStatus(status);
        // TODO   update into database
        return true;
    }

    /**
     * Delete an experiment with the name experimentName
     * This deletes the experiment from all three tables
     * kruize_experiments, kruize_results and kruize_recommendations
     * Delete from kruize_results and kruize_recommendations only if the delete from kruize_experiments succeeds.
     *
     * @param experimentName
     * @return
     */
    @Override
    public ValidationOutputData deleteKruizeExperimentEntryByName(String experimentName) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                Query query = session.createQuery(DELETE_FROM_EXPERIMENTS_BY_EXP_NAME, null);
                query.setParameter("experimentName", experimentName);
                int deletedCount = query.executeUpdate();
                if (deletedCount == 0) {
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage("KruizeExperimentEntry not found with experiment name: " + experimentName);
                } else {
                    // Remove the experiment from the Results table
                    Query kruizeResultsEntryquery = session.createQuery(DELETE_FROM_RESULTS_BY_EXP_NAME, null);
                    kruizeResultsEntryquery.setParameter("experimentName", experimentName);
                    kruizeResultsEntryquery.executeUpdate();

                    // Remove the experiment from the Recommendations table
                    Query kruizeRecommendationEntryquery = session.createQuery(DELETE_FROM_RECOMMENDATIONS_BY_EXP_NAME, null);
                    kruizeRecommendationEntryquery.setParameter("experimentName", experimentName);
                    kruizeRecommendationEntryquery.executeUpdate();
                    validationOutputData.setSuccess(true);
                }
                tx.commit();
            } catch (HibernateException e) {
                LOGGER.error("Not able to delete experiment {} due to {}", experimentName, e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to delete experiment {} due to {}", experimentName, e.getMessage());
        }
        return validationOutputData;
    }


    /**
     * Delete an experiment with the name experimentName
     * This deletes the experiment from two tables kruize_lm_experiments, kruize_recommendations
     * Delete from kruize_recommendations only if delete from kruize_experiments succeeds.
     *
     * @param experimentName
     * @return
     */
    @Override
    public ValidationOutputData deleteKruizeLMExperimentEntryByName(String experimentName) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                Query query = session.createQuery(DELETE_FROM_LM_EXPERIMENTS_BY_EXP_NAME, null);
                query.setParameter("experimentName", experimentName);
                int deletedCount = query.executeUpdate();
                if (deletedCount == 0) {
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage("KruizeLMExperimentEntry not found with experiment name: " + experimentName);
                } else {
                    // Remove the experiment from the Recommendations table
                    Query kruizeLMRecommendationEntryquery = session.createQuery(DELETE_FROM_LM_RECOMMENDATIONS_BY_EXP_NAME, null);
                    kruizeLMRecommendationEntryquery.setParameter("experimentName", experimentName);
                    kruizeLMRecommendationEntryquery.executeUpdate();
                    validationOutputData.setSuccess(true);
                }
                tx.commit();
            } catch (HibernateException e) {
                LOGGER.error("Not able to delete experiment {} due to {}", experimentName, e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to delete experiment {} due to {}", experimentName, e.getMessage());
        }
        return validationOutputData;
    }

    /**
     * Delete metadata with the name dataSourceName
     * This deletes the metadata from the KruizeDSMetadataEntry table
     *
     * @param dataSourceName
     * @return
     */
    @Override
    public ValidationOutputData deleteKruizeDSMetadataEntryByName(String dataSourceName) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                Query query = session.createQuery(DELETE_FROM_METADATA_BY_DATASOURCE_NAME, null);
                query.setParameter("dataSourceName", dataSourceName);
                int deletedCount = query.executeUpdate();

                if (deletedCount == 0) {
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage("KruizeDSMetadataEntry not found with datasource name: " + dataSourceName);
                } else {
                    validationOutputData.setSuccess(true);
                }
                tx.commit();
            } catch (HibernateException e) {
                LOGGER.error("Not able to delete metadata for datasource {} due to {}", dataSourceName, e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to delete metadata for datasource {} due to {}", dataSourceName, e.getMessage());
        }
        return validationOutputData;
    }

    /**
     * Delete metric profile with specified profile name
     * This deletes the metadata from the KruizeMetricProfileEntry table
     *
     * @param metricProfileName
     * @return
     */
    @Override
    public ValidationOutputData deleteKruizeMetricProfileEntryByName(String metricProfileName) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                Query query = session.createQuery(DELETE_FROM_METRIC_PROFILE_BY_PROFILE_NAME, null);
                query.setParameter("metricProfileName", metricProfileName);
                int deletedCount = query.executeUpdate();

                if (deletedCount == 0) {
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage(AnalyzerErrorConstants.APIErrors.DeleteMetricProfileAPI.DELETE_METRIC_PROFILE_ENTRY_NOT_FOUND_WITH_NAME + metricProfileName);
                } else {
                    validationOutputData.setSuccess(true);
                }
                tx.commit();
            } catch (HibernateException e) {
                LOGGER.error(AnalyzerErrorConstants.APIErrors.DeleteMetricProfileAPI.DELETE_METRIC_PROFILE_ENTRY_ERROR_MSG, metricProfileName, e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error(AnalyzerErrorConstants.APIErrors.DeleteMetricProfileAPI.DELETE_METRIC_PROFILE_ENTRY_ERROR_MSG, metricProfileName, e.getMessage());
        }
        return validationOutputData;
    }

    @Override
    public List<KruizeExperimentEntry> loadAllExperiments() throws Exception {
        //todo load only experimentStatus=inprogress , playback may not require completed experiments
        List<KruizeExperimentEntry> entries = null;
        String statusValue = "failure";
        Timer.Sample timerLoadAllExp = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_EXPERIMENTS, KruizeExperimentEntry.class).list();
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Not able to load experiment due to {}", e.getMessage());
            throw new Exception("Error while loading exsisting experiments from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadAllExp) {
                MetricsConfig.timerLoadAllExp = MetricsConfig.timerBLoadAllExp.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadAllExp.stop(MetricsConfig.timerLoadAllExp);
            }
        }
        return entries;
    }

    @Override
    public List<KruizeLMExperimentEntry> loadAllLMExperiments() throws Exception {
        //todo load only experimentStatus=inprogress , playback may not require completed experiments
        List<KruizeLMExperimentEntry> entries = null;
        String statusValue = "failure";
        Timer.Sample timerLoadAllExp = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_LM_EXPERIMENTS, KruizeLMExperimentEntry.class).list();
            // TODO: remove native sql query and transient
            //getExperimentTypeInKruizeExperimentEntry(entries);
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Not able to load experiment due to {}", e.getMessage());
            throw new Exception("Error while loading exsisting experiments from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadAllExp) {
                MetricsConfig.timerLoadAllExp = MetricsConfig.timerBLoadAllExp.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadAllExp.stop(MetricsConfig.timerLoadAllExp);
            }
        }
        return entries;
    }

   
    @Override
    public List<KruizeResultsEntry> loadAllResults() throws Exception {
        // TODO: load only experimentStatus=inProgress , playback may not require completed experiments
        List<KruizeResultsEntry> kruizeResultsEntries = null;
        String statusValue = "failure";
        Timer.Sample timerLoadAllResults = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            kruizeResultsEntries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_RESULTS, KruizeResultsEntry.class).list();
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Not able to load results due to: {}", e.getMessage());
            throw new Exception("Error while loading results from the database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadAllResults) {
                MetricsConfig.timerLoadAllResults = MetricsConfig.timerBLoadAllResults.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadAllResults.stop(MetricsConfig.timerLoadAllResults);
            }

        }
        return kruizeResultsEntries;
    }

    @Override
    public List<KruizeRecommendationEntry> loadAllRecommendations() throws Exception {
        List<KruizeRecommendationEntry> recommendationEntries = null;
        String statusValue = "failure";
        Timer.Sample timerLoadAllRec = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            recommendationEntries = session.createQuery(
                    DBConstants.SQLQUERY.SELECT_FROM_RECOMMENDATIONS,
                    KruizeRecommendationEntry.class).list();
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Not able to load recommendations due to {}", e.getMessage());
            throw new Exception("Error while loading existing recommendations from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadAllRec) {
                MetricsConfig.timerLoadAllRec = MetricsConfig.timerBLoadAllRec.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadAllRec.stop(MetricsConfig.timerLoadAllRec);
            }
        }
        return recommendationEntries;
    }

    @Override
    public List<KruizeLMRecommendationEntry> loadAllLMRecommendations() throws Exception {
        List<KruizeLMRecommendationEntry> recommendationEntries = null;
        String statusValue = "failure";
        Timer.Sample timerLoadAllRec = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            recommendationEntries = session.createQuery(
                    DBConstants.SQLQUERY.SELECT_FROM_LM_RECOMMENDATIONS,
                    KruizeLMRecommendationEntry.class).list();
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Not able to load recommendations due to {}", e.getMessage());
            throw new Exception("Error while loading existing recommendations from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadAllRec) {
                MetricsConfig.timerLoadAllRec = MetricsConfig.timerBLoadAllRec.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadAllRec.stop(MetricsConfig.timerLoadAllRec);
            }
        }
        return recommendationEntries;
    }

    @Override
    public List<KruizePerformanceProfileEntry> loadAllPerformanceProfiles() throws Exception {
        String statusValue = "failure";
        Timer.Sample timerLoadAllPerfProfiles = Timer.start(MetricsConfig.meterRegistry());
        List<KruizePerformanceProfileEntry> entries = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_PERFORMANCE_PROFILE, KruizePerformanceProfileEntry.class).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load Performance Profile  due to {}", e.getMessage());
            throw new Exception("Error while loading existing Performance Profile from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadAllPerfProfiles) {
                MetricsConfig.timerLoadAllPerfProfiles = MetricsConfig.timerBLoadAllPerfProfiles.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadAllPerfProfiles.stop(MetricsConfig.timerLoadAllPerfProfiles);
            }
        }
        return entries;
    }

    /**
     * Fetches all the Metric Profile records from KruizeMetricProfileEntry database table
     *
     * @return List of all KruizeMetricProfileEntry database objects
     * @throws Exception
     */
    @Override
    public List<KruizeMetricProfileEntry> loadAllMetricProfiles() throws Exception {
        String statusValue = "failure";
        Timer.Sample timerLoadAllMetricProfiles = Timer.start(MetricsConfig.meterRegistry());
        List<KruizeMetricProfileEntry> entries = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_METRIC_PROFILE, KruizeMetricProfileEntry.class).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load Metric Profile  due to {}", e.getMessage());
            throw new Exception("Error while loading existing Metric Profile from database due to : " + e.getMessage());
        }
        return entries;
    }

    /**
     * Fetches all the Metadata Profile records from KruizeLMMetadataProfileEntry database table
     *
     * @return List of all KruizeLMMetadataProfileEntry database objects
     * @throws Exception
     */
    @Override
    public List<KruizeLMMetadataProfileEntry> loadAllMetadataProfiles() throws Exception {
        String statusValue = "failure";
        Timer.Sample timerLoadAllMetadataProfiles = Timer.start(MetricsConfig.meterRegistry());

        List<KruizeLMMetadataProfileEntry> entries = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_METADATA_PROFILE, KruizeLMMetadataProfileEntry.class).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load Metadata Profile  due to {}", e.getMessage());
            throw new Exception("Error while loading existing Metadata Profile from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadAllMetadataProfiles) {
                MetricsConfig.timerLoadAllMetadataProfiles = MetricsConfig.timerBLoadAllMetadataProfiles.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadAllMetadataProfiles.stop(MetricsConfig.timerLoadAllMetadataProfiles);
            }
        }
        return entries;
    }

    @Override
    public List<KruizeLMExperimentEntry> loadLMExperimentByName(String experimentName) throws Exception {
        //todo load only experimentStatus=inprogress , playback may not require completed experiments
        List<KruizeLMExperimentEntry> entries = null;
        String statusValue = "failure";
        Timer.Sample timerLoadExpName = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_LM_EXPERIMENTS_BY_EXP_NAME, KruizeLMExperimentEntry.class)
                    .setParameter("experimentName", experimentName).list();
            // TODO: remove native sql query and transient
            //getExperimentTypeInKruizeExperimentEntry(entries);
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Not able to load experiment {} due to {}", experimentName, e.getMessage());
            throw new Exception("Error while loading existing experiment from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadExpName) {
                MetricsConfig.timerLoadExpName = MetricsConfig.timerBLoadExpName.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadExpName.stop(MetricsConfig.timerLoadExpName);
            }

        }
        return entries;
    }

    @Override
    public List<KruizeExperimentEntry> loadExperimentByName(String experimentName) throws Exception {
        //todo load only experimentStatus=inprogress , playback may not require completed experiments
        List<KruizeExperimentEntry> entries = null;
        String statusValue = "failure";
        Timer.Sample timerLoadExpName = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_EXPERIMENTS_BY_EXP_NAME, KruizeExperimentEntry.class)
                    .setParameter("experimentName", experimentName).list();
            // TODO: remove native sql query and transient
            //getExperimentTypeInKruizeExperimentEntry(entries);
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Not able to load experiment {} due to {}", experimentName, e.getMessage());
            throw new Exception("Error while loading existing experiment from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadExpName) {
                MetricsConfig.timerLoadExpName = MetricsConfig.timerBLoadExpName.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadExpName.stop(MetricsConfig.timerLoadExpName);
            }

        }
        return entries;
    }

    /**
     * @param clusterName
     * @param kubernetesAPIObject
     * @return list of experiments from the DB matching the input params
     */
    @Override
    public List<KruizeExperimentEntry> loadExperimentFromDBByInputJSON(StringBuilder clusterName, KubernetesAPIObject kubernetesAPIObject) throws Exception {
        //todo load only experimentStatus=inprogress , playback may not require completed experiments
        List<KruizeExperimentEntry> entries;
        String statusValue = "failure";
        Timer.Sample timerLoadExpName = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            // assuming there will be only one container
            ContainerAPIObject containerAPIObject = kubernetesAPIObject.getContainerAPIObjects().get(0);
            // Set parameters for KubernetesObject and Container
            Query<KruizeExperimentEntry> query = session.createNativeQuery(SELECT_FROM_EXPERIMENTS_BY_INPUT_JSON, KruizeExperimentEntry.class);
            query.setParameter(CLUSTER_NAME, clusterName.toString());
            query.setParameter(KruizeConstants.JSONKeys.NAME, kubernetesAPIObject.getName());
            query.setParameter(KruizeConstants.JSONKeys.NAMESPACE, kubernetesAPIObject.getNamespace());
            query.setParameter(KruizeConstants.JSONKeys.TYPE, kubernetesAPIObject.getType());
            query.setParameter(KruizeConstants.JSONKeys.CONTAINER_NAME, containerAPIObject.getContainer_name());
            query.setParameter(KruizeConstants.JSONKeys.CONTAINER_IMAGE_NAME, containerAPIObject.getContainer_image_name());

            entries = query.getResultList();
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Error fetching experiment data: {}", e.getMessage());
            throw new Exception("Error while fetching experiment data from database: " + e.getMessage());
        } finally {
            if (null != timerLoadExpName) {
                MetricsConfig.timerLoadExpName = MetricsConfig.timerBLoadExpName.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadExpName.stop(MetricsConfig.timerLoadExpName);
            }
        }
        return entries;
    }

    @Override
    public List<KruizeLMExperimentEntry> loadLMExperimentFromDBByInputJSON(StringBuilder clusterName, KubernetesAPIObject kubernetesAPIObject) throws Exception {
        //todo load only experimentStatus=inprogress , playback may not require completed experiments
        List<KruizeLMExperimentEntry> entries;
        String statusValue = "failure";
        Timer.Sample timerLoadExpName = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            // assuming there will be only one container
            ContainerAPIObject containerAPIObject = kubernetesAPIObject.getContainerAPIObjects().get(0);
            // Set parameters for KubernetesObject and Container
            Query<KruizeLMExperimentEntry> query = session.createNativeQuery(SELECT_FROM_LM_EXPERIMENTS_BY_INPUT_JSON, KruizeLMExperimentEntry.class);
            query.setParameter(CLUSTER_NAME, clusterName.toString());
            query.setParameter(KruizeConstants.JSONKeys.NAME, kubernetesAPIObject.getName());
            query.setParameter(KruizeConstants.JSONKeys.NAMESPACE, kubernetesAPIObject.getNamespace());
            query.setParameter(KruizeConstants.JSONKeys.TYPE, kubernetesAPIObject.getType());
            query.setParameter(KruizeConstants.JSONKeys.CONTAINER_NAME, containerAPIObject.getContainer_name());
            query.setParameter(KruizeConstants.JSONKeys.CONTAINER_IMAGE_NAME, containerAPIObject.getContainer_image_name());

            entries = query.getResultList();
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Error fetching experiment data: {}", e.getMessage());
            throw new Exception("Error while fetching experiment data from database: " + e.getMessage());
        } finally {
            if (null != timerLoadExpName) {
                MetricsConfig.timerLoadExpName = MetricsConfig.timerBLoadExpName.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadExpName.stop(MetricsConfig.timerLoadExpName);
            }
        }
        return entries;
    }


    @Override
    public List<KruizeResultsEntry> loadResultsByExperimentName(String experimentName, String cluster_name, Timestamp calculated_start_time, Timestamp interval_end_time) throws Exception {
        // TODO: load only experimentStatus=inProgress , playback may not require completed experiments
        List<KruizeResultsEntry> kruizeResultsEntries = null;
        String statusValue = "failure";
        String clusterCondtionSql = "";
        if (cluster_name != null)
            clusterCondtionSql = String.format(" and k.%s = :%s ", KruizeConstants.JSONKeys.CLUSTER_NAME, KruizeConstants.JSONKeys.CLUSTER_NAME);
        else
            clusterCondtionSql = String.format(" and k.%s is null ", KruizeConstants.JSONKeys.CLUSTER_NAME);
        Timer.Sample timerLoadResultsExpName = Timer.start(MetricsConfig.meterRegistry());
        LOGGER.debug("startTime : {} , endTime : {}", calculated_start_time, interval_end_time);
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            if (null != calculated_start_time && null != interval_end_time) {
                Query<KruizeResultsEntry> kruizeResultsEntryQuery = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_RESULTS_BY_EXP_NAME_AND_DATE_RANGE_AND_LIMIT + clusterCondtionSql, KruizeResultsEntry.class)
                        .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experimentName)
                        .setParameter(KruizeConstants.JSONKeys.CALCULATED_START_TIME, calculated_start_time)
                        .setParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME, interval_end_time);
                if (cluster_name != null)
                    kruizeResultsEntryQuery.setParameter(CLUSTER_NAME, cluster_name);
                kruizeResultsEntries = kruizeResultsEntryQuery.list();
                statusValue = "success";
            } else {
                kruizeResultsEntries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_RESULTS_BY_EXP_NAME, KruizeResultsEntry.class)
                        .setParameter("experimentName", experimentName).list();
                statusValue = "success";
            }
        } catch (Exception e) {
            LOGGER.error("Not able to load results due to: {}", e.getMessage());
            throw new Exception("Error while loading results from the database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadResultsExpName) {
                MetricsConfig.timerLoadResultsExpName = MetricsConfig.timerBLoadResultsExpName.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadResultsExpName.stop(MetricsConfig.timerLoadResultsExpName);
            }
        }
        return kruizeResultsEntries;
    }

    @Override
    public List<KruizeRecommendationEntry> loadRecommendationsByExperimentName(String experimentName) throws Exception {
        List<KruizeRecommendationEntry> recommendationEntries = null;
        String statusValue = "failure";
        Timer.Sample timerLoadRecExpName = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            recommendationEntries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME, KruizeRecommendationEntry.class)
                    .setParameter("experimentName", experimentName).list();
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Not able to load recommendations due to {}", e.getMessage());
            throw new Exception("Error while loading existing recommendations from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadRecExpName) {
                MetricsConfig.timerLoadRecExpName = MetricsConfig.timerBLoadRecExpName.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadRecExpName.stop(MetricsConfig.timerLoadRecExpName);
            }
        }
        return recommendationEntries;
    }

    @Override
    public List<KruizeLMRecommendationEntry> loadLMRecommendationsByExperimentName(String experimentName) throws Exception {
        List<KruizeLMRecommendationEntry> recommendationEntries = null;
        String statusValue = "failure";
        Timer.Sample timerLoadRecExpName = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            recommendationEntries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_LM_RECOMMENDATIONS_BY_EXP_NAME, KruizeLMRecommendationEntry.class)
                    .setParameter("experimentName", experimentName).list();
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Not able to load recommendations due to {}", e.getMessage());
            throw new Exception("Error while loading existing recommendations from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadRecExpName) {
                MetricsConfig.timerLoadRecExpName = MetricsConfig.timerBLoadRecExpName.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadRecExpName.stop(MetricsConfig.timerLoadRecExpName);
            }
        }
        return recommendationEntries;
    }

    @Override
    public KruizeRecommendationEntry loadRecommendationsByExperimentNameAndDate(String experimentName, String cluster_name, Timestamp interval_end_time) throws Exception {
        KruizeRecommendationEntry recommendationEntries = null;
        String statusValue = "failure";
        String clusterCondtionSql = "";
        if (cluster_name != null)
            clusterCondtionSql = String.format(" and k.%s = :%s ", KruizeConstants.JSONKeys.CLUSTER_NAME, KruizeConstants.JSONKeys.CLUSTER_NAME);
        else
            clusterCondtionSql = String.format(" and k.%s is null ", KruizeConstants.JSONKeys.CLUSTER_NAME);

        Timer.Sample timerLoadRecExpNameDate = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            Query<KruizeRecommendationEntry> kruizeRecommendationEntryQuery = session.createQuery(SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME_AND_END_TIME + clusterCondtionSql, KruizeRecommendationEntry.class)
                    .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experimentName)
                    .setParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME, interval_end_time);
            if (cluster_name != null)
                kruizeRecommendationEntryQuery.setParameter(CLUSTER_NAME, cluster_name);
            recommendationEntries = kruizeRecommendationEntryQuery.getSingleResult();
            statusValue = "success";
        } catch (NoResultException e) {
            LOGGER.debug("Generating new recommendation for Experiment name : %s interval_end_time: %S", experimentName, interval_end_time);
        } catch (Exception e) {
            LOGGER.error("Not able to load recommendations due to {}", e.getMessage());
            recommendationEntries = null;
            throw new Exception("Error while loading existing recommendations from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadRecExpNameDate) {
                MetricsConfig.timerLoadRecExpNameDate = MetricsConfig.timerBLoadRecExpNameDate.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadRecExpNameDate.stop(MetricsConfig.timerLoadRecExpNameDate);
            }
        }
        return recommendationEntries;
    }

    @Override
    public KruizeLMRecommendationEntry loadLMRecommendationsByExperimentNameAndDate(String experimentName, String cluster_name, Timestamp interval_end_time) throws Exception {
        KruizeLMRecommendationEntry recommendationEntries = null;
        String statusValue = "failure";
        String clusterCondtionSql = "";
        if (cluster_name != null)
            clusterCondtionSql = String.format(" and k.%s = :%s ", KruizeConstants.JSONKeys.CLUSTER_NAME, KruizeConstants.JSONKeys.CLUSTER_NAME);
        else
            clusterCondtionSql = String.format(" and k.%s is null ", KruizeConstants.JSONKeys.CLUSTER_NAME);

        Timer.Sample timerLoadRecExpNameDate = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            Query<KruizeLMRecommendationEntry> kruizeRecommendationEntryQuery = session.createQuery(SELECT_FROM_LM_RECOMMENDATIONS_BY_EXP_NAME_AND_END_TIME + clusterCondtionSql, KruizeLMRecommendationEntry.class)
                    .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experimentName)
                    .setParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME, interval_end_time);
            if (cluster_name != null)
                kruizeRecommendationEntryQuery.setParameter(CLUSTER_NAME, cluster_name);
            recommendationEntries = kruizeRecommendationEntryQuery.getSingleResult();
            statusValue = "success";
        } catch (NoResultException e) {
            LOGGER.debug("Generating new recommendation for Experiment name : {} interval_end_time: {}", experimentName, interval_end_time);
        } catch (Exception e) {
            LOGGER.error("Not able to load recommendations due to {}", e.getMessage());
            recommendationEntries = null;
            throw new Exception("Error while loading existing recommendations from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadRecExpNameDate) {
                MetricsConfig.timerLoadRecExpNameDate = MetricsConfig.timerBLoadRecExpNameDate.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadRecExpNameDate.stop(MetricsConfig.timerLoadRecExpNameDate);
            }
        }
        return recommendationEntries;
    }


    public List<KruizePerformanceProfileEntry> loadPerformanceProfileByName(String performanceProfileName) throws Exception {
        String statusValue = "failure";
        Timer.Sample timerLoadPerfProfileName = Timer.start(MetricsConfig.meterRegistry());
        List<KruizePerformanceProfileEntry> entries = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_PERFORMANCE_PROFILE_BY_NAME, KruizePerformanceProfileEntry.class)
                    .setParameter("name", performanceProfileName).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load Performance Profile {} due to {}", performanceProfileName, e.getMessage());
            throw new Exception("Error while loading existing profile from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadPerfProfileName) {
                MetricsConfig.timerLoadPerfProfileName = MetricsConfig.timerBLoadPerfProfileName.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadPerfProfileName.stop(MetricsConfig.timerLoadPerfProfileName);
            }
        }
        return entries;
    }

    /**
     * Fetches Metric Profile by name from KruizeMetricProfileEntry database table
     *
     * @param metricProfileName Metric profile name
     * @return List of KruizeMetricProfileEntry objects
     * @throws Exception
     */
    public List<KruizeMetricProfileEntry> loadMetricProfileByName(String metricProfileName) throws Exception {
        String statusValue = "failure";
        Timer.Sample timerLoadMetricProfileName = Timer.start(MetricsConfig.meterRegistry());
        List<KruizeMetricProfileEntry> entries = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_METRIC_PROFILE_BY_NAME, KruizeMetricProfileEntry.class)
                    .setParameter("name", metricProfileName).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load Metric Profile {} due to {}", metricProfileName, e.getMessage());
            throw new Exception("Error while loading existing metric profile from database due to : " + e.getMessage());
        }
        return entries;
    }

    /**
     * Fetches Metadata Profile by name from KruizeLMMetadataProfileEntry database table
     *
     * @param metadataProfileName Metadata profile name
     * @return List of KruizeLMMetadataProfileEntry objects
     * @throws Exception
     */
    public List<KruizeLMMetadataProfileEntry> loadMetadataProfileByName(String metadataProfileName) throws Exception {
        String statusValue = "failure";
        Timer.Sample timerLoadMetadataProfileName = Timer.start(MetricsConfig.meterRegistry());
        List<KruizeLMMetadataProfileEntry> entries = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_METADATA_PROFILE_BY_NAME, KruizeLMMetadataProfileEntry.class)
                    .setParameter("name", metadataProfileName).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load Metadata Profile {} due to {}", metadataProfileName, e.getMessage());
            throw new Exception("Error while loading existing metadata profile from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadMetadataProfileName) {
                MetricsConfig.timerLoadMetadataProfileName = MetricsConfig.timerBLoadMetadataProfileName.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerLoadMetadataProfileName.stop(MetricsConfig.timerLoadMetadataProfileName);
            }
        }
        return entries;
    }

    @Override
    public List<KruizeResultsEntry> getKruizeResultsEntry(String experiment_name, String cluster_name, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception {
        List<KruizeResultsEntry> kruizeResultsEntryList = new ArrayList<>();
        String clusterCondtionSql = "";
        if (cluster_name != null)
            clusterCondtionSql = String.format(" and k.%s = :%s ", KruizeConstants.JSONKeys.CLUSTER_NAME, KruizeConstants.JSONKeys.CLUSTER_NAME);
        else
            clusterCondtionSql = String.format(" and k.%s is null ", KruizeConstants.JSONKeys.CLUSTER_NAME);
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            Query<KruizeResultsEntry> kruizeResultsEntryQuery = null;
            if (interval_start_time != null && interval_end_time != null) {
                kruizeResultsEntryQuery = session.createQuery(SELECT_FROM_RESULTS_BY_EXP_NAME_AND_START_END_TIME + clusterCondtionSql, KruizeResultsEntry.class)
                        .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experiment_name)
                        .setParameter(KruizeConstants.JSONKeys.INTERVAL_START_TIME, interval_start_time)
                        .setParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME, interval_end_time);
            } else if (interval_end_time != null) {
                kruizeResultsEntryQuery = session.createQuery(SELECT_FROM_RESULTS_BY_EXP_NAME_AND_END_TIME + clusterCondtionSql, KruizeResultsEntry.class)
                        .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experiment_name)
                        .setParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME, interval_end_time);
            } else {
                kruizeResultsEntryQuery = session.createQuery(SELECT_FROM_RESULTS_BY_EXP_NAME_AND_MAX_END_TIME + clusterCondtionSql, KruizeResultsEntry.class)
                        .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experiment_name);
            }
            if (cluster_name != null)
                kruizeResultsEntryQuery.setParameter(CLUSTER_NAME, cluster_name);
            kruizeResultsEntryList = kruizeResultsEntryQuery.getResultList();
        } catch (NoResultException e) {
            LOGGER.error(DBConstants.DB_MESSAGES.DATA_NOT_FOUND_KRUIZE_RESULTS, experiment_name, interval_end_time);
            kruizeResultsEntryList = null;
        } catch (Exception e) {
            kruizeResultsEntryList = null;
            LOGGER.error("Not able to load results due to: {}", e.getMessage());
            throw new Exception("Error while loading results from the database due to : " + e.getMessage());
        }
        return kruizeResultsEntryList;
    }

    public YearMonth buildDateForNextMonth(YearMonth yearMonth) {
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue() + 1; // increment by one as we need to create the partition for the next month
        if (month > 12) {
            month = 1;
            year += 1;
        }
        yearMonth = YearMonth.of(year, month);
        return yearMonth;
    }

    /**
     * @param dataSourceName
     * @return
     */
    @Override
    public List<KruizeDSMetadataEntry> loadMetadataByName(String dataSourceName) throws Exception {
        List<KruizeDSMetadataEntry> kruizeMetadataList;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            kruizeMetadataList = session.createQuery(SELECT_FROM_METADATA_BY_DATASOURCE_NAME, KruizeDSMetadataEntry.class)
                    .setParameter("dataSourceName", dataSourceName).list();
        } catch (Exception e) {
            LOGGER.error("Unable to load metadata with dataSourceName: {} : {}", dataSourceName, e.getMessage());
            throw new Exception("Error while loading existing metadata object from database : " + e.getMessage());
        }
        return kruizeMetadataList;
    }

    /**
     * Retrieves a list of KruizeDSMetadataEntry objects based on the specified datasource name and cluster name.
     *
     * @param dataSourceName The name of the datasource.
     * @param clusterName    The name of the cluster.
     * @return A list of KruizeDSMetadataEntry objects associated with the provided datasource and cluster name.
     * @throws Exception If there is an error while loading metadata from the database.
     */
    @Override
    public List<KruizeDSMetadataEntry> loadMetadataByClusterName(String dataSourceName, String clusterName) throws Exception {
        List<KruizeDSMetadataEntry> kruizeMetadataList;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            Query<KruizeDSMetadataEntry> kruizeMetadataQuery = session.createQuery(SELECT_FROM_METADATA_BY_DATASOURCE_NAME_AND_CLUSTER_NAME, KruizeDSMetadataEntry.class)
                    .setParameter("datasource_name", dataSourceName)
                    .setParameter("cluster_name", clusterName);

            kruizeMetadataList = kruizeMetadataQuery.list();
        } catch (Exception e) {
            LOGGER.error("Unable to load metadata with dataSourceName: {} and clusterName : {} : {}", dataSourceName, clusterName, e.getMessage());
            throw new Exception("Error while loading existing metadata object from database : " + e.getMessage());
        }
        return kruizeMetadataList;
    }

    /**
     * Retrieves a list of KruizeDSMetadataEntry objects based on the specified
     * datasource name, cluster name and namespace.
     *
     * @param dataSourceName The name of the datasource.
     * @param clusterName    The name of the cluster.
     * @param namespace      namespace
     * @return A list of KruizeDSMetadataEntry objects associated with the provided datasource, cluster name and namespaces.
     * @throws Exception If there is an error while loading metadata from the database.
     */
    public List<KruizeDSMetadataEntry> loadMetadataByNamespace(String dataSourceName, String clusterName, String namespace) throws Exception {
        List<KruizeDSMetadataEntry> kruizeMetadataList;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            Query<KruizeDSMetadataEntry> kruizeMetadataQuery = session.createQuery(SELECT_FROM_METADATA_BY_DATASOURCE_NAME_CLUSTER_NAME_AND_NAMESPACE, KruizeDSMetadataEntry.class)
                    .setParameter("datasource_name", dataSourceName)
                    .setParameter("cluster_name", clusterName)
                    .setParameter("namespace", namespace);

            kruizeMetadataList = kruizeMetadataQuery.list();
        } catch (Exception e) {
            LOGGER.error("Unable to load metadata with dataSourceName: {}, clusterName : {} and namespace : {} : {}", dataSourceName, clusterName, namespace, e.getMessage());
            throw new Exception("Error while loading existing metadata object from database : " + e.getMessage());
        }
        return kruizeMetadataList;
    }

    /**
     * @param name
     * @return single element list of datasource after fetching from the DB
     * @throws Exception
     */
    @Override
    public List<KruizeDataSourceEntry> loadDataSourceByName(String name) throws Exception {
        List<KruizeDataSourceEntry> kruizeDataSourceList;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            kruizeDataSourceList = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_DATASOURCE_BY_NAME, KruizeDataSourceEntry.class)
                    .setParameter("name", name).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load datasource: {} : {}", name, e.getMessage());
            throw new Exception("Error while loading existing datasource from database : " + e.getMessage());
        }
        return kruizeDataSourceList;
    }

    /**
     * @return list of datasources after fetching from the DB
     */
    @Override
    public List<KruizeDataSourceEntry> loadAllDataSources() throws Exception {
        List<KruizeDataSourceEntry> entries;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(SELECT_FROM_DATASOURCE, KruizeDataSourceEntry.class).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load datasource: {}", e.getMessage());
            throw new Exception("Error while loading existing datasources from database: " + e.getMessage());
        }
        return entries;
    }

   /* private void getExperimentTypeInKruizeExperimentEntry(List<KruizeExperimentEntry> entries) throws Exception {
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            for (KruizeExperimentEntry entry : entries) {
                if (isTargetCluserLocal(entry.getTarget_cluster())) {
                    if (null == entry.getE || entry.getExperimentType().isEmpty()) {
                        String sql = DBConstants.SQLQUERY.SELECT_EXPERIMENT_EXP_TYPE;
                        Query query = session.createNativeQuery(sql);
                        query.setParameter("experiment_id", entry.getExperiment_id());
                        List<String> experimentType = query.getResultList();
                        if (null != experimentType && !experimentType.isEmpty()) {
                            entry.setExperimentType(experimentType.get(0));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Not able to get experiment type from experiment entry due to {}", e.getMessage());
            throw new Exception("Error while loading experiment type from database due to : " + e.getMessage());
        }
    }*/

    /*private void updateExperimentTypeInKruizeExperimentEntry(KruizeLMExperimentEntry kruizeLMExperimentEntry) throws Exception {
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            if (isTargetCluserLocal(kruizeLMExperimentEntry.getTarget_cluster())) {
                Transaction tx = session.beginTransaction();
                String sql = DBConstants.SQLQUERY.UPDATE_EXPERIMENT_EXP_TYPE;
                Query query = session.createNativeQuery(sql);
                query.setParameter("experiment_type", kruizeLMExperimentEntry.getExperimentType());
                query.setParameter("experiment_name", kruizeLMExperimentEntry.getExperiment_name());
                query.executeUpdate();
                tx.commit();
            }
        } catch (Exception e) {
            LOGGER.error("Not able to update experiment type in experiment entry due to {}", e.getMessage());
            throw new Exception("Error while updating experiment type to database due to : " + e.getMessage());
        }
    }

    private void updateExperimentTypeInKruizeExperimentEntry(KruizeExperimentEntry kruizeExperimentEntry) throws Exception {
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            if (isTargetCluserLocal(kruizeExperimentEntry.getTarget_cluster())) {
                Transaction tx = session.beginTransaction();
                String sql = DBConstants.SQLQUERY.UPDATE_EXPERIMENT_EXP_TYPE;
                Query query = session.createNativeQuery(sql);
                query.setParameter("experiment_type", kruizeExperimentEntry.getExperimentType());
                query.setParameter("experiment_name", kruizeExperimentEntry.getExperiment_name());
                query.executeUpdate();
                tx.commit();
            }
        } catch (Exception e) {
            LOGGER.error("Not able to update experiment type in experiment entry due to {}", e.getMessage());
            throw new Exception("Error while updating experiment type to database due to : " + e.getMessage());
        }
    }*/


    private void getExperimentTypeInSingleKruizeRecommendationsEntry(KruizeRecommendationEntry recomEntry) throws Exception {
        List<KruizeExperimentEntry> expEntries = loadExperimentByName(recomEntry.getExperiment_name());

    }


    private boolean isTargetCluserLocal(String targetCluster) {
        if (AnalyzerConstants.LOCAL.equalsIgnoreCase(targetCluster)) {
            return true;
        }
        return false;
    }
}
