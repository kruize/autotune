/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.data;

import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.result.Containers;
import com.autotune.common.data.result.DeploymentResultData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.StartEndTimeStampResults;
import com.autotune.common.k8sObjects.ContainerObject;
import com.autotune.common.k8sObjects.DeploymentObject;
import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.dbactivites.init.KruizeHibernateUtil;
import com.autotune.dbactivites.model.ExperimentDetail;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.Utils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

public class ExperimentInterfaceImpl implements ExperimentInterface {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentInterfaceImpl.class);

    @Override
    public boolean addExperimentToLocalStorage(Map<String, KruizeObject> mainKruizeExperimentMap, List<KruizeObject> kruizeExperimentList) {
        kruizeExperimentList.forEach(
                (kruizeObject) -> {
                    kruizeObject.setStatus(AnalyzerConstants.ExperimentStatus.QUEUED);
                    kruizeObject.setExperimentId(Utils.generateID(toString()));
                    mainKruizeExperimentMap.put(
                            kruizeObject.getExperimentName(),
                            kruizeObject
                    );
                    LOGGER.debug("Added Experiment name : {} into main map.", kruizeObject.getExperimentName());
                }
        );
        return true;
    }

    /**
     * @param kruizeObject
     * @return boolean
     * <p>
     * This is a Java method that adds an experiment to a database. It takes a KruizeObject parameter, which is converted to an ExperimentDetail object using a converter method from a class named KruizeObjectConverters. The ExperimentDetail object is then persisted to the database using Hibernate.
     * The Hibernate session is opened using a try-with-resources statement that automatically closes the session after the transaction completes or an exception is thrown.
     */
    @Override
    public boolean addExperimentToDB(KruizeObject kruizeObject) {
        boolean success = false;
        updateExperimentStatus(kruizeObject, AnalyzerConstants.ExperimentStatus.IN_PROGRESS);
        ExperimentDetail experimentDetail = Utils.Converters.KruizeObjectConverters.convertKruizeObjectToExperimentDBObj(kruizeObject);

        Transaction tx = null;
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            try {
                tx = session.beginTransaction();
                session.persist(experimentDetail);
                tx.commit();
                success = true;
            } catch (HibernateException e) {
                LOGGER.error("Not able to save experiment due to {}", e.getMessage());
                updateExperimentStatus(kruizeObject, AnalyzerConstants.ExperimentStatus.FAILED);
                if (tx != null) tx.rollback();
                e.printStackTrace();
                //todo save error to API_ERROR_LOG
            }
        }

        return success;
    }

    @Override
    public boolean updateExperimentStatus(KruizeObject kruizeObject, AnalyzerConstants.ExperimentStatus status) {
        kruizeObject.setStatus(status);
        return true;
    }

    @Override
    public boolean addResultsToLocalStorage(Map<String, KruizeObject> mainKruizeExperimentMap, List<ExperimentResultData> experimentResultDataList) {
        experimentResultDataList.forEach(
                (resultData) -> {
                    resultData.setStatus(AnalyzerConstants.ExperimentStatus.QUEUED);
                    KruizeObject ko = mainKruizeExperimentMap.get(resultData.getExperiment_name());
                    Set<ExperimentResultData> results = null;
                    if (ko.getResultData() == null)
                        results = new HashSet<>();
                    else
                        results = ko.getResultData();
                    results.add(resultData);
                    ko.setResultData(results);
                    HashMap<String, DeploymentObject> deploymentsMap = ko.getDeployments();
                    if (null == deploymentsMap) {
                        deploymentsMap = new HashMap<>();
                    }
                    List<DeploymentResultData> resultDeploymentList = resultData.getDeployments();
                    for (DeploymentResultData deploymentResultData : resultDeploymentList) {
                        String dName = deploymentResultData.getDeployment_name();
                        DeploymentObject deploymentObject;
                        HashMap<String, ContainerObject> containersMap;
                        if (null == deploymentsMap.get(dName)) {
                            deploymentObject = new DeploymentObject(dName);
                            containersMap = new HashMap<>();
                        } else {
                            deploymentObject = deploymentsMap.get(dName);
                            containersMap = deploymentObject.getContainers();
                        }
                        List<Containers> resultContainerList = deploymentResultData.getContainers();
                        for (Containers containers : resultContainerList) {
                            String cName = containers.getContainer_name();
                            String imgName = containers.getImage_name();
                            ContainerObject containerObject;
                            if (null == containersMap.get(cName)) {
                                containerObject = new ContainerObject(cName, imgName);
                            } else {
                                containerObject = containersMap.get(cName);
                            }
                            HashMap<AnalyzerConstants.AggregatorType, MetricAggregationInfoResults> aggregatorHashMap = new HashMap<>();
                            for (AnalyzerConstants.MetricName aggregationInfoName : containers.getContainer_metrics().keySet()) {
                                MetricAggregationInfoResults aggregatorResult = containers.getContainer_metrics().get(aggregationInfoName).get("results").getAggregationInfoResult();
                                aggregatorHashMap.put(AnalyzerConstants.AggregatorType.valueOf(aggregationInfoName.toString()), aggregatorResult);
                            }
                            HashMap<Timestamp, StartEndTimeStampResults> resultsAggregatorStartEndTimeStampMap = containerObject.getResults();

                            if (null == resultsAggregatorStartEndTimeStampMap) {
                                resultsAggregatorStartEndTimeStampMap = new HashMap<>();
                            }
                            StartEndTimeStampResults startEndTimeStampResults = new StartEndTimeStampResults(resultData.getStarttimestamp(), resultData.getEndtimestamp());
                            startEndTimeStampResults.setMetrics(aggregatorHashMap);
                            resultsAggregatorStartEndTimeStampMap.put(resultData.getEndtimestamp(), startEndTimeStampResults);

                            containerObject.setResults(resultsAggregatorStartEndTimeStampMap);
                            containersMap.put(cName, containerObject);
                        }
                        deploymentObject.setContainers(containersMap);
                        deploymentsMap.put(dName, deploymentObject);
                    }
                    ko.setDeployments(deploymentsMap);
                    LOGGER.debug("Added Results for Experiment name : {} with TimeStamp : {} into main map.", ko.getExperimentName(), resultData.getEndtimestamp());
                }
        );
        // TODO   Insert into database
        return true;
    }

    @Override
    public boolean addResultsToDB(KruizeObject kruizeObject, ExperimentResultData resultData) {
        // TODO   Insert into database
        resultData.setStatus(AnalyzerConstants.ExperimentStatus.IN_PROGRESS);
        return false;
    }


    @Override
    public boolean loadAllExperiments(Map<String, KruizeObject> mainKruizeExperimentMap) {
        //TOdo load all experiments from DB
        return false;
    }

}
