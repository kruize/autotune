# KRUIZE TEST PLAN RELEASE 0.5

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

This document describes the test plan for Kruize release 0.5

----

## FEATURES TO BE TESTED

* Default metric & metadata profiles setup
* Integration of metadata into bulk config and create experiment
* Save BulkJob to DB
* Kafka integration changes 
* Update to latest UBI minimal 9.5 version
* CVE fixes to netty handler
* Tests for default metadata & metric profiles setup
* Tests and demo updates related to metadata integration


------

## BUG FIXES TO BE TESTED

* Check for auto or recreate mode to query insta slice object
* Enable GPU metrics for only in non-ROS case

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster

---

## TEST DELIVERABLES

### New Test Cases Developed

| # | ISSUE (NEW FEATURE)          | TEST DESCRIPTION                                               | TEST DELIVERABLES                                    | RESULTS | COMMENTS |
|---|------------------------------|----------------------------------------------------------------|------------------------------------------------------|---------|----------|
| 1 | Default metric & metadata profiles setup |                                                                |                                                      | PASSED  |          |
| 2 | Save BulkJob to DB   | Tested using Bulk Demo                                         |                                                      | PASSED  |          |
| 3 | Integration of metadata into bulk config and create experiment  | Updated existing tests and also added new tests                |                                                | PASSED  |          |
| 4 | Kafka integration changes  | Tests will be added later, Tested with kafka demo              |                                                      | PASSED  |          |


### Regression Testing

| # | ISSUE (BUG/NEW FEATURE)                                   | TEST CASE                                          | RESULTS | COMMENTS |
|---|-----------------------------------------------------------|----------------------------------------------------|---------|----------|
| 1 | Check for auto or recreate mode to query insta slice object          | Kruize local monitoring Bulk demo                  | PASSED  |          | 
| 2 | Enable GPU metrics for only in non-ROS case | Kruize local monitoring Bulk demo                  | PASSED  |          |
| 3 | CVE fixes to netty handler | Verified the quay scan report for the docker image | PASSED  |          |

---

## SCALABILITY TESTING

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

Short Scalability run
- 5K exps / 15 days of results / 2 containers per exp
- Kruize replicas - 10
- OCP - Scalelab cluster
- PV/PVC storage - 1Gi
- Kruize DB resources - requests - 10Gi / 2 cores, limits - 30Gi / 2 cores
- Kruize resources - requests - 4Gi / 2 cores, limits - 8Gi / 2 cores

| Kruize Release                                         | Exps / Results / Recos | Execution time | Latency (Max/ Avg) in seconds |               |                   | Postgres DB size(MB) | Kruize Max CPU | Kruize Max Memory (GB) |
|--------------------------------------------------------|------------------------|----------------|-------------------------------|---------------|-------------------|----------------------|----------------|------------------------|
|                                                        |                        |                | UpdateRecommendations         | UpdateResults | LoadResultsByExpName |                      |                |                        |
| 0.4 (on old scalelab cluster - run on 18th feb 2025)   | 5K / 72L / 3L          | 4h 27 mins     | 0.96 / 0.51                   | 0.07 / 0.07   | 0.48 / 0.31       | 21755                | 8.04           | 11.58                  |
| 0.5 (on old scalelab cluster - run on 26th March 2025) | 5K / 72L / 3L          | 4h 02 mins     | 0.82 / 0.48                   | 0.12 / 0.1    | 0.44 / 0.3        | 21752                | 5.84           | 38.05                  |
| 0.4 (on old scalelab cluster - run on 27th March 2025) | 5K / 72L / 3L          | 4h 02 mins     | 0.82 / 0.48                   | 0.12 / 0.1    | 0.43 / 0.3        | 21756                | 6.03           | 37.12                  |

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


| #  | TEST SUITE                                     | EXPECTED RESULTS                         | ACTUAL RESULTS                           | COMMENTS                                                                                                                                                                                                                                                                                                                                                                                                          |
|----|------------------------------------------------|------------------------------------------|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| 
| 1  | Kruize Remote monitoring Functional testsuite  | TOTAL - 359, PASSED - 316 / FAILED - 43  | TOTAL - 359, PASSED - 316 / FAILED - 43  | Intermittent issue seen [1281](https://github.com/kruize/autotune/issues/1281), [1393](https://github.com/kruize/autotune/issues/1393), existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610)                                                                                                                                              |
| 2  | Fault tolerant test                            | PASSED                                   | PASSED                                   |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 3  | Stress test                                    | PASSED                                   | PASSED                                   | Changed users to 10 (as connection issues & 504 gateway timeouts are seen with 50 users)                                                                                                                                                                                                                                                                                                                          |
| 4  | Scalability test (short run)                   | PASSED                                   | PASSED                                   | Exps - 5000, Results - 72000, execution time - 4 hours, 27 mins                                                                                                                                                                                                                                                                                                                                                   |
| 5  | DB Migration test                              | PASSED                                   | PASSED                                   | Tested on scalelab openshift cluster                                                                                                                                                                                                                                                                                                                                                                              |
| 6  | Recommendation and box plot values validations | PASSED                                   | FAILED                                   | Recommendations do not match for one of the csvs. Tested on scalelab and aws cluster. Also fails on minikube. [JIRA](https://issues.redhat.com/browse/KRUIZE-745)                                                                                                                                                                                                                                                 |
| 7  | Kruize remote monitoring demo                  | PASSED                                   | PASSED                                   | Tested manually, works fine on openshift and minikube                                                                                                                                                                                                                                                                                                                                                             |
| 8  | Kruize Local monitoring demo                   | PASSED                                   | PASSED                                   | Tested manually, works fine on openshift and minikube                                                                                                                                                                                                                                                                                                                                                             |
| 9  | Kruize Bulk demo                               | PASSED                                   | PASSED                                   | Tested manually, works fine on openshift and minikube                                                                                                                                                                                                                                                                                                                                                             |
| 10 | Kruize Local Functional tests                  | TOTAL - 126 , PASSED - 119  / FAILED - 7 | TOTAL - 126 , PASSED - 119  / FAILED - 7 | [Issue 1395](https://github.com/kruize/autotune/issues/1395), [Issue 1217](https://github.com/kruize/autotune/issues/1217), [Issue 1273](https://github.com/kruize/autotune/issues/1273)   GPU accelerator test failed, failure can be ignored for now [PR 1437](https://github.com/kruize/autotune/pull/1437) added for create_exp failures which will go in 0.4, Bulk API tests need updation due to new format |
| 11 | Local Fault tolerant test                      | PASSED                                   | FAILED                                   | [Issue 1552](https://github.com/kruize/autotune/issues/1552)                                                                                                                                                                                                                                                                                                                                                      |

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
