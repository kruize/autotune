# KRUIZE TEST PLAN RELEASE 0.7-dev

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

This document describes the test plan for Kruize release 0.7-prod / 0.7-rc1

----

## FEATURES TO BE TESTED

* Add experiment type field as bigint
* Add new DB columns to support experiment type and corresponding tests
* Add UpdateMetadataProfile API in local monitoring mode
* Test updates for Update and Delete MetadataProfile APIs
* Stress test updates for namespace recommendations
* Include GPU validation in remote monitoring demo
* Negative tests for Update Results
------

## BUG FIXES TO BE TESTED

* Add validation checks for namespace experiments
* Update maven distribution URL in the Dockerfile
* Fix vulnerabilities reported by upgrading to latest ubi & kafka client packages
* Fix negative test failure count

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster

---

## TEST DELIVERABLES

### New Test Cases Developed

None

### Regression Testing

| # | ISSUE (NEW FEATURE)                                                             | TEST DESCRIPTION                                                                      | TEST DELIVERABLES | RESULTS | COMMENTS |
|---|---------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|-------------------|---------|----------|
| 1 | Add validation checks for namespace experiments                                 | Tested using functional testsuite and newly added negative tests for update results   |                   | PASSED  |          |
| 2 | Update maven distribution URL in the Dockerfile                                 | Tested manually                                                                       |                   | PASSED  |          |
| 3 | Fix vulnerabilities reported by upgrading to latest ubi & kafka client packages | Tested manually                                                                       |                   | PASSED  |          |
| 4 | Add new DB columns to support experiment type and corresponding tests           | Tests were added for new DB columns (experiment_type, creation_date, update_date      |                   | PASSED  |          |
| 5 | Add UpdateMetadataProfile API in local monitoring mode                          | Tested using functional testsuite and newly added tests for updateMetadataProfile API |                   | PASSED  |          |
| 6 | Fix negative test failure count                                                 | Tested using remote monitoring functional testsuite                                   |                   | PASSED  |          |

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

| Kruize Release                            | Exps / Results / Recos                             | Execution time | Latency (Max/ Avg) in seconds |                |                      | Postgres DB size(MB) | Kruize Max CPU | Kruize Max Memory (GB) |
|-------------------------------------------|----------------------------------------------------|----------------|-------------------------------|----------------|----------------------|----------------------|----------------|------------------------|
|                                           |                                                    |                | UpdateRecommendations         | UpdateResults  | LoadResultsByExpName |                      |                |                        |
| 0.6 (on aws cluster 9th May 2025)         | 5K container exps / 72L / 3L                       | 4h 25 mins     | 0.87 / 0.49                   | 0.12 / 0.1     | 0.38 / 0.26          | 21756                | 7.39           | 35.11                  |
| 0.7-dev (on aws cluster 18th June 2025)   | 5K container exps / 72L / 3L                       | 5h             | 1.03 / 0.56                   | 0.12 / 0.1     | 0.33 / 0.22          | 21757                | 9.47           | 42.45                  |
| 0.7-dev (on aws cluster 18th June 2025)   | 5K namespace exps / 72L / 3L                       | 3h 17 mins     | 0.62 / 0.35                   | 0.11 / 0.08    | 0.26 / 0.16          | 10524                | 5.37           | 28.48                  |
| 0.7-dev (on aws cluster 18th June 2025)   | 4.5k container exps, 500 namespace exps / 72L / 3L | 4h 59 mins     | 0.98 / 0.54                   | 0.12 / 0.1     | 0.31 / 0.22          | 20631                | 9.13           | 34.45                  |
| 0.7-rc1 (on aws cluster 12th August 2025) | 5K container exps / 72L / 3L                       | 4h 29 mins     | 0.88 / 0.49                   | 0.13 / 0.1     | 0.4 / 0.27           | 21759                | 8.31           | 39.37                  |
 
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
| 1  | Kruize Remote monitoring Functional testsuite  | TOTAL - 527, PASSED - 485 / FAILED - 42                | TOTAL - 527, PASSED - 485 / FAILED - 42                | Existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610)                                                                                                                                                                                                                                                                                      |
| 2  | Fault tolerant test                            | PASSED                                                 | PASSED                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 3  | Stress test                                    | PASSED                                                 | PASSED                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 4  | Scalability test (short run)                   | PASSED                                                 |                                                        | Execution time differs from prev release for container exps, check the table above                                                                                                                                                                                                                                                                                                                                |
| 5  | DB Migration test                              | PASSED                                                 | PASSED                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 6  | Recommendation and box plot values validations | PASSED                                                 | PASSED                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 7  | Kruize remote monitoring demo                  | PASSED                                                 | PASSED                                                 | [Issue 1633](https://github.com/kruize/autotune/issues/1633)                                                                                                                                                                                                                                                                                                                                                      |
| 8  | Kruize Local monitoring demo                   | PASSED                                                 | PASSED                                                 | Tested manually, works fine on openshift and kind                                                                                                                                                                                                                                                                                                                                                                 |
| 9  | Kruize Bulk demo                               | PASSED                                                 | PASSED                                                 | Tested manually, works fine on openshift                                                                                                                                                                                                                                                                                                                                                                          |
| 10 | Kruize Local Functional tests                  | TOTAL - 147 , PASSED - 140  / FAILED - 6 / SKIPPED - 1 | TOTAL - 147 , PASSED - 140  / FAILED - 6 / SKIPPED - 1 | [Issue 1395](https://github.com/kruize/autotune/issues/1395), [Issue 1217](https://github.com/kruize/autotune/issues/1217), [Issue 1273](https://github.com/kruize/autotune/issues/1273)   GPU accelerator test failed, failure can be ignored for now [PR 1437](https://github.com/kruize/autotune/pull/1437) added for create_exp failures which will go in 0.4, Bulk API tests need updation due to new format |
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
