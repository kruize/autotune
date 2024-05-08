# KRUIZE TEST PLAN RELEASE 0.0.22

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

This document describes the test plan for Kruize remote monitoring release 0.0.22

----

## FEATURES TO BE TESTED

* Refactoring of Kruize logging

Enabled logging of success and failures for APIs

------

## BUG FIXES TO BE TESTED

* Update box plots metric calculation logic to include Avg, Min and Max instead of Sum
* Fix the issue when box plots were getting generated even with no recommendations
* Exclude monitoring start time to avoid generating additional plot data
* Fix issue where format appears to be blank when all metric values are zero

* [1189](https://github.com/kruize/autotune/pull/1189)

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster 

---

## TEST DELIVERABLES

### New Test Cases Developed

| #   | ISSUE (NEW FEATURE)                                                                                                                  | TEST DESCRIPTION | TEST DELIVERABLES | RESULTS | COMMENTS |
| --- |--------------------------------------------------------------------------------------------------------------------------------------| ---------------- | ----------------- |  -----  | --- |
| 1   | [Box plots validation tests] (https://github.com/kruize/autotune/issues/1116)                                                                   | Tests added - [1108](https://github.com/kruize/autotune/pull/1108) |  | |
| 2   | [Box plots values validation tests] (https://github.com/kruize/autotune/issues/1116)                                                                   | Tests added - [73](https://github.com/kruize/kruize-demos/pull/73) |  | |

### Regression Testing

| #   | ISSUE (BUG/NEW FEATURE)        |  TEST CASE | RESULTS | COMMENTS |
| --- |--------------------------------| ---------------- | -------- | --- |
| 1   | Kruize remote monitoring tests | Functional test suite | PASSED | |
| 2   | Kruize fault tolerant tests | Functional test suite | PASSED | |
| 3   | Kruize stress tests | Functional test suite | PASSED | |
| 4   | Short Scalability test         | 5k exps / 15 days | PASSED |

---

## SCALABILITY TESTING

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

Short Scalability run
- 5K exps / 15 days of results / 2 containers per exp
- Kruize replicas - 10
- OCP - Scalelab cluster

Kruize Release | Exps / Results / Recos | Execution time | Latency (Max/ Avg) in seconds ||| Postgres DB size(MB) | Kruize Max CPU | Kruize Max Memory (GB)
-- | -- | -- | -- | -- | -- | --| -- | --
  |   |   | UpdateRecommendations | UpdateResults | LoadResultsByExpName |   |   |  
0.0.21_mvp | 5K / 72L / 3L | 3h 50 mins | 0.62 / 0.39 | 0.25 / 0.17 | 0.34 / 0.25 |  21417.14  | 6.04 | 35.37
0.0.21_mvp (With Box plots) | 5K / 72L / 3L | 3h 53 mins | 0.63 / 0.39 |  0.25 / 0.17 | 0.35 / 0. 25 |  21868.5  | 4.4 | 40.71
0.0.22_mvp | |  | | | | | |
0.0.22_mvp (With Box plots) |  | | | | | | | 

Long Scalability run
- 100K exps / 15 days of results / 2 containers per exp
- Kruize replicas - 10
- OCP - AWS cluster

Kruize Release | Exps / Results / Recos | Execution time | Latency (Max/ Avg) in seconds ||| Postgres DB size(MB) | Kruize Max CPU | Kruize Max Memory (GB)
-- | -- | -- | -- | -- | -- | --| -- | --
  |   |   | UpdateRecommendations | UpdateResults | LoadResultsByExpName |   |   |  
0.0.22_mvp | |  | | | | | |
0.0.22_mvp (With Box plots) |  | | | | | | | 

----
## RELEASE TESTING

As part of the release testing, following tests will be executed:
- [Kruize Remote monitoring Functional tests](/tests/scripts/remote_monitoring_tests/Remote_monitoring_tests.md)
- Kruize Local monitoring demo
- [Fault tolerant test](/tests/scripts/remote_monitoring_tests/fault_tolerant_tests.md)
- [Stress test](/tests/scripts/remote_monitoring_tests/README.md)
- [Scalability test (On openshift)](/tests/scripts/remote_monitoring_tests/scalability_test.md) - scalability test with 5000 exps / 15 days usage data
- [Kruize remote monitoring demo (On minikube)](https://github.com/kruize/kruize-demos/blob/main/monitoring/remote_monitoring_demo/README.md)


| #   | TEST SUITE | EXPECTED RESULTS | ACTUAL RESULTS | COMMENTS |
| --- | ---------- | ---------------- | -------------- | -------- | 
| 1   |  Kruize Remote monitoring Functional testsuite | TOTAL - , PASSED -  / FAILED -  | TOTAL - , PASSED - / FAILED - | No new regressions seen, existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610) |
| 2   |  Kruize Local monitoring demo | | | |
| 3   |  Fault tolerant test | | | |
| 4   |  Stress test | | | |
| 5   |  Scalability test (short run)| | | |
| 6   |  Kruize remote monitoring demo | | | |

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

