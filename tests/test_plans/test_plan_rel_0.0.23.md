# KRUIZE TEST PLAN RELEASE 0.0.23

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

This document describes the test plan for Kruize remote monitoring release 0.0.23

----

## FEATURES TO BE TESTED

* Add metrics logging for Kruize recommendation notifications 
* Update the scalability test to print the metrics summary

Kruize Local:

* Update the kruize-demos to include different workload conditions
* Included tests for list datasource metadata and multiple import metadata
* Support for ‘kind’ instead of minikube to run kruize local monitoring demo


------

## BUG FIXES TO BE TESTED

* Kruize pod fails if postgres doesn't comeup
* Analyze updateRecommendation failures in production
* Box plots - long_term : duration_in_hours - Limit to 2 decimal places similar to Short _term and mid_term
* Cleaned up recommendation engine code to remove redundant constants
* Security Vulnerability fixes

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster 

---

## TEST DELIVERABLES

### New Test Cases Developed

| #   | ISSUE (NEW FEATURE)                                                   | TEST DESCRIPTION | TEST DELIVERABLES | RESULTS | COMMENTS |
| --- |-----------------------------------------------------------------------| ---------------- | ----------------- |  -----  | --- |
| 1   | [Add metrics logging for Kruize recommendation notifications](https://github.com/kruize/autotune/pull/1206) | Updated scalability test to capture the notifications metrics |  | PASSED | |
| 2   | [Support multiple invocations of import metadata](https://github.com/kruize/autotune/pull/1178) | [Included new tests](https://github.com/kruize/autotune/blob/master/tests/scripts/local_monitoring_tests/Local_monitoring_tests.md) | Tests added - [1211](https://github.com/kruize/autotune/pull/1211) | PASSED | |
| 3   | Tests for list datasource metadata | [Included new tests](https://github.com/kruize/autotune/blob/master/tests/scripts/local_monitoring_tests/Local_monitoring_tests.md) | Tests added - [1199](https://github.com/kruize/autotune/pull/1199) | PASSED | |
| 4   | [Update the kruize-demos to include different workload conditions](https://github.com/kruize/kruize-demos/pull/79) | Different load conditions idle/over utilized/under utilized were simulated | | PASSED | |
| 5   | [Support for ‘kind’ instead of minikube to run kruize local demo](https://github.com/kruize/kruize-demos/pull/81) | local monitoring demo has been updated to use kind | | CPU recommendations are not generated, being investigated | |
| 6   | Update the scalability test to print the metrics summary | Existing Scale test has been updated to print the summary | [81](https://github.com/kruize/autotune/pull/81) | PASSED | |

### Regression Testing

| #   | ISSUE (BUG/NEW FEATURE)        |  TEST CASE | RESULTS | COMMENTS |
| --- |--------------------------------| ---------- |---------| --- |
| 1   | Kruize pod fails if postgres doesn't comeup | Kruize remote monitoring tests | PASSED | | 
| 2   | Box plots - long_term : duration_in_hours - Limit to 2 decimal places similar to Short _term and mid_term | Updated existing tests | PASSED | | 

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
0.0.22_mvp | 5K / 72L / 3L | 3h 51 mins            | 0.62 / 0.39      | 0.24 / 0.17  | 0.34 / 0.25  | 21756.32     | 7.12           | 33.64
0.0.23_mvp | 5K / 72L / 3L | 3h 51 mins | 0.63 / 0.39 | 0.24 / 0.17 | 0.35 / 0.25 |  21760 | 4.52 | 32.59

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
| 1   |  Kruize Remote monitoring Functional testsuite | TOTAL - 357, PASSED - 314 / FAILED - 43 | TOTAL - 357, PASSED - 314/ FAILED - 43 | No new regressions seen, existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610) |
| 2   |  Fault tolerant test | PASSED | PASSED |  |
| 3   |  Stress test | PASSED   | PASSED |  |
| 4   |  Scalability test (short run)| PASSED | PASSED | Exps - 5000, Results - 72000, execution time - 3 hours 51 mins |
| 5   |  DB Migration test | PASSED   | PASSED | Tested on Openshift |
| 6   |  Recommendation and box plot values validations | PASSED  | PASSED |  |
| 7   |  Kruize remote monitoring demo | PASSED | PASSED | Tested manually |
| 8   |  Kruize Local monitoring demo | PASSED | PASSED |  |
| 9   |  Kruize Local Functional tests |  TOTAL - 35, PASSED - 35 / FAILED - 0 | TOTAL - 35, PASSED - 33 / FAILED - 2 | [Issue 1217](https://github.com/kruize/autotune/issues/1217) |

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

