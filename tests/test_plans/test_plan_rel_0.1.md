# KRUIZE TEST PLAN RELEASE 0.1

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

This document describes the test plan for Kruize remote monitoring release 0.1

----

## FEATURES TO BE TESTED

* Bulk API support
* Enabling kruize local by default
* Kruize GPU recommendations for MIG
* Test updates for Kruize local default & GPU
* Updates to DS Metadata queries

------

## BUG FIXES TO BE TESTED

* Exclude namespace queries to fetch container metrics

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster 

---

## TEST DELIVERABLES

### New Test Cases Developed

| # | ISSUE (NEW FEATURE)                                                                        | TEST DESCRIPTION                                      | TEST DELIVERABLES | RESULTS | COMMENTS |
|---|--------------------------------------------------------------------------------------------|-------------------------------------------------------|-------------------|---------| --- |
| 1 | [Bulk API support] (https://github.com/kruize/autotune/pull/1323)                          | Tests will be added later, tested using the Bulk demo |                   | PASSED  | |
| 2 | [Enabling kruize local by default](https://github.com/kruize/autotune/pull/1289)           | Existing tests updated                                |                   | PASSED  | |
| 3 | Kruize GPU recommendations for MIG [1312](https://github.com/kruize/autotune/issue/1312)   | Tested manually                                       |                   | PASSED  | |
| 4 | [Updates to DS Metadata queries](https://github.com/kruize/autotune/pull/1322)             | Regression testing                                    |                   | PASSED  | |



### Regression Testing

| # | ISSUE (BUG/NEW FEATURE)                               | TEST CASE                         | RESULTS | COMMENTS |
|---|-------------------------------------------------------|-----------------------------------|---------| --- |
| 1 | Exclude namespace queries to fetch container metrics  | Kruize local monitoring tests     | PASSED | | 
| 2 | Enabling kruize local by default                      | Kruize local and remote monitoring functional tests | PASSED | |

---

## SCALABILITY TESTING

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

Short Scalability run
- 5K exps / 15 days of results / 2 containers per exp
- Kruize replicas - 10
- OCP - Scalelab cluster

Kruize Release | Exps / Results / Recos | Execution time | Latency (Max/ Avg) in seconds |               |                      | Postgres DB size(MB) | Kruize Max CPU | Kruize Max Memory (GB)
-- |------------------------|----------------|-------------------------------|---------------|----------------------|----------------------|----------------| --
  |   |     |                | UpdateRecommendations         | UpdateResults | LoadResultsByExpName |                      |                |  
0.0.25_mvp | 5K / 72L / 3L | 4h 06 mins     | 0.8 / 0.47                    | 0.14 / 0.12   | 0.52 / 0.36          | 21756                | 4.91           | 30.13
0.1 | 5K / 72L / 3L | 5h 02 mins     | 0.97 / 0.55                   | 0.16 / 0.14   | 0.52 / 0.36          | 21757                | 7.3            | 33.67

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


| #   | TEST SUITE | EXPECTED RESULTS                        | ACTUAL RESULTS                          | COMMENTS                                                                                                                                                                                                     |
| --- | ---------- |-----------------------------------------|-----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| 
| 1   |  Kruize Remote monitoring Functional testsuite | TOTAL - 359, PASSED - 316 / FAILED - 44 | TOTAL - 359, PASSED - 316 / FAILED - 44 | Intermittent issue seen [1281](https://github.com/kruize/autotune/issues/1281), existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610) |
| 2   |  Fault tolerant test | PASSED                                  | PASSED                                  |                                                                                                                                                                                                              |
| 3   |  Stress test | PASSED                                  | PASSED                                  |                                                                                                                                                                                                              |
| 4   |  Scalability test (short run)| PASSED                                  | PASSED                                  | Exps - 5000, Results - 72000, execution time - 5 hours 2 mins, there is an increase in execution time by an hour as the test was run on a different cluster (AWS cluster  kruize scalelab)                   |
| 5   |  DB Migration test | PASSED                                  | PASSED                                  | Tested on Openshift                                                                                                                                                                                          |
| 6   |  Recommendation and box plot values validations | PASSED                                  | PASSED                                  |                                                                                                                                                                                                              |
| 7   |  Kruize remote monitoring demo | PASSED                                  | PASSED                                  | Tested manually                                                                                                                                                                                              |
| 8   |  Kruize Local monitoring demo | PASSED                                  | PASSED                                  |                                                                                                                                                                                                              |
| 9   |  Kruize Local Functional tests | TOTAL - 79, PASSED - 74 / FAILED - 4    | TOTAL - 79, PASSED - 74 / FAILED - 4    | [Issue 1217](https://github.com/kruize/autotune/issues/1217), [Issue 1273](https://github.com/kruize/autotune/issues/1273)   GPU accelerator test failed, failure can be ignored for now                     |

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

