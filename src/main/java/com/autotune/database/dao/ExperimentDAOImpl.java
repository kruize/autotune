package com.autotune.database.dao;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.database.helper.DBHelpers;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.database.table.KruizeExperimentEntry;
import com.autotune.database.table.KruizeResultsEntry;
import com.autotune.utils.Utils;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ExperimentDAOImpl implements ExperimentDAO {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentDAOImpl.class);

    @Override
    public ValidationOutputData addExperimentToDB(KruizeObject kruizeObject) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        try {
            updateExperimentStatus(kruizeObject, AnalyzerConstants.ExperimentStatus.IN_PROGRESS);
            KruizeExperimentEntry kruizeExperimentEntry = DBHelpers.Converters.KruizeObjectConverters.convertKruizeObjectToExperimentDBObj(kruizeObject);
            String experiment_id = Utils.generateID(kruizeObject);
            kruizeExperimentEntry.setExperiment_id(experiment_id);
            Transaction tx = null;
            try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
                try {
                    tx = session.beginTransaction();
                    session.persist(kruizeExperimentEntry);
                    tx.commit();
                    validationOutputData.setSuccess(true);
                } catch (HibernateException e) {
                    LOGGER.error("Not able to save experiment due to {}", e.getMessage());
                    updateExperimentStatus(kruizeObject, AnalyzerConstants.ExperimentStatus.FAILED);
                    if (tx != null) tx.rollback();
                    e.printStackTrace();
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage(e.getMessage());
                    //todo save error to API_ERROR_LOG
                }
            } catch (Exception e) {
                LOGGER.error("Not able to save experiment due to {}", e.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save experiment due to {}", e.getMessage());
        }

        return validationOutputData;
    }


    @Override
    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status) {
        kruizeObject.setStatus(status);
        // TODO   update into database
        return true;
    }

    @Override
    public ValidationOutputData addResultsToDB(ExperimentResultData resultData) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        KruizeResultsEntry kruizeResultsEntry = DBHelpers.Converters.KruizeObjectConverters.convertExperimentResultToExperimentResultsTable(resultData);
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                session.persist(kruizeResultsEntry);
                tx.commit();
                validationOutputData.setSuccess(true);
                resultData.setStatus(AnalyzerConstants.ExperimentStatus.IN_PROGRESS);
            } catch (HibernateException e) {
                LOGGER.error("Not able to save experiment due to {}", e.getMessage());
                resultData.setStatus(AnalyzerConstants.ExperimentStatus.FAILED);
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to save experiment due to {}", e.getMessage());
        }
        return validationOutputData;
    }

    @Override
    public ValidationOutputData deleteKruizeExperimentEntryByName(String experimentName) {
        ValidationOutputData validationOutputData = new ValidationOutputData(false, null, null);
        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                Query query = session.createQuery("DELETE FROM KruizeExperimentEntry k WHERE k.experiment_name = :experimentName", null);
                query.setParameter("experimentName", experimentName);
                int deletedCount = query.executeUpdate();
                if (deletedCount == 0) {
                    validationOutputData.setSuccess(false);
                    validationOutputData.setMessage("KruizeExperimentEntry not found with experiment name: " + experimentName);
                }else{
                    Query KruizeResultsEntryquery = session.createQuery("DELETE FROM KruizeResultsEntry k WHERE k.experiment_name = :experimentName", null);
                    KruizeResultsEntryquery.setParameter("experimentName", experimentName);
                    KruizeResultsEntryquery.executeUpdate();
                    validationOutputData.setSuccess(true);
                }
                tx.commit();
            } catch (HibernateException e) {
                LOGGER.error("Not able to delete experiment due to {}", e.getMessage());
                if (tx != null) tx.rollback();
                e.printStackTrace();
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(e.getMessage());
                //todo save error to API_ERROR_LOG
            }
        } catch (Exception e) {
            LOGGER.error("Not able to delete experiment due to {}", e.getMessage());
        }
        return validationOutputData;
    }



    @Override
    public boolean loadAllExperiments(Map<String, KruizeObject> mainKruizeExperimentMap) {
        //TOdo load all experiments from DB
        return false;
    }
}
