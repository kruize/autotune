# KRUIZE TEST PLAN RELEASE 0.0.20.3

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

This document describes the test plan for Kruize remote monitoring release 0.0.20.3

----

## FEATURES TO BE TESTED
* Update Term Design 

Update the Recommendations Term design to include minimum data threshold for each term and strictly enforce the term duration.
For more details refer to the github issues

* [1059](https://github.com/kruize/autotune/issues/1059)
* [1088](https://github.com/kruize/autotune/issues/1088)

* Box plots preview

Box plots is included as a preview in this release and is not enabled by default, it can be enabled by setting plots to true in the manifest file.

* [1130](https://github.com/kruize/autotune/pull/1130)


------

## BUG FIXES TO BE TESTED

* [1097](https://github.com/kruize/autotune/issues/1097) - Duration in hours should indicate the duration of results available for that term
* [1124](https://github.com/kruize/autotune/pull/1124) - Troubleshooting EE Perfprofile error and avoid Pod restart solution

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster 

---

## TEST DELIVERABLES

### New Test Cases Developed

| #   | ISSUE (NEW FEATURE) | TEST DESCRIPTION | TEST DELIVERABLES | RESULTS | COMMENTS |
| --- | --------- | ---------------- | ----------------- |  -----  | --- |
| 1   |  [Term Design changes](https://github.com/kruize/autotune/issues/1059) |       |[Tests added as part of PR](https://github.com/kruize/autotune/pull/1081) |  |  |
| 2   |  [Term Documentation](https://github.com/kruize/autotune/issues/1088) |       |[Tests added as part of PR](https://github.com/kruize/autotune/pull/1096) |  |  |

### Regression Testing

| #   | ISSUE (BUG/NEW FEATURE) |  TEST CASE | RESULTS | COMMENTS |
| --- | --------- | ---------------- | -------- | --- |
| 1   | Term Design Changes | Functional test suite | | |
| 2   | Kruize remote monitoring demo | kruize demo | Medium & long term recos were not generated |[1142](https://github.com/kruize/autotune/pull/1142)|
| 2   | Short Scalability test | 5k exps / 15 days | Postgres connections issue |[1141](https://github.com/kruize/autotune/pull/1141)|

---

## SCALABILITY TESTING

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

| #   | OBJECTIVE | INPUT | EXPECTED RESULTS |  ACTUAL RESULTS   | COMMENTS |
| --- | --------- | ----- | ---------------- | ----------------- | -------  |
| 1   |  Run a short run of scalability to check for any regressions |  5k exps / 15 days     | PASSED | PASSED |  |


Short Scalability run - 5K exps / 15 days of results / 2 containers per exp
Kruize replicas - 10
OCP - Scalelab cluster

Kruize Release | Exps / Results / Recos | Execution time | Latency (Max/ Avg) in seconds ||| Postgres DB size(MB) | Kruize Max CPU | Kruize Max Memory (GB)
-- | -- | -- | -- | -- | -- | --| -- | --
  |   |   | UpdateRecommendations | UpdateResults | LoadResultsByExpName |   |   |  
0.0.20.2_mvp | 5K / 72L | 3h 49mins | 0.61 / 0.4 | 0.25 / 0.18 | 0.34 / 0.25 | 21 (GB) | 5.5 | 37
0.0.20.3_mvp | 5K / 72L / 3L | 3h 49 mins | 0.62/ 0.39 | 0.24 / 0.17 | 0.34 / 0.25 | 21302.32 | 4.8 | 40.6
0.0.20.3_mvp (With Box plots) | 5K / 72L / 3L | 3h 50mins | 0.61 / 0.39 | 025 / 0.18 | 0.34 / 0.25 | 21855.04 | 4.7 | 35.1


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
| 1   |  Functional testsuite | TOTAL - 350, PASSED -307 / FAILED - 43  |  TOTAL - 350, PASSED -307 / FAILED - 43 | No new regressions seen, existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610)    |
| 2   |  Fault tolerant test | PASSED | PASSED | |
| 3   |  Stress test | PASSED | FAILED | Intermittent on openshift - issuse raised [1106](https://github.com/kruize/autotune/issues/1106) |
| 4   |  Scalability test | PASSED | PASSED | Exps - 5000, Results - 72000, execution time - 3 hrs 49 mins | [1148](https://github.com/kruize/autotune/issues/1148)
| 5   |  Kruize remote monitoring demo | PASSED | PASSED | Tested manually using Kruize UI |

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

