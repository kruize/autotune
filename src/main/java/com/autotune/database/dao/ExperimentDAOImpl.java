package com.autotune.database.dao;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.helper.DBConstants;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.database.table.KruizeExperimentEntry;
import com.autotune.database.table.KruizePerformanceProfileEntry;
import com.autotune.database.table.KruizeRecommendationEntry;
import com.autotune.database.table.KruizeResultsEntry;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.autotune.database.helper.DBConstants.SQLQUERY.*;

public class ExperimentDAOImpl implements ExperimentDAO {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentDAOImpl.class);

    @Override
    public ValidationOutputData addExperimentToDB(KruizeExperimentEntry kruizeExperimentEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        Timer.Sample timerAddExpDB = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                session.persist(kruizeExperimentEntry);
                tx.commit();
                validationOutputData.setSuccess(true);
            } catch (HibernateException e) {
                LOGGER.error("Not able to save experiment due to {}", e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save experiment due to {}", e.getMessage());
            validationOutputData.setMessage(e.getMessage());
        } finally {
            if (null != timerAddExpDB) timerAddExpDB.stop(MetricsConfig.timerAddExpDB);
        }
        return validationOutputData;
    }

    @Override
    public ValidationOutputData addResultsToDB(KruizeResultsEntry resultsEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        Timer.Sample timerAddResultsDB = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                session.persist(resultsEntry);
                tx.commit();
                validationOutputData.setSuccess(true);
            } catch (PersistenceException ex) {
                if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage(
                            String.format("A record with the name %s already exists within the timestamp range starting from %s and ending on %s.", resultsEntry.getExperiment_name(), resultsEntry.getInterval_start_time(), resultsEntry.getInterval_end_time())
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
            if (null != timerAddResultsDB) timerAddResultsDB.stop(MetricsConfig.timerAddResultsDB);
        }
        return validationOutputData;
    }

    @Override
    public List<KruizeResultsEntry> addToDBAndFetchFailedResults(List<KruizeResultsEntry> kruizeResultsEntries) {
        List<KruizeResultsEntry> failedResultsEntries = new ArrayList<>();
        Transaction tx = null;
        Timer.Sample timerAddResultsDB = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            for (KruizeResultsEntry entry : kruizeResultsEntries) {
                try {
                    session.merge(entry);
                } catch (PersistenceException e) {
                    entry.setErrorReasons(List.of(String.format("A record with the name %s already exists within the timestamp range starting from %s and ending on %s.", entry.getExperiment_name(), new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT).format(entry.getInterval_start_time()),
                            new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT).format(entry.getInterval_end_time()))));
                    failedResultsEntries.add(entry);
                } catch (Exception e) {
                    entry.setErrorReasons(List.of(e.getMessage()));
                    failedResultsEntries.add(entry);
                }
            }
            tx.commit();
            if (!failedResultsEntries.isEmpty()) {
                //  find elements in kruizeResultsEntries but not in failedResultsEntries
                List<KruizeResultsEntry> elementsInSuccessOnly = kruizeResultsEntries.stream()
                        .filter(entry -> !failedResultsEntries.contains(entry))
                        .collect(Collectors.toList());
                tx = session.beginTransaction();
                for (KruizeResultsEntry entry : elementsInSuccessOnly) {
                    session.merge(entry);
                }
                tx.commit();
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save experiment due to {}", e.getMessage());
            failedResultsEntries.addAll(kruizeResultsEntries);
            failedResultsEntries.forEach((entry) -> {
                entry.setErrorReasons(List.of(e.getMessage()));
            });
        } finally {
            if (null != timerAddResultsDB) timerAddResultsDB.stop(MetricsConfig.timerAddResultsDB);
        }
        return failedResultsEntries;
    }

    @Override
    public ValidationOutputData addRecommendationToDB(KruizeRecommendationEntry recommendationEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        Timer.Sample timerAddRecDB = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                KruizeRecommendationEntry existingRecommendationEntry = loadRecommendationsByExperimentNameAndDate(recommendationEntry.getExperiment_name(), recommendationEntry.getInterval_end_time());
                if (null == existingRecommendationEntry) {
                    tx = session.beginTransaction();
                    session.persist(recommendationEntry);
                    tx.commit();
                    validationOutputData.setSuccess(true);
                } else {
                    tx = session.beginTransaction();
                    existingRecommendationEntry.setExtended_data(recommendationEntry.getExtended_data());
                    session.merge(existingRecommendationEntry);
                    tx.commit();
                    validationOutputData.setSuccess(true);
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
            if (null != timerAddRecDB) timerAddRecDB.stop(MetricsConfig.timerAddRecDB);
        }
        return validationOutputData;
    }

    @Override
    public ValidationOutputData addPerformanceProfileToDB(KruizePerformanceProfileEntry kruizePerformanceProfileEntry) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                session.persist(kruizePerformanceProfileEntry);
                tx.commit();
                validationOutputData.setSuccess(true);
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
        Timer.Sample timerLoadAllExp = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_EXPERIMENTS, KruizeExperimentEntry.class).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load experiment due to {}", e.getMessage());
            throw new Exception("Error while loading exsisting experiments from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadAllExp) timerLoadAllExp.stop(MetricsConfig.timerLoadAllExp);
        }
        return entries;
    }

    @Override
    public List<KruizeResultsEntry> loadAllResults() throws Exception {
        // TODO: load only experimentStatus=inProgress , playback may not require completed experiments
        List<KruizeResultsEntry> kruizeResultsEntries = null;
        Timer.Sample timerLoadAllResults = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            kruizeResultsEntries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_RESULTS, KruizeResultsEntry.class).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load results due to: {}", e.getMessage());
            throw new Exception("Error while loading results from the database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadAllResults) timerLoadAllResults.stop(MetricsConfig.timerLoadAllResults);
        }
        return kruizeResultsEntries;
    }

    @Override
    public List<KruizeRecommendationEntry> loadAllRecommendations() throws Exception {
        List<KruizeRecommendationEntry> recommendationEntries = null;
        Timer.Sample timerLoadAllRec = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            recommendationEntries = session.createQuery(
                    DBConstants.SQLQUERY.SELECT_FROM_RECOMMENDATIONS,
                    KruizeRecommendationEntry.class).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load recommendations due to {}", e.getMessage());
            throw new Exception("Error while loading existing recommendations from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadAllRec) timerLoadAllRec.stop(MetricsConfig.timerLoadAllRec);
        }
        return recommendationEntries;
    }

    @Override
    public List<KruizePerformanceProfileEntry> loadAllPerformanceProfiles() throws Exception {
        List<KruizePerformanceProfileEntry> entries = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_PERFORMANCE_PROFILE, KruizePerformanceProfileEntry.class).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load Performance Profile  due to {}", e.getMessage());
            throw new Exception("Error while loading existing Performance Profile from database due to : " + e.getMessage());
        }
        return entries;
    }

    @Override
    public List<KruizeExperimentEntry> loadExperimentByName(String experimentName) throws Exception {
        //todo load only experimentStatus=inprogress , playback may not require completed experiments
        List<KruizeExperimentEntry> entries = null;
        Timer.Sample timerLoadExpName = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_EXPERIMENTS_BY_EXP_NAME, KruizeExperimentEntry.class)
                    .setParameter("experimentName", experimentName).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load experiment {} due to {}", experimentName, e.getMessage());
            throw new Exception("Error while loading existing experiment from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadExpName) timerLoadExpName.stop(MetricsConfig.timerLoadExpName);
        }
        return entries;
    }

    @Override
    public List<KruizeResultsEntry> loadResultsByExperimentName(String experimentName, Timestamp interval_end_time, Integer limitRows) throws Exception {
        // TODO: load only experimentStatus=inProgress , playback may not require completed experiments
        List<KruizeResultsEntry> kruizeResultsEntries = null;
        Timer.Sample timerLoadResultsExpName = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            if (null != limitRows && null != interval_end_time) {
                kruizeResultsEntries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_RESULTS_BY_EXP_NAME_AND_DATE_RANGE, KruizeResultsEntry.class)
                        .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experimentName)
                        .setParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME, interval_end_time)
                        .setMaxResults(limitRows)
                        .list();
            } else {
                kruizeResultsEntries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_RESULTS_BY_EXP_NAME, KruizeResultsEntry.class)
                        .setParameter("experimentName", experimentName).list();
            }
        } catch (Exception e) {
            LOGGER.error("Not able to load results due to: {}", e.getMessage());
            throw new Exception("Error while loading results from the database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadResultsExpName) timerLoadResultsExpName.stop(MetricsConfig.timerLoadResultsExpName);
        }
        return kruizeResultsEntries;
    }

    @Override
    public List<KruizeRecommendationEntry> loadRecommendationsByExperimentName(String experimentName) throws Exception {
        List<KruizeRecommendationEntry> recommendationEntries = null;
        Timer.Sample timerLoadRecExpName = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            recommendationEntries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME, KruizeRecommendationEntry.class)
                    .setParameter("experimentName", experimentName).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load recommendations due to {}", e.getMessage());
            throw new Exception("Error while loading existing recommendations from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadRecExpName) timerLoadRecExpName.stop(MetricsConfig.timerLoadRecExpName);
        }
        return recommendationEntries;
    }

    @Override
    public KruizeRecommendationEntry loadRecommendationsByExperimentNameAndDate(String experimentName, Timestamp interval_end_time) throws Exception {
        KruizeRecommendationEntry recommendationEntries = null;
        Timer.Sample timerLoadRecExpName = Timer.start(MetricsConfig.meterRegistry());
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            recommendationEntries = session.createQuery(SELECT_FROM_RECOMMENDATIONS_BY_EXP_NAME_AND_END_TIME, KruizeRecommendationEntry.class)
                    .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experimentName)
                    .setParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME, interval_end_time)
                    .getSingleResult();
        } catch (NoResultException e) {
            LOGGER.debug("Generating mew recommendation for Experiment name : %s interval_end_time: %S", experimentName, interval_end_time);
        } catch (Exception e) {
            LOGGER.error("Not able to load recommendations due to {}", e.getMessage());
            recommendationEntries = null;
            throw new Exception("Error while loading existing recommendations from database due to : " + e.getMessage());
        } finally {
            if (null != timerLoadRecExpName) timerLoadRecExpName.stop(MetricsConfig.timerLoadRecExpName);
        }
        return recommendationEntries;
    }

<<<<<<< HEAD
    @Override
    public List<KruizeResultsEntry> getKruizeResultsEntry(String experiment_name, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception {
        List<KruizeResultsEntry> kruizeResultsEntryList = new ArrayList<>();
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {

            if (interval_start_time != null && interval_end_time != null) {
                kruizeResultsEntryList = session.createQuery(SELECT_FROM_RESULTS_BY_EXP_NAME_AND_START_END_TIME, KruizeResultsEntry.class)
                        .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experiment_name)
                        .setParameter(KruizeConstants.JSONKeys.INTERVAL_START_TIME, interval_start_time)
                        .setParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME, interval_end_time)
                        .getResultList();
            } else if (interval_end_time != null) {
                kruizeResultsEntryList = session.createQuery(SELECT_FROM_RESULTS_BY_EXP_NAME_AND_END_TIME, KruizeResultsEntry.class)
                        .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experiment_name)
                        .setParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME, interval_end_time)
                        .getResultList();
            } else {
                kruizeResultsEntryList = session.createQuery(SELECT_FROM_RESULTS_BY_EXP_NAME_AND_MAX_END_TIME, KruizeResultsEntry.class)
                        .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experiment_name)
                        .getResultList();
            }
        } catch (NoResultException e) {
            LOGGER.error("Data not found in kruizeResultsEntry for exp_name:{} interval_end_time:{} ", experiment_name, interval_end_time);
            kruizeResultsEntryList = null;
        } catch (Exception e) {
            kruizeResultsEntryList = null;
            LOGGER.error("Not able to load results due to: {}", e.getMessage());
            throw new Exception("Error while loading results from the database due to : " + e.getMessage());
        }
        return kruizeResultsEntryList;
    }
=======

    public List<KruizePerformanceProfileEntry> loadPerformanceProfileByName(String performanceProfileName) throws Exception {
        List<KruizePerformanceProfileEntry> entries = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_PERFORMANCE_PROFILE_BY_NAME, KruizePerformanceProfileEntry.class)
                    .setParameter("name", performanceProfileName).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load Performance Profile {} due to {}", performanceProfileName, e.getMessage());
            throw new Exception("Error while loading existing profile from database due to : " + e.getMessage());
        }
        return entries;
    }


    @Override
    public List<KruizeResultsEntry> getKruizeResultsEntry(String experiment_name, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception {
        List<KruizeResultsEntry> kruizeResultsEntryList = new ArrayList<>();
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {

            if (interval_start_time != null && interval_end_time != null) {
                kruizeResultsEntryList = session.createQuery(SELECT_FROM_RESULTS_BY_EXP_NAME_AND_START_END_TIME, KruizeResultsEntry.class)
                        .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experiment_name)
                        .setParameter(KruizeConstants.JSONKeys.INTERVAL_START_TIME, interval_start_time)
                        .setParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME, interval_end_time)
                        .getResultList();
            } else if (interval_end_time != null) {
                kruizeResultsEntryList = session.createQuery(SELECT_FROM_RESULTS_BY_EXP_NAME_AND_END_TIME, KruizeResultsEntry.class)
                        .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experiment_name)
                        .setParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME, interval_end_time)
                        .getResultList();
            } else {
                kruizeResultsEntryList = session.createQuery(SELECT_FROM_RESULTS_BY_EXP_NAME_AND_MAX_END_TIME, KruizeResultsEntry.class)
                        .setParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME, experiment_name)
                        .getResultList();
            }


        } catch (NoResultException e) {
            LOGGER.error("Data not found in kruizeResultsEntry for exp_name:{} interval_end_time:{} ", experiment_name, interval_end_time);
            kruizeResultsEntryList = null;
        } catch (Exception e) {
            kruizeResultsEntryList = null;
            LOGGER.error("Not able to load results due to: {}", e.getMessage());
            throw new Exception("Error while loading results from the database due to : " + e.getMessage());
        }
        return kruizeResultsEntryList;

    }
<<<<<<< HEAD
>>>>>>> d35a6510 (Taking care of duplicate entry to recommendation table)
=======

    public List<KruizePerformanceProfileEntry> loadPerformanceProfileByName(String performanceProfileName) throws Exception {
        List<KruizePerformanceProfileEntry> entries = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            entries = session.createQuery(DBConstants.SQLQUERY.SELECT_FROM_PERFORMANCE_PROFILE_BY_NAME, KruizePerformanceProfileEntry.class)
                    .setParameter("name", performanceProfileName).list();
        } catch (Exception e) {
            LOGGER.error("Not able to load Performance Profile {} due to {}", performanceProfileName, e.getMessage());
            throw new Exception("Error while loading existing profile from database due to : " + e.getMessage());
        }
        return entries;
    }
>>>>>>> f82f7db7 (update service methods for performance profiles)
}
