# KRUIZE TEST PLAN RELEASE 0.8.1

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

This document describes the test plan for Kruize release 0.8.1

----

## FEATURES TO BE TESTED

* Add support for namespace box plots
* Test updates to validate box plots
* Refactor datasource code
* Cleanup testsuite to remove tests for deprecated APIs
* Kruize autotune documentation update
* Demo updates to enable kruize operator support for kind/minikube
* Deploy script update to support kind

------

## BUG FIXES TO BE TESTED

* Fix issues with bulk filter options

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster

---

## TEST DELIVERABLES

### New Test Cases Developed

| # | ISSUE (NEW FEATURE)                                                        | TEST DESCRIPTION                                                       | TEST DELIVERABLES                                    | RESULTS | COMMENTS |
|---|----------------------------------------------------------------------------|------------------------------------------------------------------------|------------------------------------------------------|---------|----------|
| 1 | Fix issues with bulk filter options | Added new testcases | [1733](https://github.com/kruize/autotune/pull/1733) | PASSED | |
| 2 | Namespace box plots support | Updated existing tests to test namespace box plots | [1746](https://github.com/kruize/autotune/pull/1746) | PASSED | |
| 3 | Demo updates to support Kruize operator on kind / minikube  | Tested manually | [166](https://github.com/kruize/kruize-demos/pull/166) | PASSED  | |

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
- [Kruize local monitoring Functional tests](/tests/scripts/local_monitoring_tests/Local_monitoring_tests.md)


### RELEASE TESTS RESULTS SUMMARY

All Release tests have been run against the Kruize release 0.8.1 image and all tests have PASSED except the below:
- Bulk demo failed on Openshift (REGRESSION) - [Issue 1759](https://github.com/kruize/autotune/issues/1759) 
- VPA Demo with Operator on Kind (NEW issue, supported added in 0.0.3 operator release), [issue](https://github.com/kruize/kruize-demos/issues/171) raised


### KRUIZE TEST RESULTS

| # | TEST SUITE | OPENSHIFT RESULTS | MINIKUBE RESULTS | COMMENTS |
|:---|:---|:---|:---|:---|
| 1 | Kruize Remote monitoring Functional testsuite | TOTAL - 738, PASSED - 695 / FAILED - 42 / SKIPPED - 1 | TOTAL - 738, PASSED - 695 / FAILED - 42 / SKIPPED - 1 | Existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610) |
| 2 | Fault tolerant test | PASSED | PASSED | |
| 3 | Stress test | PASSED | PASSED | |
| 4 | Scalability test (short run) | PASSED | NA | |
| 5 | DB Migration test | PASSED | NA | |
| 6 | Recommendation and box plot values validations | PASSED | PASSED | |
| 7 | Local Fault tolerant test | PASSED | PASSED | |
| 8 | Kruize Local Functional tests | TOTAL - 167 , PASSED - 163 / FAILED - 3 / SKIPPED - 1 | TOTAL - 167, PASSED - 163 / FAILED - 3 / SKIPPED - 1 | [Issue 1395](https://github.com/kruize/autotune/issues/1395), [Issue 1273](https://github.com/kruize/autotune/issues/1273), [Issue 1763](https://github.com/kruize/autotune/issues/1763) |

Kruize test result summary:

No new issues seen except one issue which is related to test case and not a bug [Issue 1763](https://github.com/kruize/autotune/issues/1763)

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
|:---|:---|:---|:---|:---|:---|:---|:---|:---|
| **0.7.1-rc2** (28 Oct) | 5K container / 72L / 3L | 4h 38m | 0.9 / 0.5 | 0.14 / 0.11 | 0.45 / 0.29 | 21757 | 7.66 | 39.6 |
| **0.8** (18 Dec) | 5K container / 72L / 3L | 4h 32m | 0.87 / 0.47 | 0.12 / 0.09 | 0.4 / 0.27 | 21754 | 7.28 | 30.22 |
| **0.8.1** (14 Jan) | 5K container / 72L / 3L | 4h 39m | 0.91 / 0.5 | 0.14 / 0.1 | 0.45 / 0.31 | 21754 | 6.86 | 35.96 |
| **0.7.1-rc2** (27 Oct) | 5K namespace / 72L / 3L | 2h 54m | 0.52 / 0.29 | 0.1 / 0.08 | 0.28 / 0.18 | 10523 | 5.52 | 25.6 |
| **0.8** (19 Dec) | 5K namespace / 72L / 3L | 2h 57m | 0.54 / 0.3 | 0.1 / 0.07 | 0.29 / 0.19 | 10520 | 6.41 | 23.6 |
| **0.8.1** (15 Jan) | 5K namespace / 72L / 3L | 2h 59m | 0.54 / 0.3 | 0.1 / 0.07 | 0.3 / 0.19 | 10775 | 7.6 | 23.65 |
| **0.7.1-rc2** (22 Dec - run2) | 4.5k cont, 500 ns / 72L / 3L | 4h 31m | 0.84 / 0.48 | 0.14 / 0.11 | 0.41 / 0.28 | 20628 | 7.17 | 40.81 |
| **0.8** (22 Dec - run2) | 4.5k cont, 500 ns / 72L / 3L | 4h 37m | 0.86 / 0.49 | 0.14 / 0.11 | 0.43 / 0.31 | 20627 | 6.54 | 42.57 |
| **0.8.1** (15 Jan) | 4.5k cont, 500 ns / 72L / 3L | 4h 33m | 0.85 / 0.49 | 0.13 / 0.1 | 0.43 / 0.3 | 20648 | 7.3 | 38.14 |
| **0.8** (19 Dec) | 5k gpucontainer / 72L / 3L | 7h 40m | 1.56 / 0.86 | 0.21 / 0.19 | 0.7 / 0.53 | 31140 | 10.68 | 30.97 |
| **0.8.1** (15 Jan) | 5k gpucontainer / 72L / 3L | 7h 46m | 1.58 / 0.87 | 0.22 / 0.19 | 0.74 / 0.56 | 31140 | 10.19 | 35.76 |

Scalability test result summary:

Scalability short run worked fine. No regressions seen in scale test latencies, execution time when compared to the previous release

----

### KRUIZE DEMOS RESULTS

| # | KRUIZE DEMO | CLUSTER | OPERATOR MODE RESULTS | NON-OPERATOR MODE RESULTS | COMMENTS |
|---|:---|:---|:---|:---|:---|
| 1 | Kruize remote monitoring demo | Openshift | NA | PASSED  | |
| 2 | Kruize remote monitoring demo | Minikube | NA | PASSED | |
| 3 | Kruize remote monitoring demo | Kind | NA | PASSED | Tested using [PR 161](https://github.com/kruize/kruize-demos/pull/161)|
| 4 | Kruize local monitoring demo | Openshift | PASSED | PASSED | |
| 5 | Kruize local monitoring demo | Minikube | PASSED | PASSED | |
| 6 | Kruize local monitoring demo | Kind | PASSED | PASSED | |
| 7 | Kruize bulk monitoring demo | Openshift | NA | FAILED | Kruize operator 0.0.3 does not support Bulk, [Issue 1759](https://github.com/kruize/autotune/issues/1759) |
| 8 | Kruize bulk monitoring demo | Minikube | NA | PASSED |  Kruize operator 0.0.3 does not support Bulk |
| 9 | Kruize bulk monitoring demo | Kind | NA | PASSED | Kruize operator 0.0.3 does not support Bulk |
| 10 | Kruize vpa monitoring demo | Openshift | PASSED | PASSED | |
| 11 | Kruize vpa monitoring demo | Minikube | FAILED | PASSED | [Issue 171](https://github.com/kruize/kruize-demos/issues/171) |
| 12 | Kruize vpa monitoring demo | Kind | FAILED | PASSED | [Issue 171](https://github.com/kruize/kruize-demos/issues/171) |

Kruize Demos result summary:

All Demos worked fine as expected, except the below:
- Bulk demo failed on Openshift - [Issue 1759](https://github.com/kruize/autotune/issues/1759) 
- VPA demo failed on Kind / Minikube - [Issue 171](https://github.com/kruize/kruize-demos/issues/171)

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
