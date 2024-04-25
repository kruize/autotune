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
 using the metric results from the specified datasource. Refer [doc]() for further details.

* Kruize Security vulnerability issues

  Security Vulnerabilities in the Kruize dependencies have been fixed through the below issues:


  * [1150](https://github.com/kruize/autotune/pull/1150)
  * [1153](https://github.com/kruize/autotune/pull/1153)


* Kruize logging using CloudWatch

Send kruize logs to CloudWatch so that these logs can be viewed using tools like kibana to debug issues


------

## BUG FIXES TO BE TESTED

* [1156](https://github.com/kruize/autotune/pull/1156) - Notification is not displayed when the CPU usage is less than a millicore
* [1165](https://github.com/kruize/autotune/pull/1165) - Fix the missing validation for Update recommendation API

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster 

---

## TEST DELIVERABLES

### New Test Cases Developed

| #   | ISSUE (NEW FEATURE)                                                                                                                  | TEST DESCRIPTION | TEST DELIVERABLES | RESULTS | COMMENTS |
| --- |--------------------------------------------------------------------------------------------------------------------------------------| ---------------- | ----------------- |  -----  | --- |
| 1   | [Kruize local changes](https://github.com/kruize/autotune/issues/)                                                                   | Test scenarios identified - [1134](https://github.com/kruize/autotune/issues/1134), [1129](https://github.com/kruize/autotune/issues/1129), [1160](https://github.com/kruize/autotune/issues/1160) |Kruize local is PoC, tests will be implemented while productizing | Kruize local workflow tested manually | PASSED on Openshift | Debugging generate recommendations issue on minikube
| 2   | [Kruize CloudWatch logging](https://github.com/kruize/autotune/pull/1173)                                                              | Kruize logging to CloudWatch is tested by using a CloudWatch in AWS cluster manually | Manual test | PASSED |  |
| 3   | [Notifications are not displayed when the CPU usage is less than a millicore or zero](https://github.com/kruize/autotune/pull/1156) | Kruize Functiona testsuite will be updated to post results with cpu usage of less than millicore or zero to validate these notifications | Functional tests included in the same PR | PASSED |  |

### Regression Testing

| #   | ISSUE (BUG/NEW FEATURE)        |  TEST CASE | RESULTS | COMMENTS |
| --- |--------------------------------| ---------------- | -------- | --- |
| 1   | Kruize remote monitoring tests | Functional test suite | PASSED | |
| 1   | Kruize fault tolerant tests | Functional test suite | PASSED | |
| 1   | Kruize stress tests | Functional test suite | PASSED | |
| 2   | Kruize local monitoring demo   | kruize demo | Tested it manually | Authentication failure on Openshift, recommendations issue on minikube
| 3   | Short Scalability test         | 5k exps / 15 days | PASSED |

---

## SCALABILITY TESTING

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

Short Scalability run - 5K exps / 15 days of results / 2 containers per exp
Kruize replicas - 10
OCP - Scalelab cluster

Kruize Release | Exps / Results / Recos | Execution time | Latency (Max/ Avg) in seconds ||| Postgres DB size(MB) | Kruize Max CPU | Kruize Max Memory (GB)
-- | -- | -- | -- | -- | -- | --| -- | --
  |   |   | UpdateRecommendations | UpdateResults | LoadResultsByExpName |   |   |  
0.0.20.3_mvp | 5K / 72L / 3L | 3h 49 mins | 0.62 / 0.39 | 0.24 / 0.17 | 0.34 / 0.25 | 21302.32 | 4.8 | 40.6
0.0.20.3_mvp (With Box plots) | 5K / 72L / 3L | 3h 50mins | 0.61 / 0.39 | 025 / 0.18 | 0.34 / 0.25 | 21855.04 | 4.7 | 35.1
0.0.21_mvp | 5K / 72L / 3L | 3h 50 mins | 0.62 / 0.39 | 0.25 / 0.17 | 0.34 / 0.25 |  21417.14  | 6.04 | 35.37
0.0.21_mvp (With Box plots) | 5K / 72L / 3L | 3h 53 mins | 0.63 / 0.39 |  0.25 / 0.17 | 0.35 / 0. 25 |  21868.5  | 4.4 | 40.71

----
## RELEASE TESTING

As part of the release testing, test the following will be executed:
- [Kruize Remote monitoring Functional tests](/tests/scripts/remote_monitoring_tests/Remote_monitoring_tests.md)
- [Kruize Local monitoring workflow](Tested manually)
- [Fault tolerant test](/tests/scripts/remote_monitoring_tests/fault_tolerant_tests.md)
- [Stress test](/tests/scripts/remote_monitoring_tests/README.md)
- [Scalability test (On openshift)](/tests/scripts/remote_monitoring_tests/scalability_test.md) - scalability test with 5000 exps / 15 days usage data
- [Kruize remote monitoring demo (On minikube)](https://github.com/kruize/kruize-demos/blob/main/remote_monitoring_demo/README.md)


| #   | TEST SUITE | EXPECTED RESULTS | ACTUAL RESULTS | COMMENTS |
| --- | ---------- | ---------------- | -------------- | -------- | 
| 1   |  Kruize Remote monitoring Functional testsuite | TOTAL - 356, PASSED - 313 / FAILED - 43 | TOTAL - 356, PASSED - 313 / FAILED - 43 | No new regressions seen, existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610) |
| 2   |  Kruize Local monitoring workflow | PASSED | PASSED on Openshift | PoC code, tested it manually |
| 3   |  Fault tolerant test | PASSED | PASSED | |
| 4   |  Stress test | PASSED | FAILED | |
| 5   |  Scalability test (short run)| PASSED | PASSED | |
| 6   |  Kruize remote monitoring demo | PASSED | PASSED | |

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

