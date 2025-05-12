# KRUIZE TEST PLAN RELEASE 0.6

- [INTRODUCTION](#introduction)
- [FEATURES TO BE TESTED](#features-to-be-tested)
- [BUG FIXES TO BE TESTED](#bug-fixes-to-be-tested)
- [TEST ENVIRONMENT](#test-environment)
- [TEST DELIVERABLES](#test-deliverables)
    - [New Test Cases Developed](#new-test-cases-developed)
    - [Regression Testing](#regresion-testing)
- [SCALABILITY TESTING](#scalability-testing)
- [RELEASE TESTING](#release-testing)
- [TEST METRICS](#test-metrics)
- [RISKS AND CONTINGENCIES](#risks-and-contingencies)
- [APPROVALS](#approvals)

-----

## INTRODUCTION

This document describes the test plan for Kruize release 0.6

----

## FEATURES TO BE TESTED

* None

------

## BUG FIXES TO BE TESTED

* Fix the number of pods for a time interval while calculating recommendations
* Update maven link in the Dockerfile
* Updated RH UBI 9.5 minimal to latest available

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster

---

## TEST DELIVERABLES

### New Test Cases Developed

None

### Regression Testing

| # | ISSUE (NEW FEATURE)          | TEST DESCRIPTION                                  | TEST DELIVERABLES                                    | RESULTS | COMMENTS |
|---|------------------------------|---------------------------------------------------|------------------------------------------------------|---------|----------|
| 1 | Fix the number of pods for a time interval while calculating recommendations | Tested using recommendation infra                 |     | PASSED  |          |
| 2 | Update maven link in the Dockerfile   | Tested manually                                   |                                            | PASSED  |          |
| 3 | Updated RH UBI 9.5 minimal to latest available  | Tested manually and also using PR check  |                  | PASSED  |          |

---

## SCALABILITY TESTING

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

Short Scalability run
- 5K exps / 15 days of results / 2 containers per exp
- Kruize replicas - 10
- OCP - AWS cluster
- PV/PVC storage - 1Gi
- Kruize DB resources - requests - 10Gi / 2 cores, limits - 30Gi / 2 cores
- Kruize resources - requests - 4Gi / 2 cores, limits - 8Gi / 2 cores

| Kruize Release                            | Exps / Results / Recos | Execution time | Latency (Max/ Avg) in seconds |               |                      | Postgres DB size(MB) | Kruize Max CPU | Kruize Max Memory (GB) |
|-------------------------------------------|------------------------|----------------|-------------------------------|---------------|----------------------|----------------------|----------------|------------------------|
|                                           |                        |                | UpdateRecommendations         | UpdateResults | LoadResultsByExpName |                      |                |                        |
| 0.0.25_mvp (on aws cluster 23rd Apr 2025) | 5K / 72L / 3L          | 4h 27 mins     | 0.87 / 0.49                   | 0.13 / 0.1    | 0.43 / 0.27          | 21751                | 7.17           | 41.23                  |
| 0.5 (on aws cluster 24th Apr 2025)        | 5K / 72L / 3L          | 4h 46 mins     | 0.9 / 0.5                     | 0.16 / 0.12   | 0.55 / 0.38          | 21752                | 8.15           | 33.42                  |
| 0.6 (on aws cluster 9th May 2025)         | 5K / 72L / 3L          | 4h 25 mins     | 0.87 / 0.49                   | 0.12 / 0.1    | 0.38 / 0.26          | 21756                | 7.39           | 35.11                  |

Long Scalability run
- 100K exps / 15 days of results / 2 containers per exp
- Kruize replicas - 10
- OCP - AWS cluster
- PV/PVC storage - 20Gi (DB scheduled on worker node with atleast 500 Gi ephemeral storage)
- Kruize DB resources - requests - 10Gi / 2 cores, limits - 30Gi / 2 cores
- Kruize resources - requests - 4Gi / 2 cores, limits - 8Gi / 2 cores

| Kruize Release                            | Exps / Results / Recos | Execution time | Latency (Max/ Avg) in seconds |               |                      | Postgres DB size(GB) | Kruize Max CPU | Kruize Max Memory (GB) |
|-------------------------------------------|------------------------|----------------|-------------------------------|---------------|----------------------|----------------------|----------------|------------------------|
|                                           |                        |                | UpdateRecommendations         | UpdateResults | LoadResultsByExpName |                      |                |                        |
| 0.0.25_mvp (on aws cluster 17th Apr 2025) | 100K / 144M / 60L      | 142 h 26 mins  | 1.27 / 0.72                   | 0.14 / 0.13   | 0.96 / 0.73          | 424                  | 6.32           | 37.63                  |
| 0.5 (on aws cluster 7th Apr 2025)         | 100K / 144M / 60L          | 151 h 45 mins  | 1.43 / 0.76               | 0.11 / 0.1    | 0.99 / 0.72          | 424                  | 8.4            | 42.95                  |
| 0.6 (on aws cluster 10th May 2025)        | 100K / 144M / 60L          | 4h 25 mins     |                | 0.12 / 0.1    | 0.38 / 0.26          | 21756                | 7.39           | 35.11                  |
 
----
## RELEASE TESTING

As part of the release testing, following tests will be executed:
- [Kruize Remote monitoring Functional tests](/tests/scripts/remote_monitoring_tests/Remote_monitoring_tests.md)
- [Fault tolerant test](/tests/scripts/remote_monitoring_tests/fault_tolerant_tests.md)
- [Stress test](/tests/scripts/remote_monitoring_tests/README.md)
- [DB Migration test](/tests/scripts/remote_monitoring_tests/db_migration_test.md)
- [Recommendation and box plot values validation test](https://github.com/kruize/kruize-demos/blob/main/monitoring/remote_monitoring_demo/recommendations_infra_demo/README.md)
- [Scalability test (On openshift)](/tests/scripts/remote_monitoring_tests/scalability_test.md) - scalability test with 5000 exps / 15 days usage data
- [Kruize remote monitoring demo (On minikube)](https://github.com/kruize/kruize-demos/blob/main/monitoring/remote_monitoring_demo/README.md)
- [Kruize local monitoring demo (On openshift)](https://github.com/kruize/kruize-demos/blob/main/monitoring/local_monitoring_demo)
- [Kruize local monitoring Functional tests](/tests/scripts/local_monitoring_tests/Local_monitoring_tests.md)


| #  | TEST SUITE                                     | EXPECTED RESULTS                                       | ACTUAL RESULTS                                         | COMMENTS                                                                                                                                                                                                                                                                                                                                                                                                          |
|----|------------------------------------------------|--------------------------------------------------------|--------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| 
| 1  | Kruize Remote monitoring Functional testsuite  | TOTAL - 359, PASSED - 316 / FAILED - 43                | TOTAL - 359, PASSED - 316 / FAILED - 43                | Intermittent issue seen [1281](https://github.com/kruize/autotune/issues/1281), [1393](https://github.com/kruize/autotune/issues/1393), existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610)                                                                                                                                              |
| 2  | Fault tolerant test                            | PASSED                                                 | PASSED                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 3  | Stress test                                    | PASSED                                                 | PASSED                                                 | Changed users to 10 (as connection issues & 504 gateway timeouts are seen with 50 users)                                                                                                                                                                                                                                                                                                                          |
| 4  | Scalability test (short run)                   | PASSED                                                 | PASSED                                                 | Exps - 5000, Results - 72000, execution time - 4 hours, 25 mins                                                                                                                                                                                                                                                                                                                                                   |
| 5  | DB Migration test                              | PASSED                                                 | PASSED                                                 | Tested on aws openshift cluster                                                                                                                                                                                                                                                                                                                                                                                   |
| 6  | Recommendation and box plot values validations | PASSED                                                 | PASSED                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 7  | Kruize remote monitoring demo                  | PASSED                                                 | PASSED                                                 | Tested manually, works fine on openshift and minikube                                                                                                                                                                                                                                                                                                                                                             |
| 8  | Kruize Local monitoring demo                   | PASSED                                                 | PASSED                                                 | Tested manually, works fine on openshift and minikube                                                                                                                                                                                                                                                                                                                                                             |
| 9  | Kruize Bulk demo                               | PASSED                                                 | PASSED                                                 | Tested manually, works fine on openshift and minikube                                                                                                                                                                                                                                                                                                                                                             |
| 10 | Kruize Local Functional tests                  | TOTAL - 127 , PASSED - 119  / FAILED - 7 / SKIPPED - 1 | TOTAL - 127 , PASSED - 119  / FAILED - 7 / SKIPPED - 1 | [Issue 1395](https://github.com/kruize/autotune/issues/1395), [Issue 1217](https://github.com/kruize/autotune/issues/1217), [Issue 1273](https://github.com/kruize/autotune/issues/1273)   GPU accelerator test failed, failure can be ignored for now [PR 1437](https://github.com/kruize/autotune/pull/1437) added for create_exp failures which will go in 0.4, Bulk API tests need updation due to new format |
| 11 | Local Fault tolerant test                      | PASSED                                                 | PASSED                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                   |

---

## TEST METRICS

### Test Completion Criteria

* All must_fix defects identified for the release are fixed
* New features work as expected and tests have been added to validate these
* No new regressions in the functional tests
* All non-functional tests work as expected without major issues
* Documentation updates have been completed

----

## RISKS AND CONTINGENCIES

* None

----
## APPROVALS

Sign-off

----
