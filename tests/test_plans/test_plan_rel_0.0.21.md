# KRUIZE TEST PLAN RELEASE 0.0.21

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

This document describes the test plan for Kruize remote monitoring release 0.0.21

----

## FEATURES TO BE TESTED

* Kruize local changes

Kruize local changes have been included in this release which allows a user to add datasources, import datasource metadata, create an experiment and generate recommendations
 from using the metric results from the specified datasource

* Kruize Security vulnerability issues

* [1150](https://github.com/kruize/autotune/pull/1150)
* [1153](https://github.com/kruize/autotune/pull/1153)

* Kruize logging using CloudWatch

Send kruize logs to CloudWatch so that these logs can be viewed using tools like kibana to debug issues

* Recommendation code refactoring

------

## BUG FIXES TO BE TESTED


---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster 

---

## TEST DELIVERABLES

### New Test Cases Developed

| #   | ISSUE (NEW FEATURE) | TEST DESCRIPTION | TEST DELIVERABLES | RESULTS | COMMENTS |
| --- | --------- | ---------------- | ----------------- |  -----  | --- |
| 1   |  [Kruize local changes](https://github.com/kruize/autotune/issues/1059) |       | |  |  |

### Regression Testing

| #   | ISSUE (BUG/NEW FEATURE) |  TEST CASE | RESULTS | COMMENTS |
| --- | --------- | ---------------- | -------- | --- |
| 1   | Term Design Changes | Functional test suite | | |
| 2   | Kruize remote monitoring demo | kruize demo | |
| 2   | Short Scalability test | 5k exps / 15 days | |

---

## SCALABILITY TESTING

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

| #   | OBJECTIVE | INPUT | EXPECTED RESULTS |  ACTUAL RESULTS   | COMMENTS |
| --- | --------- | ----- | ---------------- | ----------------- | -------  |
| 1   |  Run a short run of scalability to check for any regressions |  5k exps / 15 days     | | |  |


Short Scalability run - 5K exps / 15 days of results / 2 containers per exp
Kruize replicas - 10
OCP - Scalelab cluster

Kruize Release | Exps / Results / Recos | Execution time | Latency (Max/ Avg) in seconds ||| Postgres DB size(MB) | Kruize Max CPU | Kruize Max Memory (GB)
-- | -- | -- | -- | -- | -- | --| -- | --
  |   |   | UpdateRecommendations | UpdateResults | LoadResultsByExpName |   |   |  
0.0.20.3_mvp | 5K / 72L / 3L | 3h 49 mins | 0.62/ 0.39 | 0.24 / 0.17 | 0.34 / 0.25 | 21302.32 | 4.8 | 40.6
0.0.20.3_mvp (With Box plots) | 5K / 72L / 3L | 3h 50mins | 0.61 / 0.39 | 025 / 0.18 | 0.34 / 0.25 | 21855.04 | 4.7 | 35.1
0.0.21_mvp | 5K / 72L | | | | | | |


----
## RELEASE TESTING

As part of the release testing, test the following will be executed:
- [Functional tests](/tests/scripts/remote_monitoring_tests/Remote_monitoring_tests.md)
- [Fault tolerant test](/tests/scripts/remote_monitoring_tests/fault_tolerant_tests.md)
- [Stress test](/tests/scripts/remote_monitoring_tests/README.md)
- [Scalability test (On openshift)](/tests/scripts/remote_monitoring_tests/scalability_test.md) - scalability test with 5000 exps / 15 days usage data
- [Kruize remote monitoring demo (On minikube)](https://github.com/kruize/kruize-demos/blob/main/remote_monitoring_demo/README.md)


| #   | TEST SUITE | EXPECTED RESULTS | ACTUAL RESULTS | COMMENTS |
| --- | ---------- | ---------------- | -------------- | -------- | 
| 1   |  Functional testsuite |  |  | |
| 2   |  Fault tolerant test | | | |
| 3   |  Stress test | | | |
| 4   |  Scalability test | | | |
| 5   |  Kruize remote monitoring demo | | | |

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

