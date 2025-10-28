# KRUIZE TEST PLAN RELEASE 0.7.1

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

This document describes the test plan for Kruize release 0.7.1

----

## FEATURES TO BE TESTED

* Add updatePerformanceProfile & deletePerformanceProfile APIs in remote monitoring mode
* Test updates for Update Performance Profile API
* Refactor DDL files

------

## BUG FIXES TO BE TESTED

* Fix vulnerabilities reported by upgrading to latest ubi & netty packages
* Fix and validate "version" field while importing metadata in local monitoring

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster

---

## TEST DELIVERABLES

### New Test Cases Developed

| # | ISSUE (NEW FEATURE)                                                        | TEST DESCRIPTION                                                       | TEST DELIVERABLES                                    | RESULTS | COMMENTS |
|---|----------------------------------------------------------------------------|------------------------------------------------------------------------|------------------------------------------------------|---------|----------|
| 1 | Add functional tests for updatePeformanceProfile API | Added new testcases, tested using functional tests                      | [1657](https://github.com/kruize/autotune/pull/1657) | PASSED  |          |
| 2 | Add a backward compatible test for updatePerformanceProfile  API |    | [1660](https://github.com/kruize/autotune/pull/1660) | PASSED  |          |

### Regression Testing

| # | ISSUE (NEW FEATURE)                                                             | TEST DESCRIPTION                                                                      | TEST DELIVERABLES                                    | RESULTS | COMMENTS |
|---|---------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|------------------------------------------------------|---------|----------|
| 1 | Fix vulnerabilities reported by upgrading to latest ubi & netty client packages | Tested manually                                                                       |                                                      | PASSED  |          |
| 2 | Fix and validate "version" field while importing metadata in local monitoring   | LM functional testsuite |  | PASSED  |  |

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

| Kruize Release                            | Exps / Results / Recos                             | Execution time | Latency (Max/ Avg) in seconds |               |                      | Postgres DB size(MB) | Kruize Max CPU | Kruize Max Memory (GB) |
|-------------------------------------------|----------------------------------------------------|----------------|-------------------------------|---------------|----------------------|----------------------|----------------|------------------------|
|                                           |                                                    |                | UpdateRecommendations         | UpdateResults | LoadResultsByExpName |                      |                |                        |
| 0.6 (on aws cluster 9th May 2025)         | 5K container exps / 72L / 3L                       | 4h 25 mins     | 0.87 / 0.49                   | 0.12 / 0.1    | 0.38 / 0.26          | 21756                | 7.39           | 35.11                  |
| 0.7-rc1 (on aws cluster 12th August 2025) | 5K container exps / 72L / 3L                       | 4h 29 mins     | 0.88 / 0.49                   | 0.13 / 0.1    | 0.4 / 0.27           | 21759                | 8.31           | 39.37                  |
| 0.7-rc1 (on aws cluster 14th August 2025) | 5K namespace exps / 72L / 3L                       | 3h 02 mins     | 0.55 / 0.3                    | 0.1 / 0.08    | 0.3 / 0.19           | 10525                | 4.01           | 24.08                  |
| 0.7-rc1 (on aws cluster 14th August 2025) | 4.5k container exps, 500 namespace exps / 72L / 3L | 4h 21 mins     | 0.82 / 0.47                   | 0.12 / 0.1    | 0.38 / 0.26          | 20627                | 7.12           | 42.33                  |
| 0.7.1-rc1 (on aws cluster 25th October 2025) | 5K container exps / 72L / 3L                       | 4h 40 mins     | 0.91 / 0.51                   | 0.14 / 0.11    | 0.46 / 0.31           | 21755                | 7.91           | 42.37                  |
| 0.7.1-rc1 (on aws cluster 26th October 2025) | 5K namespace exps / 72L / 3L                       | 2h 56 mins     | 0.53 / 0.3                    | 0.11 / 0.08    | 0.29 / 0.18           | 10523                | 7.33           | 26.58                  |
| 0.7.1-rc1 (on aws cluster 26th October 2025) | 4.5k container exps, 500 namespace exps / 72L / 3L | 4h 25 mins     | 0.83 / 0.47                   | 0.13 / 0.1    | 0.4 / 0.28          | 20628                | 6.51           | 43.1                  |
| 0.7.1-rc2 (on aws cluster 28th October 2025) | 5K container exps / 72L / 3L                       | 4h 38 mins     | 0.9 / 0.5                   | 0.14 / 0.11    | 0.45 / 0.29           | 21757               | 7.66           | 39.6                  |
| 0.7.1-rc2 (on aws cluster 27th October 2025) | 5K namespace exps / 72L / 3L                       | 2h 54 mins     | 0.52 / 0.29                    | 0.1 / 0.08    | 0.28 / 0.18           | 10523                | 5.52           | 25.6                  |
| 0.7.1-rc2 (on aws cluster 28th October 2025) | 4.5k container exps, 500 namespace exps / 72L / 3L | 4h 20 mins     | 0.81 / 0.46                   | 0.13 / 0.1    | 0.38 / 0.26          | 20628                | 6.87           | 40.0                  |

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
| 1  | Kruize Remote monitoring Functional testsuite  | TOTAL - 705, PASSED - 661 / FAILED - 42 / SKIPPED - 1  | TOTAL - 705, PASSED - 661 / FAILED - 43 / SKIPPED - 1  | Existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610)                                                                                                                                                                                                                                                                                      |
| 2  | Fault tolerant test                            | PASSED                                                 | PASSED                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 3  | Stress test                                    | PASSED                                                 | PASSED                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 4  | Scalability test (short run)                   | PASSED                                                 | PASSED                                                 | Execution time differs from prev release for container exps and Kruize max memory for container and namespace exps, check the table above                                                                                                                                                                                                                                                                         |
| 5  | DB Migration test                              | PASSED                                                 | PASSED                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 6  | Recommendation and box plot values validations | PASSED                                                 | PASSED                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 7  | Kruize remote monitoring demo                  | PASSED                                                 | PASSED                                                 | [Issue 1633](https://github.com/kruize/autotune/issues/1633)                                                                                                                                                                                                                                                                                                                                                      |
| 8  | Kruize Local monitoring demo                   | PASSED                                                 | PASSED                                                 | Tested manually, works fine on openshift and kind                                                                                                                                                                                                                                                                                                                                                                 |
| 9  | Kruize Bulk demo                               | PASSED                                                 | FAILED                                                 | Tested manually, kruize operator deployment fails on openshift cluster demo needs to be fixed                                                                                                                                                                                                                                                                                                                                                                          |
| 10 | Kruize Local Functional tests                  | TOTAL - 152 , PASSED - 146  / FAILED - 6 / SKIPPED - 1 | TOTAL - 152 , PASSED - 144  / FAILED - 8 / SKIPPED - 1 | [Issue 1395](https://github.com/kruize/autotune/issues/1395), [Issue 1217](https://github.com/kruize/autotune/issues/1217), [Issue 1273](https://github.com/kruize/autotune/issues/1273)   GPU accelerator test failed, failure can be ignored for now [PR 1437](https://github.com/kruize/autotune/pull/1437) added for create_exp failures which will go in 0.4, Bulk API tests need updation due to new format |
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
