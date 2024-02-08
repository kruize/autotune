# TEST PLAN TITLE

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

A short concise project description along with any references to the project plan 

----

## FEATURES TO BE TESTED
Desription of new features that needs to be tested along with any references

------
## BUG FIXES TO BE TESTED
Desription of bug fixes that needs to be tested along with any references

---
## TEST ENVIRONMENT

Are there any special requirements for this test plan, such as:

* Hardware configuration

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

Add references to test deliverables

### New Test Cases Developed

| #   | ISSUE (NEW FEATURE) | TEST DESCRIPTION | TEST DELIVERABLES | RESULTS | COMMENTS |
| --- | --------- | ---------------- | ----------------- |  -----     | --- |
| 1   |           |       |                  |           |        |


### Regression Testing

| #   | ISSUE (BUG/NEW FEATURE) |  TEST SUITE | RESULTS | COMMENTS |
| --- | --------- | ---------------- | -------- | --- |
| 1   |           |                  |          |     |

---
## SCALABILITY TESTING

### Scalability Test Strategy

Evaluate Kruize Scalability on OCP, with 5k and 100k experiments and with the resource usage data for 15 days

### Scalability Test Cases

| #   | OBJECTIVE | INPUT | EXPECTED RESULTS |  ACTUAL RESULTS   | COMMENTS |
| --- | --------- | ----- | ---------------- | ----------------- | -------  |
| 1   |           |       |                  |                   |          |

----
## RELEASE TESTING

### Release Testing

As part of the release testing, test the following will be executed:
- [Functional tests](/tests/scripts/remote_monitoring_tests/Remote_monitoring_tests.md)
- [Fault tolerant test](/tests/scripts/remote_monitoring_tests/fault_tolerant_tests.md)
- [Stress test](/tests/scripts/remote_monitoring_tests/README.md)
- [Scalability test (On openshift)](/tests/scripts/remote_monitoring_tests/scalability_test.md) - scalability test with 5000 exps / 15 days usage data


| #   | TEST SUITE | EXPECTED RESULTS | ACTUAL RESULTS | COMMENTS |
| --- | --------- | ---------------- | -------------- | ---- |
| 1   |           |                  |                |      |

---

## TEST METRICS

### Test Completion Criteria

Define the test completion or exit criteria

* All must_fix defects identified for the release are fixed
* New features work as expected and tests have been added to validate these
* No new regressions in the functional tests
* 98% functional tests have passed
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

