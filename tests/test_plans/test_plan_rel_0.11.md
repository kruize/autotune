# KRUIZE TEST PLAN RELEASE 0.11

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

This document describes the test plan for Kruize release 0.11

----

## FEATURES TO BE TESTED

* PodCount feature support for current configuration
* Test updates to run with both new and old recommendation APIs
* Layers API update and delete support along with test updates
* Ignore DeploymentConfig workloads for kruize monitoring
* Test schema optimization updates


------

## BUG FIXES TO BE TESTED

* UBI version update & CVE fixes
* Dependabot workflow update to raise PRs against mvp_demo


---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster

---

## TEST DELIVERABLES

### New Test Cases Developed

| # | ISSUE (NEW FEATURE)                                                        | TEST DESCRIPTION                                                       | TEST DELIVERABLES                                    | RESULTS | COMMENTS |
|---|----------------------------------------------------------------------------|------------------------------------------------------------------------|------------------------------------------------------|---------|----------|
| 1 | Tests updated to run with legacy & v1 recommendations API | Updated existing testcases | [1999](https://github.com/kruize/autotune/pull/1999), [2003](https://github.com/kruize/autotune/pull/2003), [2004](https://github.com/kruize/autotune/pull/2004)  | PASSED | |
| 2 | Test updates to optimize jsonschema | Update existing tests | [1965](https://github.com/kruize/autotune/pull/1965) | PASSED | |
| 3 | Recommendation validation infra update to run with legacy & v1 recommendations API  | Update existing tests | [188](https://github.com/kruize/kruize-demos/pull/188) | PASSED  | |
| 4 | Layers test updates   | Added new tests | [1831](https://github.com/kruize/autotune/pull/1831) | PASSED  |  |


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

All Release tests have been run against the Kruize release 0.11 image and all tests have passed

Scalability short test - We observed an increase in CPU consumption in Kruize v0.11 with v1 API when compared with v0.11 (legacy API) and v0.10 release only for container experiments. This could be intermittent and needs multiple runs and is not a blocker for the release.

Kruize demos - Test with legacy API works as expected.


### KRUIZE TEST RESULTS

| # | TEST SUITE | OPENSHIFT RESULTS | MINIKUBE RESULTS | COMMENTS |
|:---|:---|:---|:---|:---|
| 1 | Kruize Remote monitoring Functional testsuite (legacy) | TOTAL - 744, PASSED - 702 / FAILED - 41 / SKIPPED - 1 | TOTAL - 744, PASSED - 701 / FAILED - 42 / SKIPPED - 1 | Existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610) |
| 2 | Kruize Remote monitoring Functional testsuite (v1) | TOTAL - 744, PASSED - 702 / FAILED - 41  / SKIPPED - 1 | TOTAL - 744, PASSED - 702 / FAILED - 41 / SKIPPED - 1 | Existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610) |
| 3 | Fault tolerant test | PASSED | PASSED | |
| 4 | Stress test (legacy/v1) | PASSED | PASSED | |
| 5 | Scalability test (short run legacy/v1 API) | PASSED | NA | |
| 6 | DB Migration test | PASSED | NA | There is an issue with backup / restore which needs to be investigated |
| 7 | Perf Profile Migration test | PASSED | NA | |
| 8 | Recommendation and box plot values validations | PASSED | PASSED | |
| 9 | Local Fault tolerant test | PASSED | PASSED | |
| 10 | Kruize Local Functional tests (legacy) | TOTAL - 234 , PASSED - 230 / FAILED - 2 / SKIPPED - 2 | TOTAL - 234, PASSED - 228 / FAILED - 4 / SKIPPED - 2 | [Issue 1395](https://github.com/kruize/autotune/issues/1395), [Issue 1273](https://github.com/kruize/autotune/issues/1273), [Issue 1821](https://github.com/kruize/autotune/issues/1821) |
| 11 | Kruize Local Functional tests (v1) | TOTAL - 234 , PASSED - 224 / FAILED - 8 / SKIPPED - 2 | TOTAL - 234, PASSED - 228 / FAILED - 4 / SKIPPED - 2 | [Issue 1395](https://github.com/kruize/autotune/issues/1395), [Issue 1273](https://github.com/kruize/autotune/issues/1273), [Issue 1821](https://github.com/kruize/autotune/issues/1821) |

Note: 
legacy - with old recommendations API - updateRecommendations / generateRecommendations
v1 - with new recommendations API - kruize/api/v1/recommendations that will replace the old APIs


Kruize test result summary:

No new regressions seen

### SCALE TEST RESULTS

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications. Short scalability test will be run as part of the release testing

Short Scalability run configuration:
- 5K exps / 15 days of results / 2 containers per exp
- Kruize replicas - 10
- OCP - Aliaslab cluster
- PV/PVC storage - 1Gi
- Kruize DB resources - requests - 10Gi / 2 cores, limits - 30Gi / 2 cores
- Kruize resources - requests - 4Gi / 2 cores, limits - 8Gi / 2 cores



| Kruize Release | Exps / Results / Recos | Execution Time | Latency: UpdateRecos (Max/Avg) | Latency: UpdateResults (Max/Avg) | Latency: LoadResults (Max/Avg) | Postgres DB (MB) | Max CPU | Max Memory (GB) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **0.10-rc3** (7 May) | 5K container / 72L / 3L | 4h 37m | 0.9 / 0.49 | 0.13 / 0.11 | 0.44 / 0.3 | 21757 | 8.38 | 41.73 |
| **0.11-rc2** (28 Jun legacy API) | 5K container / 72L / 3L | 3h 20m | 0.69 / 0.4 | 0.08 / 0.08 | 0.29 / 0.2 | 22012 | 7.44 | 41.51 |
| **0.11-rc2** (28 Jun v1 API) | 5K container / 72L / 3L | 3h 17m | 0.67 / 0.39 | 0.09 / 0.08 | 0.31 / 0.22 | 22010 | 11.72 | 43.52 |
| **0.10** (30 Jun) | 5K container / 72L / 3L | 3h 25m | 0.71 / 0.4 | 0.08 / 0.08 | 0.29 / 0.2 | 21749 | 7.78 | 42.55 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **0.10-rc3** (8 May) | 5K namespace / 72L / 3L | 3h 12m | 0.57 / 0.31 | 0.13 / 0.09 | 0.35 / 0.22 | 10779 | 9.05 | 25.37 |
| **0.11-rc2** (28 Jun legacy API) | 5K namespace / 72L / 3L | 2h 15m | 0.43 / 0.26 | 0.09 / 0.07 | 0.23 / 0.16 | 10777 | 6.36 | 27.58 |
| **0.11-rc2** (28 Jun v1 API) | 5K namespace / 72L / 3L | 2h 12m | 0.42 / 0.25 | 0.09 / 0.07 | 0.23 / 0.16 | 10776 | 7.58 | 28.39 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **0.10-rc3** (8 May) | 4.5k container, 500 namespace / 72L / 3L | 4h 35m | 0.85 / 0.47 | 0.13 / 0.11 | 0.43 / 0.3 | 20649 | 8.69 | 38.45 |
| **0.11-rc2** (29 Jun legacy API) | 4.5k container, 500 namespace / 72L / 3L | 3h 18m | 0.65 / 0.38 | 0.08 / 0.07 | 0.28 / 0.19 | 20882 | 7.11 | 42 |
| **0.11-rc2** (29 Jun v1 API) | 4.5k container, 500 namespace / 72L / 3L | 3h 18m | 0.65 / 0.38 | 0.08 / 0.07 | 0.28 / 0.19 | 20882 | 7.11 | 42 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **0.10-rc2** (1 May) | 5k gpucontainer / 72L / 3L | 7h 55m | 1.61 / 0.85 | 0.22 / 0.19 | 0.77 / 0.54 | 31206 | 12.67 | 35.94 |
| **0.11-rc2** (29 Jun legacy API) | 5k gpucontainer / 72L / 3L | 6h | 1.28 / 0.67 | 0.13 / 0.12 | 0.55 / 0.37 | 31459 | 13.62 | 38.86 |
| **0.11-rc2** (30 Jun v1 API) | 5k gpucontainer / 72L / 3L | 5h 50m | 1.23 / 0.65 | 0.14 / 0.13 | 0.57 / 0.39 | 31459 | 13.19 | 34.27 |

Here exps - Experiments, L - Lakhs


Scalability test result summary:
We observed an increase in CPU consumption in Kruize v0.11 with v1/recommendations API only for container experiments when compared to legacy API with Kruize v0.11 and Kruize v0.10 release.

----

### KRUIZE DEMOS RESULTS

| # | KRUIZE DEMO | CLUSTER | OPERATOR MODE RESULTS | NON-OPERATOR MODE RESULTS | COMMENTS |
|---|:---|:---|:---|:---|:---|
| 1 | Kruize remote monitoring demo | Openshift | NA | PASSED | with increase mem limit to 1Gi instead of 768Mi |
| 2 | Kruize remote monitoring demo | Minikube | NA | PASSED | |
| 3 | Kruize remote monitoring demo | Kind | NA | PASSED | |
| 4 | Kruize local monitoring demo | Openshift | PASSED | PASSED | |
| 5 | Kruize local monitoring demo | Minikube | PASSED | PASSED | |
| 6 | Kruize local monitoring demo | Kind | PASSED | PASSED  | |
| 7 | Kruize bulk demo | Openshift | PASSED | PASSED | |
| 8 | Kruize bulk demo | Minikube | PASSED | PASSED | |
| 9 | Kruize bulk demo | Kind | PASSED | PASSED | |
| 10 | Kruize vpa demo | Openshift | PASSED | PASSED | |
| 11 | Kruize vpa demo | Minikube | PASSED | PASSED | |
| 12 | Kruize vpa demo | Kind | PASSED  | PASSED | |
| 13 | Kruize runtimes demo | Openshift | PASSED | PASSED | |
| 14 | Kruize runtimes demo | Minikube | PASSED | PASSED | |
| 15 | Kruize runtimes demo | Kind | PASSED | PASSED | |
| 16 | Kruize optimizer demo | Openshift | PASSED | PASSED | |
| 17 | Kruize optimizer demo | Minikube | PASSED | PASSED | |
| 18 | Kruize optimizer demo | Kind | PASSED  | PASSED | |

Kruize Demos result summary:

Tested Demos with legacy API, works as expected.

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
