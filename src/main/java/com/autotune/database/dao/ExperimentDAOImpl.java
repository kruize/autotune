package com.autotune.database.dao;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.helper.DBConstants;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.database.table.*;
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
    public ValidationOutputData addExperimentToDB(KruizeExperimentEntry kruizeExperimentEntry) {
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
            } else if (partitionType.equalsIgnoreCase(DBConstants.PARTITION_TYPES.BY_DAY)) {
                String daterange = String.format(DB_PARTITION_DATERANGE, tableName, year, month, String.format("%02d", 1), tableName,
                        year, month, String.format("%02d", 1), year, month, String.format("%02d", 1));
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

    /**
     * @return list of datasources after fetching from the DB
     */
    @Override
    public List<KruizeDataSource> loadAllDataSources() throws Exception {
        List<KruizeDataSource> entries;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(SELECT_FROM_DATASOURCE, KruizeDataSource.class).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load datasource: {}", e.getMessage());
            throw new Exception("Error while loading existing datasources from database: " + e.getMessage());
        }
        return entries;
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
                            createPartitions(entry);
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
     * @param kruizeDataSource 
     * @return validationOutputData contains the status of the DB insert operation
     */
    @Override
    public ValidationOutputData addDataSourceToDB(KruizeDataSource kruizeDataSource) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);        
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                session.persist(kruizeDataSource);
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

    @Override
    public List<KruizeExperimentEntry> loadExperimentByName(String experimentName) throws Exception {
        //todo load only experimentStatus=inprogress , playback may not require completed experiments
        List<KruizeExperimentEntry> entries = null;
        String statusValue = "failure";
        Timer.Sample timerLoadExpName = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_EXPERIMENTS_BY_EXP_NAME, KruizeExperimentEntry.class)
                    .setParameter("experimentName", experimentName).list();
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
     * @param name
     * @return single element list of datasource after fetching from the DB
     * @throws Exception
     */
    @Override
    public List<KruizeDataSource> loadDataSourceByName(String name) throws Exception {
        List<KruizeDataSource> kruizeDataSourceList;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            kruizeDataSourceList = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_DATASOURCE_BY_NAME, KruizeDataSource.class)
                    .setParameter("name", name).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load datasource: {} : {}", name, e.getMessage());
            throw new Exception("Error while loading existing datasource from database : " + e.getMessage());
        }
        return kruizeDataSourceList;
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
}
