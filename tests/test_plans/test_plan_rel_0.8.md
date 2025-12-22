# KRUIZE TEST PLAN RELEASE 0.8

- [INTRODUCTION](#introduction)
- [FEATURES TO BE TESTED](#features-to-be-tested)
- [BUG FIXES TO BE TESTED](#bug-fixes-to-be-tested)
- [TEST ENVIRONMENT](#test-environment)
- [TEST DELIVERABLES](#test-deliverables)
    - [New Test Cases Developed](#new-test-cases-developed)
    - [Regression Testing](#regression-testing)
- [SCALABILITY TESTING](#scalability-testing)
- [RELEASE TESTING](#release-testing)
- [TEST METRICS](#test-metrics)
- [RISKS AND CONTINGENCIES](#risks-and-contingencies)
- [APPROVALS](#approvals)

-----

## INTRODUCTION

This document describes the test plan for Kruize release 0.8

----

## FEATURES TO BE TESTED

* Add new GPU profiles for H200, B200 and Blackwell cards
* Update Accelerator notifications
* Test updates for Performance Profile API
* Upgrade Java to 25 and microdnf updates in Dockerfile

------

## BUG FIXES TO BE TESTED

* Fix vulnerabilities reported by upgrading to latest ubi & netty packages
* Validations in Bulk API for datasource and time ranges
* Performance profile failures
* Demo updates to fix openshift kruize URLs, BSD support in mac and logging issues

---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster

---

## TEST DELIVERABLES

### New Test Cases Developed

| # | ISSUE (NEW FEATURE)                                                        | TEST DESCRIPTION                                                       | TEST DELIVERABLES                                    | RESULTS | COMMENTS |
|---|----------------------------------------------------------------------------|------------------------------------------------------------------------|------------------------------------------------------|---------|----------|
| 1 | Add new GPU profiles for H200, B200 and Blackwell cards | Added new testcases | [1717](https://github.com/kruize/autotune/pull/1717) | PASSED  |          |
| 2 | Update Accelerator notifications | Updates to existing testcases | [1717](https://github.com/kruize/autotune/pull/1717) | PASSED  |          |

### Regression Testing

| # | ISSUE (NEW FEATURE)                                                             | TEST DESCRIPTION                                                                      | TEST DELIVERABLES                                    | RESULTS | COMMENTS |
|---|---------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|------------------------------------------------------|---------|----------|
| 1 | Fix vulnerabilities reported by upgrading to latest ubi & netty packages | Tested manually                                                                       |                                                      | PASSED  |          |
| 2 | Validations in Bulk API for datasource and time ranges   | New functional tests added |  | PASSED  |  |
| 3 | Demo updates to fix openshift kruize URLs, BSD support in mac and logging issues   | Tested manually |  | PASSED  |  |

---

## SCALABILITY TESTING

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

Short Scalability run
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
| **0.7.1-rc2** (27 Oct) | 5K namespace / 72L / 3L | 2h 54m | 0.52 / 0.29 | 0.1 / 0.08 | 0.28 / 0.18 | 10523 | 5.52 | 25.6 |
| **0.8** (19 Dec) | 5K namespace / 72L / 3L | 2h 57m | 0.54 / 0.3 | 0.1 / 0.07 | 0.29 / 0.19 | 10520 | 6.41 | 23.6 |
| **0.7.1-rc2** (28 Oct) | 4.5k cont, 500 ns / 72L / 3L | 4h 20m | 0.81 / 0.46 | 0.13 / 0.1 | 0.38 / 0.26 | 20628 | 6.87 | 40.0 |
| **0.8** (19 Dec) | 4.5k cont, 500 ns / 72L / 3L | 4h 20m | 0.84 / 0.48 | 0.13 / 0.1 | 0.42 / 0.29 | 20627 | 6.31 | 43.0 |
| **0.8** (19 Dec) | 5k gpucontainer / 72L / 3L | 7h 40m | 1.56 / 0.86 | 0.21 / 0.19 | 0.7 / 0.53 | 31140 | 10.68 | 30.97 |



----
## RELEASE TESTING

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

| # | TEST SUITE | EXPECTED RESULTS | ACTUAL RESULTS | COMMENTS |
|:---|:---|:---|:---|:---|
| 1 | Kruize Remote monitoring Functional testsuite | TOTAL - 738, PASSED - 695 / FAILED - 42 / SKIPPED - 1 | TOTAL - 738, PASSED - 695 / FAILED - 42 / SKIPPED - 1 | Existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610), [1668](https://github.com/kruize/autotune/issues/1668) |
| 2 | Fault tolerant test | PASSED | PASSED | |
| 3 | Stress test | PASSED | PASSED | |
| 4 | Scalability test (short run) | PASSED | PASSED | Observed an increase in Kruize max memory for container_ns exps by 3GB |
| 5 | DB Migration test | PASSED | PASSED | |
| 6 | Recommendation and box plot values validations | PASSED | PASSED | |
| 7 | Local Fault tolerant test | PASSED | PASSED | |
| 8 | Kruize Local Functional tests | TOTAL - 157 , PASSED - 154 / FAILED - 2 / SKIPPED - 1 | Openshift - TOTAL - 157 , PASSED - 154 / FAILED - 2 / SKIPPED - 1, Minikube - TOTAL - 157 , PASSED - 149 / FAILED - 7 / SKIPPED - 1| [Issue 1395](https://github.com/kruize/autotune/issues/1395), [Issue 1273](https://github.com/kruize/autotune/issues/1273), [Issue 1734](https://github.com/kruize/autotune/issues/1734) |


Kruize Demo results

| # | KRUIZE DEMO | CLUSTER | OPERATOR MODE RESULTS | NON-OPERATOR MODE RESULTS | COMMENTS |
|---|:---|:---|:---|:---|:---|
| 1 | Kruize remote monitoring demo | Openshift | NA | PASSED | |
| 2 | Kruize remote monitoring demo | Minikube | NA | PASSED | |
| 3 | Kruize remote monitoring demo | Kind | NA | NOT TESTED| kind support in progress [PR 161](https://github.com/kruize/kruize-demos/pull/161) |
| 4 | Kruize local monitoring demo | Openshift | FAILED | PASSED | [Issue 162](https://github.com/kruize/kruize-demos/issues/162) |
| 5 | Kruize local monitoring demo | Minikube | FAILED | FAILED | [Issue 127](https://github.com/kruize/kruize-demos/issues/127) |
| 6 | Kruize local monitoring demo | Kind | FAILED | PASSED | Failed as kind/minikube support is not available in 0.0.2 kruize operator |
| 7 | Kruize bulk monitoring demo | Openshift | FAILED  | PASSED | |
| 8 | Kruize bulk monitoring demo | Minikube | FAILED  | PASSED | |
| 9 | Kruize bulk monitoring demo | Kind | FAILED | PASSED | |
| 10 | Kruize vpa monitoring demo | Openshift | FAILED | PASSED | |
| 11 | Kruize vpa monitoring demo | Minikube | FAILED | PASSED |  |
| 12 | Kruize vpa monitoring demo | Kind | FAILED | PASSED |  |


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
