package com.autotune.service;

public class PodStartupService {

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
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            PodStatus pod = session.get(PodStatus.class, currentPodName);
            if (pod == null) {
                pod = new PodStatus();
                pod.setPodName(currentPodName);
            }
            pod.setStatus("started");
            pod.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            pod.setLastChecked(new Timestamp(System.currentTimeMillis()));

            session.saveOrUpdate(pod);
            tx.commit();
        }
    }

    private void validateAndResumeStalePods() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            Timestamp cutoff = Timestamp.from(Instant.now().minus(Duration.ofMinutes(3)));

            List<PodStatus> stalePods = session.createQuery(
                "FROM PodStatus WHERE status = 'started' AND podName != :currentPod AND (lastChecked IS NULL OR lastChecked < :cutoff)",
                PodStatus.class)
                .setParameter("currentPod", currentPodName)
                .setParameter("cutoff", cutoff)
                .setMaxResults(3)
                .list();

            for (PodStatus pod : stalePods) {
                pod.setLastChecked(new Timestamp(System.currentTimeMillis()));
                session.update(pod);

                Pod k8sPod = k8sClient.pods().inNamespace(namespace).withName(pod.getPodName()).get();
                boolean notRunning = (k8sPod == null || !"Running".equalsIgnoreCase(k8sPod.getStatus().getPhase()));

                if (notRunning) {
                    pod.setStatus("validating");
                    pod.setReason("Pod not running");
                    session.update(pod);

                    List<KruizeBulkJob> jobs = session.createQuery(
                        "FROM KruizeBulkJob WHERE podName = :podName", KruizeBulkJob.class)
                        .setParameter("podName", pod.getPodName())
                        .list();

                    for (KruizeBulkJob job : jobs) {
                        if (!"resumed".equalsIgnoreCase(job.getJobState())) {
                            resumeJob(job);
                            job.setJobState("resumed");
                            session.update(job);
                        }
                    }

                    pod.setStatus("stopped");
                    pod.setReason("Jobs resumed");
                    pod.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                    session.update(pod);
                }
            }

            tx.commit();
        }
    }

    private void resumeJob(KruizeBulkJob job) {
        // Implement actual job resumption logic here
        System.out.println("Resuming job: " + job.getJobId());
    }
}
