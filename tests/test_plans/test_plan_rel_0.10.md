# KRUIZE TEST PLAN RELEASE 0.10

- [INTRODUCTION](#introduction)
- [FEATURES TO BE TESTED](#features-to-be-tested)
- [BUG FIXES TO BE TESTED](#bug-fixes-to-be-tested)
- [TEST ENVIRONMENT](#test-environment)
- [TEST DELIVERABLES](#test-deliverables)
    - [New Test Cases Developed](#new-test-cases-developed)
- [RELEASE TESTING](#release-testing)
    - [RELEASE TESTS](#release-tests)
    - [RELEASE TESTS RESULTS SUMMARY](#release-tests-results-summary)
    - [KRUIZE TEST RESULTS](#kruize-test-results)
    - [SCALE TEST RESULTS](#scale-test-results)
    - [KRUIZE DEMOS RESULTS](#kruize-demos-results)
- [TEST METRICS](#test-metrics)
- [RISKS AND CONTINGENCIES](#risks-and-contingencies)
- [APPROVALS](#approvals)

-----

## INTRODUCTION

This document describes the test plan for Kruize release 0.10

----

## FEATURES TO BE TESTED

* Kruize UI pod changed to deployment
* Add support for mTLS authentication to Kruize datasource
* Add support for Kruize optimizer
* Updated dependencies & RH UBI minimal to fix vulnerabilities
* Library upgrade and refactoring code
* Test for runtime recommendations
* Kruize demos test updates for runtime and optimizer

------

## BUG FIXES TO BE TESTED

* Permission updates to fix Kruize UI deployment crash

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster

---

## TEST DELIVERABLES

### New Test Cases Developed

| # | ISSUE (NEW FEATURE)                                                        | TEST DESCRIPTION                                                       | TEST DELIVERABLES                                    | RESULTS | COMMENTS |
|---|----------------------------------------------------------------------------|------------------------------------------------------------------------|------------------------------------------------------|---------|----------|
| 1 | Unit tests added for Kruize optimizer | Added new testcases | [8](https://github.com/kruize/kruize-optimizer/pull/8) | PASSED | |
| 2 | Runtime recommendations test | Added new test for runtime recommendations | [1824](https://github.com/kruize/autotune/pull/1824) | PASSED | |
| 3 | Optimizer demo   | Tested manually | [180](https://github.com/kruize/kruize-demos/pull/180) | PASSED  | |

---

## RELEASE TESTING

### RELEASE TESTS

As part of the release testing, following tests will be executed:
- [Kruize Remote monitoring Functional tests](/tests/scripts/remote_monitoring_tests/Remote_monitoring_tests.md)
- [Fault tolerant test](/tests/scripts/remote_monitoring_tests/fault_tolerant_tests.md)
- [Stress test](/tests/scripts/remote_monitoring_tests/README.md)
- [DB Migration test](/tests/scripts/remote_monitoring_tests/db_migration_test.md)
- [Recommendation and box plot values validation test](https://github.com/kruize/kruize-demos/blob/main/monitoring/remote_monitoring_demo/recommendations_infra_demo/README.md)
- [Scalability test (On openshift)](/tests/scripts/remote_monitoring_tests/scalability_test.md) - scalability test with 5000 exps / 15 days usage data
- [Kruize remote monitoring demo](https://github.com/kruize/kruize-demos/blob/main/monitoring/remote_monitoring_demo/README.md)
- [Kruize local monitoring demo](https://github.com/kruize/kruize-demos/blob/main/monitoring/local_monitoring/ReadMe.md)
- [Kruize bulk demo](https://github.com/kruize/kruize-demos/blob/main/monitoring/local_monitoring/bulk_demo/README.md)
- [Kruize vpa demo](https://github.com/kruize/kruize-demos/blob/main/monitoring/local_monitoring/vpa_demo/README.md)
- [Kruize runtimes demo](https://github.com/kruize/kruize-demos/blob/main/monitoring/local_monitoring/runtimes_demo/README.md)
- [Kruize optimizer demo](https://github.com/kruize/kruize-demos/blob/main/monitoring/local_monitoring/optimizer_demo/README.md)
- [Kruize local monitoring Functional tests](/tests/scripts/local_monitoring_tests/Local_monitoring_tests.md)


### RELEASE TESTS RESULTS SUMMARY

All Release tests have been run against the Kruize release 0.10 image and all tests have - :
- No regressions seen, runtime recommendations introduced in this release has issues with notification code & additional logging enabled in the kruize pod log related to runtime queries [Issue 1821](https://github.com/kruize/autotune/issues/1821)

Scalability short -  


### KRUIZE TEST RESULTS

| # | TEST SUITE | OPENSHIFT RESULTS | MINIKUBE RESULTS | COMMENTS |
|:---|:---|:---|:---|:---|
| 1 | Kruize Remote monitoring Functional testsuite | TOTAL - 738, PASSED - 695 / FAILED - 42 / SKIPPED - 1 | TOTAL - 738, PASSED - 695 / FAILED - 42 / SKIPPED - 1 | Existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610) |
| 2 | Fault tolerant test | PASSED | PASSED | |
| 3 | Stress test | PASSED | PASSED | |
| 4 | Scalability test (short run) |  | NA | |
| 5 | DB Migration test | PASSED | NA | |
| 6 | Recommendation and box plot values validations | PASSED | PASSED | |
| 7 | Local Fault tolerant test | PASSED | PASSED | |
| 8 | Kruize Local Functional tests | TOTAL - 234 , PASSED - 228 / FAILED - 4 / SKIPPED - 2 | TOTAL - 234, PASSED - 227 / FAILED - 4 / SKIPPED - 3 | [Issue 1395](https://github.com/kruize/autotune/issues/1395), [Issue 1273](https://github.com/kruize/autotune/issues/1273), [Issue 1763](https://github.com/kruize/autotune/issues/1763), [Issue 1821](https://github.com/kruize/autotune/issues/1821) |

Kruize test result summary:

No regressions seen, runtime recommendations introduced in this release has issues with notification code & additional logging enabled in the kruize pod log related to runtime queries [Issue 1821](https://github.com/kruize/autotune/issues/1821)

### SCALE TEST RESULTS

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

Short Scalability run configuration:
- 5K exps / 15 days of results / 2 containers per exp
- Kruize replicas - 10
- OCP - AWS cluster
- PV/PVC storage - 1Gi
- Kruize DB resources - requests - 10Gi / 2 cores, limits - 30Gi / 2 cores
- Kruize resources - requests - 4Gi / 2 cores, limits - 8Gi / 2 cores



| Kruize Release | Exps / Results / Recos | Execution Time | Latency: UpdateRecos (Max/Avg) | Latency: UpdateResults (Max/Avg) | Latency: LoadResults (Max/Avg) | Postgres DB (MB) | Max CPU | Max Memory (GB) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **0.8.1** (14 Jan) | 5K container / 72L / 3L | 4h 39m | 0.91 / 0.5 | 0.14 / 0.1 | 0.45 / 0.31 | 21754 | 6.86 | 35.96 |
| **0.9** (24 Feb) | 5K container / 72L / 3L | 4h 31m | 0.86 / 0.47 | 0.12 / 0.09 | 0.38 / 0.26 | 21756 | 7.36 | 27.83 |
| **0.10** (16 Apr) | 5K container / 72L / 3L | 4h 34m | 0.89 / 0.48 | 0.13 / 0.1 | 0.44 / 0.3 | 21755 | 7.97 | 46.33 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **0.8.1** (15 Jan) | 5K namespace / 72L / 3L | 2h 59m | 0.54 / 0.3 | 0.1 / 0.07 | 0.3 / 0.19 | 10775 | 7.6 | 23.65 |
| **0.9** (25 Feb) | 5K namespace / 72L / 3L | 2h 58m | 0.54 / 0.3 | 0.11 / 0.08 | 0.29 / 0.19 | 10774 | 5.2 | 23.45 |
| **0.10** (16 Apr) | 5K namespace / 72L / 3L | 3h 04m | 0.55 / 0.31 | 0.12 / 0.08 | 0.33 / 0.21 | 10776 | 8.96 | 28.95 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **0.8.1** (15 Jan) | 4.5k container, 500 namespace / 72L / 3L | 4h 33m | 0.85 / 0.49 | 0.13 / 0.1 | 0.43 / 0.3 | 20648 | 7.3 | 38.14 |
| **0.9** (25 Feb) | 4.5k container, 500 namespace / 72L / 3L | 4h 33m | 0.85 / 0.49 | 0.13 / 0.1 | 0.42 / 0.3 | 20652 | 7.22 | 41.25 |
| **0.10** (16 Apr) | 4.5k container, 500 namespace / 72L / 3L | 4h 33m | 0.85 / 0.47 | 0.13 / 0.11 | 0.43 / 0.31 | 20649 | 8.5 | 44.39 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **0.8.1** (15 Jan) | 5k gpucontainer / 72L / 3L | 7h 46m | 1.58 / 0.87 | 0.22 / 0.19 | 0.74 / 0.56 | 31140 | 10.19 | 35.76 |
| **0.9** (25 Feb) | 5k gpucontainer / 72L / 3L | 7h 43m | 1.58 / 0.86 | 0.21 / 0.19 | 0.69 / 0.52 | 31143 | 10.94 | 34.08 |
| **0.10** (16 Apr) | 5k gpucontainer / 72L / 3L | 7h 56m | 1.63 / 0.85 | 0.21 / 0.19 | 0.76 / 0.53 | 31141 | 12.77 | 30.29 |

Here exps - Experiments, L - Lakhs


Scalability test result summary:


----

### KRUIZE DEMOS RESULTS

| # | KRUIZE DEMO | CLUSTER | OPERATOR MODE RESULTS | NON-OPERATOR MODE RESULTS | COMMENTS |
|---|:---|:---|:---|:---|:---|
| 1 | Kruize remote monitoring demo | Openshift | NA | PASSED | |
| 2 | Kruize remote monitoring demo | Minikube | NA | PASSED | |
| 3 | Kruize remote monitoring demo | Kind | NA | PASSED |  |
| 4 | Kruize local monitoring demo | Openshift |  | PASSED | |
| 5 | Kruize local monitoring demo | Minikube |  | PASSED | |
| 6 | Kruize local monitoring demo | Kind |  | PASSED | |
| 7 | Kruize bulk demo | Openshift |  | PASSED |  |
| 8 | Kruize bulk demo | Minikube |  | PASSED | |
| 9 | Kruize bulk demo | Kind |  | PASSED | |
| 10 | Kruize vpa demo | Openshift |  | PASSED | |
| 11 | Kruize vpa demo | Minikube |  | PASSED |  |
| 12 | Kruize vpa demo | Kind |  | PASSED | |
| 13 | Kruize runtimes demo | Openshift |  | PASSED | |
| 14 | Kruize runtimes demo | Minikube |  | PASSED | |
| 15 | Kruize runtimes demo | Kind |  | PASSED | |
| 16 | Kruize optimizer demo | Openshift |  |  | |
| 17 | Kruize optimizer demo | Minikube |  |  | |
| 18 | Kruize optimizer demo | Kind |  |  | |

Kruize Demos result summary:

All Demos worked fine as expected, except the below:
- When demos are run with operator & manifests one after the other, this issue is seen - [Issue 1788](https://github.com/kruize/autotune/issues/1788) 

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
