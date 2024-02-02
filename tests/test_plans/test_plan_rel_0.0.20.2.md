# KRUIZE TEST PLAN RELEASE 0.0.20.2

- [INTRODUCTION](#introduction)
- [FEATURES TO BE TESTED](#features-to-be-tested)
- [BUG FIXES TO BE TESTED](#bug-fixes-to-be-tested)
- [TEST ENVIRONMENT](#test-environment)
- [TEST SCHEDULE](#test-schedule)
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

This document describes the test plan for Kruize remote monitoring release 0.0.20.2 

----

## FEATURES TO BE TESTED
* Update Term Design 

Update the Recommendations Term design to include minimum data threshold for each term and strictly enforce the term duration.
For more details refer the github issue [1059](https://github.com/kruize/autotune/issues/1059)

* Delete Partitions

This feature addressed the filling up of the Kruize database. As part of the database cleanup, older partitions that exceed the specified threshold days needs to be deleted. For more details refer the github issue [1084](https://github.com/kruize/autotune/pull/1084)

------
## BUG FIXES TO BE TESTED
* [1065](https://github.com/kruize/autotune/issues/1065) - CreateExperiment failed after increase in DB size
* [561](https://github.com/kruize/autotune/issues/561) - Add validations for the UpdateResultsAPI objects
* [908](https://github.com/kruize/autotune/issues/908) - Fix the NPE issue in ListExperimentsAPI

---
## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster 

---
## TEST SCHEDULE

### Scalability Testing

Start Date (The date when testing begins)

End Date (The deadline)

### Release Testing

Start Date (The date when testing begins)

End Date (The deadline)


---

## TEST DELIVERABLES

### New Test Cases Developed

| #   | ISSUE (NEW FEATURE) | TEST DESCRIPTION | TEST DELIVERABLES | RESULTS | COMMENTS |
| --- | --------- | ---------------- | ----------------- |  -----  | --- |
| 1   |  [Term Design changes](https://github.com/kruize/autotune/issues/1059)         |       |[Tests added as part of PR](https://github.com/kruize/autotune/pull/1081)                  |           |        |
| 2   |  [DeletePartitions](https://github.com/kruize/autotune/issues/1084)         | This has been tested manually |                  |           |        |

### Regression Testing

| #   | ISSUE (BUG/NEW FEATURE) |  TEST CASE | RESULTS | COMMENTS |
| --- | --------- | ---------------- | -------- | --- |
| 1   |  [1065](https://github.com/kruize/autotune/issues/1065)  | Tested Manually | PASSED | | 
| 2   |  [561](https://github.com/kruize/autotune/issues/561)  | Functional testsuite  | PASSED | |
| 1   |  [908](https://github.com/kruize/autotune/issues/908)  | Fault tolerant test | PASSED | |

---
## SCALABILITY TESTING

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

| #   | OBJECTIVE | INPUT | EXPECTED RESULTS |  ACTUAL RESULTS   | COMMENTS |
| --- | --------- | ----- | ---------------- | ----------------- | -------  |
| 1   |  Run a short run of scalability to check for any regressions         |  5k exps / 15 days     |                  |                   |          |

----
## RELEASE TESTING

As part of the release testing, test the following will be executed:
- [Functional tests](/tests/scripts/remote_monitoring_tests/Remote_monitoring_tests.md)
- [Fault tolerant test](/tests/scripts/remote_monitoring_tests/fault_tolerant_tests.md)
- [Stress test](/tests/scripts/remote_monitoring_tests/README.md)
- [Scalability test (On openshift)](/tests/scripts/remote_monitoring_tests/scalability_test.md) - scalability test with 5000 exps / 15 days usage data

| #   | TEST SUITE | EXPECTED RESULTS | ACTUAL RESULTS | COMMENTS |
| --- | --------- | ---------------- | -------------- | ---- | 
| 1   |  Functional testsuite         |                  |                |      |
| 2   |  Fault tolerant test         |                  |                |      |
| 3   |  Stress test         |                  |                |      |
| 4   |  Scalability test         |                  |                |      |


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

* Lack of availability of required hardware or software
* Delay in delivery of the software
* Requirements or design changes

----
## APPROVALS

Sign-off

----

