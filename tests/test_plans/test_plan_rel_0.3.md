# KRUIZE TEST PLAN RELEASE 0.3

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

This document describes the test plan for Kruize remote monitoring release 0.3

----

## FEATURES TO BE TESTED

* Concurrent RM and LM changes
* Bulk API filtration feature 
* Auto mode support in Kruize for VPA integration PoC


------

## BUG FIXES TO BE TESTED

* ListExperiments API Not Listing Recommendation
* Namespace experiments not getting created
* `No data available` issue in GenereateRecommendations API
* Bulk API test failures
* Parallel requests issue with Bulk API
* Jetty Server version upgrade

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster

---

## TEST DELIVERABLES

### New Test Cases Developed

| # | ISSUE (NEW FEATURE)          | TEST DESCRIPTION                                      | TEST DELIVERABLES                                      | RESULTS | COMMENTS |
|---|------------------------------|-------------------------------------------------------|--------------------------------------------------------|---------|----------|
| 1 | Concurrent RM and LM changes | Functional suite updated                              | [1424](https://github.com/kruize/autotune/pull/1424)   | PASSED  |          |
| 2 | VPA changes PoC              | Demo code added                                       | [107](https://github.com/kruize/kruize-demos/pull/107) | PASSED  |          |
| 3 | Bulk API filtration feature  | Tests will be added later, tested using the Bulk demo |                                                        |         |          |

### Regression Testing

| # | ISSUE (BUG/NEW FEATURE)                                   | TEST CASE                                 | RESULTS | COMMENTS |
|---|-----------------------------------------------------------|-------------------------------------------|---------|----------|
| 1 | ListExperiments API Not Listing Recommendation            | Kruize local monitoring Bulk demo         | PASSED  |          | 
| 2 | Namespace experiments not getting created                 | Kruize local monitoring demo              | PASSED  |          |
| 3 | `No data available` issue in GenereateRecommendations API | Kruize local monitoring demo              | PASSED  |          |
| 4 | Bulk API test failures                                    | Kruize local monitoring bulk service demo | PASSED  |          |
| 5 | Parallel requests issue with Bulk API                     | Kruize local monitoring bulk service demo | PASSED  |          |
| 6 | Jetty Server version upgrade                              | Quay repo scan                            | PASSED  |          |

---

## SCALABILITY TESTING

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

Short Scalability run
- 5K exps / 15 days of results / 2 containers per exp
- Kruize replicas - 10
- OCP - Scalelab cluster

| Kruize Release | Exps / Results / Recos | Execution time | Latency (Max/ Avg) in seconds |               |                      | Postgres DB size(MB) | Kruize Max CPU | Kruize Max Memory (GB) |
|----------------|------------------------|----------------|-------------------------------|---------------|----------------------|----------------------|----------------|------------------------|
|                |                        |                | UpdateRecommendations         | UpdateResults | LoadResultsByExpName |                      |                |                        |
| 0.2            | 5K / 72L / 3L          | 4h 08 mins     | 0.81 / 0.48                   | 0.14 / 0.12   | 0.55 / 0.38          | 21749                | 4.78           | 25.31                  |
| 0.3            | 5K / 72L / 3L          | 4h 36 mins     | 1.0 / 0.52                    | 0.07 / 0.07   | 0.53 / 0.33          | 21753                | 7.76           | 11.74                  |

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


| # | TEST SUITE                                     | EXPECTED RESULTS                        | ACTUAL RESULTS                          | COMMENTS                                                                                                                                                                                                                                                                                                                                                          |
|---|------------------------------------------------|-----------------------------------------|-----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| 
| 1 | Kruize Remote monitoring Functional testsuite  | TOTAL - 359, PASSED - 316 / FAILED - 43 | TOTAL - 359, PASSED - 316 / FAILED - 43 | Intermittent issue seen [1281](https://github.com/kruize/autotune/issues/1281), [1393](https://github.com/kruize/autotune/issues/1393), existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610)                                                                                              |
| 2 | Fault tolerant test                            | PASSED                                  | PASSED                                  |                                                                                                                                                                                                                                                                                                                                                                   |
| 3 | Stress test                                    | PASSED                                  |                                         | [Intermittent failure](https://github.com/kruize/autotune/issues/1106)                                                                                                                                                                                                                                                                                            |
| 4 | Scalability test (short run)                   | PASSED                                  | PASSED                                  | Exps - 5000, Results - 72000, execution time - 4 hours, 36 mins                                                                                                                                                                                                                                                                                                   |
| 5 | DB Migration test                              | PASSED                                  | PASSED                                  | Tested on scalelab openshift cluster                                                                                                                                                                                                                                                                                                                              |
| 6 | Recommendation and box plot values validations | PASSED                                  | PASSED                                  | Tested on scalelab                                                                                                                                                                                                                                                                                                                                                |
| 7 | Kruize remote monitoring demo                  | PASSED                                  | PASSED                                  | Tested manually                                                                                                                                                                                                                                                                                                                                                   |
| 8 | Kruize Local monitoring demo                   | PASSED                                  | PASSED                                  | Tested manually                                                                                                                                                                                                                                                                                                                                                   |
| 8 | Kruize Bulk demo                               | PASSED                                  | PASSED                                  | Tested manually                                                                                                                                                                                                                                                                                                                                                   |
| 9 | Kruize Local Functional tests                  | TOTAL - 81 , PASSED - 78  / FAILED - 3  | TOTAL - 81 , PASSED - 59  / FAILED - 22 | [Issue 1395](https://github.com/kruize/autotune/issues/1395), [Issue 1217](https://github.com/kruize/autotune/issues/1217), [Issue 1273](https://github.com/kruize/autotune/issues/1273)   GPU accelerator test failed, failure can be ignored for now [PR 1437](https://github.com/kruize/autotune/pull/1437) added for create_exp failures which will go in 0.4 |

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
