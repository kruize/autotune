# KRUIZE TEST PLAN RELEASE 0.0.24

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

This document describes the test plan for Kruize remote monitoring release 0.0.24

----

## FEATURES TO BE TESTED

* Metric profile creation, listing and deletion through APIs 
* Support for ‘kind’ in Kruize local monitoring demo

------

## BUG FIXES TO BE TESTED


---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster 

---

## TEST DELIVERABLES

### New Test Cases Developed

| # | ISSUE (NEW FEATURE)                                                                       | TEST DESCRIPTION | TEST DELIVERABLES                                                  | RESULTS | COMMENTS |
|---|-------------------------------------------------------------------------------------------| ---------------- |--------------------------------------------------------------------|---------| --- |
| 1 | Tests for create, list operations in Metric profile API                                   | [Included new tests](https://github.com/kruize/autotune/blob/master/tests/scripts/local_monitoring_tests/Local_monitoring_tests.md) | Tests added - [1231](https://github.com/kruize/autotune/pull/1231) | PASSED  | |
| 2 | [Support for ‘kind’ in kruize local demo](https://github.com/kruize/kruize-demos/pull/81) | local monitoring demo has been updated to use kind |                                                                    | PASSED | |


### Regression Testing

| #   | ISSUE (BUG/NEW FEATURE)                                                                                   | TEST CASE                                | RESULTS | COMMENTS |
| --- |-----------------------------------------------------------------------------------------------------------|------------------------------------------|---------| --- |
| 1   | Kruize Metric profile API                                                                                 | Kruize local monitoring functional tests | PASSED | | 

---

## SCALABILITY TESTING

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

Short Scalability run
- 5K exps / 15 days of results / 2 containers per exp
- Kruize replicas - 10
- OCP - Scalelab cluster

Kruize Release | Exps / Results / Recos | Execution time        | Latency (Max/ Avg) in seconds |                      |              | Postgres DB size(MB) | Kruize Max CPU | Kruize Max Memory (GB)
-- |------------------------|-----------------------|-------------------------------|----------------------|--------------|----------------------|----------------| --
  |   |     |                   | UpdateRecommendations | UpdateResults                 | LoadResultsByExpName |              |                      |  
0.0.23_mvp | 5K / 72L / 3L | 3h 30 mins | 0.66 / 0.41 | 0.14 / 0.12 | 0.22 / 0.18 |  21767 | 6.96 | 36.35
0.0.24_mvp | 5K / 72L / 3L |  |  |  |  |   |  | 

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


| #   | TEST SUITE | EXPECTED RESULTS | ACTUAL RESULTS | COMMENTS |
| --- | ---------- |------------------|------------|--------------| 
| 1   |  Kruize Remote monitoring Functional testsuite | TOTAL - 357, PASSED -  / FAILED -  | TOTAL - 357, PASSED - / FAILED -  | No new regressions seen, existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610) |
| 2   |  Fault tolerant test |  |  |  |
| 3   |  Stress test |    |  |  |
| 4   |  Scalability test (short run)|  |  | Exps - 5000, Results - 72000, execution time - 3 hours 51 mins |
| 5   |  DB Migration test |    |  | Tested on Openshift |
| 6   |  Recommendation and box plot values validations |   |  |  |
| 7   |  Kruize remote monitoring demo |  |  | Tested manually |
| 8   |  Kruize Local monitoring demo |  |  |  |
| 9   |  Kruize Local Functional tests |  TOTAL - 35, PASSED -  / FAILED -  | TOTAL - , PASSED -  / FAILED -  | [Issue 1217](https://github.com/kruize/autotune/issues/1217) |

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

