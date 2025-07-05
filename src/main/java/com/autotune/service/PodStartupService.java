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

package com.autotune.service;

import com.autotune.analyzer.serviceObjects.BulkJobStatus;
import com.autotune.analyzer.workerimpl.BulkJobManager;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.database.table.KruizePodStatus;
import com.autotune.database.table.lm.KruizeBulkJobEntry;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.List;

import static com.autotune.analyzer.services.BulkService.executorService;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.IN_PROGRESS;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.started;

public class PodStartupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PodStartupService.class);

    private final KubernetesClient k8sClient;
    private final String currentPodName;
    private final String namespace;

    public PodStartupService(String podName, String namespace) {
        this.k8sClient = new KubernetesClientBuilder().build();
        this.currentPodName = podName;
        this.namespace = namespace;
    }

    public void handleStartup() {
        registerOrUpdateCurrentPod();
        validateAndResumeStalePods();
    }

    private void registerOrUpdateCurrentPod() {
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {                 // TOdo move this DB fucntion to DAO and DAOIMPL
            Transaction tx = session.beginTransaction();

            KruizePodStatus pod = session.get(KruizePodStatus.class, currentPodName);
            if (pod == null) {
                pod = new KruizePodStatus();
                pod.setPodName(currentPodName);
            }
            pod.setStatus(started);
            pod.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            //pod.setLastChecked(new Timestamp(System.currentTimeMillis()));
            session.saveOrUpdate(pod);
            tx.commit();
        } catch (Exception e) {
            LOGGER.error("registerOrUpdateCurrentPod failed due to {}.", e.getMessage());
        }
    }

    private void validateAndResumeStalePods() {
        try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
            // TOdo move this DB fucntion to DAO and DAOIMPL

            List<KruizePodStatus> stalePods = session.createQuery(
                            "FROM KruizePodStatus WHERE status = 'started' AND podName != :currentPod ",
                            KruizePodStatus.class)
                    .setParameter("currentPod", currentPodName)
                    .list();                    //todo use constant

            for (KruizePodStatus pod : stalePods) {
                Pod k8sPod = k8sClient.pods().inNamespace(namespace).withName(pod.getPodName()).get();
                boolean notRunning = (k8sPod == null || !"Running".equalsIgnoreCase(k8sPod.getStatus().getPhase()));
                if (notRunning) {
                    Transaction tx = session.beginTransaction();
                    try {
                        // Lock the row with SELECT FOR UPDATE
                        KruizePodStatus entity = session.get(KruizePodStatus.class, pod.getPodName(), LockMode.PESSIMISTIC_WRITE);
                        List<KruizeBulkJobEntry> jobs = session.createQuery(
                                        "FROM KruizeBulkJobEntry WHERE podName = :podName and status='IN_PROGRESS'", KruizeBulkJobEntry.class)
                                .setParameter("podName", pod.getPodName())
                                .list();
                        for (KruizeBulkJobEntry job : jobs) {
                            if (IN_PROGRESS.equalsIgnoreCase(job.getStatus())) {
                                job.setPodName(this.currentPodName);
                                BulkJobStatus jobStatus = job.getBulkJobStatus();
                                jobStatus.getExperiments().putAll(jobStatus.getExperimentMap());
                                session.update(job);
                                // Submit the job to be processed asynchronously
                                executorService.submit(new BulkJobManager(job.getJobId(), jobStatus, jobStatus.getSummary().getInput()));

                            }
                        }
                        entity.setStatus("stopped");
                        entity.setReason("Pod not running handed over task to POD :" + this.currentPodName);
                        session.saveOrUpdate(entity);
                        tx.commit();
                    } catch (Exception e) {
                        if (tx != null) tx.rollback();
                        e.printStackTrace();
                    }

                }
            }


        }
    }


}
