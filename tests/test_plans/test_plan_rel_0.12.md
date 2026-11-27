# KRUIZE TEST PLAN RELEASE 0.12

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

This document describes the test plan for Kruize release 0.12

----

## FEATURES TO BE TESTED

* Bulk Config CRUD API support with validation, logging, and granular updates
* Cluster name support in datasources and Bulk API
* Label filter support in Bulk API with query template substitution for pod label filters
* Model settings and term settings support in Bulk API
* Experiment type support in Bulk API
* Notification support for over/under provisioned workloads
* Metadata profile validation updates and documentation
* Exclude Configmap and CatalogSource from workloads while collecting metadata
* Filter only relevant workloads that are supported by Kruize
* Resource requests and limits added to apps in all deployment yamls
* TLS protocol validation unit tests


------

## BUG FIXES TO BE TESTED

* Fix comma issue for the non-bulk profile flow
* Guard against null credentials node when loading no-auth datasource
* Allow PUT bulkConfigs with scheduling-only recommendation_settings
* Revert metadata profile query names to support both bulk and default flows
* Fix issue with bulk config service when called from optimizer
* CVE fixes: jackson version updates
* Dependabot workflow update to raise PRs against mvp_demo


---

## TEST ENVIRONMENT

* Minikube Cluster
* Openshift Cluster
* Kind Cluster (demo testing)

---

## TEST DELIVERABLES

### New Test Cases Developed

| # | ISSUE (NEW FEATURE)                                                                            | TEST DESCRIPTION                                                                                                                                 | TEST DELIVERABLES                                    | RESULTS | COMMENTS |
|---|------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|---------|----------|
| 1 | Bulk Config CRUD API tests                                                                     | Added new tests for creating, listing, updating, and deleting bulk configurations including validation and negative scenarios                    | [2019](https://github.com/kruize/autotune/pull/2019) | PASSED  |          |
| 2 | Exp-Type, Model and Term settings support in Bulk API and Cluster support in datasources tests | Added new tests for experiment_types, model_settings, term_settings in Bulk API and cluster metadata validation in datasources                   | [2013](https://github.com/kruize/autotune/pull/2013) | PASSED  |          |
| 3 | MetadataProfile validation tests                                                               | Added unit tests for updateMetadata validation including query classification, Prometheus validation, placeholder handling, and filter injection | [2098](https://github.com/kruize/autotune/pull/2098) | PASSED  |          |
| 4 | Label filter tests for Bulk API                                                                | Added unit tests for label filter builder, query template substitution, include/exclude filters, key sanitization, and value escaping            | [2045](https://github.com/kruize/autotune/pull/2045) | PASSED  |          |
| 5 | Cluster name support tests                                                                     | Added comprehensive tests for cluster name validation across REST API, business logic passthrough, DB persistence, and input validation          | [1985](https://github.com/kruize/autotune/pull/1985) | PASSED  |          |
| 6 | TLS protocol validation unit tests                                                             | Added unit tests for TLS protocol handling validating acceptance of TLS 1.2/1.3 and rejection of legacy protocols                                | [1898](https://github.com/kruize/autotune/pull/1898) | PASSED  |          |
| 7 | CVE fixes: jackson dependency updates                                                          | Verified no regressions from jackson version upgrades via functional and stress tests                                                             | [2078](https://github.com/kruize/autotune/pull/2078), [2076](https://github.com/kruize/autotune/pull/2076), [2057](https://github.com/kruize/autotune/pull/2057) | PASSED  | No functional regressions observed |

---

## RELEASE TESTING

### RELEASE TESTS

As part of the release testing, following tests will be executed:
- [Kruize Remote monitoring Functional tests](/tests/scripts/remote_monitoring_tests/Remote_monitoring_tests.md)
- [Fault-tolerant test](/tests/scripts/remote_monitoring_tests/fault_tolerant_tests.md)
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

All Release tests have been run against the Kruize release 0.12 image and all tests have passed

Scalability short test - We observed an increase in the total time taken due to manual run on IBM cluster with v1 API when compared with v0.11 and v0.10 release only for container experiments. This could be improved by running it on jenkins.

Kruize demos - Test works as expected.


### KRUIZE TEST RESULTS

| #  | TEST SUITE                                             | OPENSHIFT RESULTS                                     | MINIKUBE RESULTS                                      | COMMENTS                                                                                                                                                                                 |
|:---|:-------------------------------------------------------|:------------------------------------------------------|:------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Kruize Remote monitoring Functional testsuite (legacy) | TOTAL - 744, PASSED - 702 / FAILED - 41 / SKIPPED - 1 | TOTAL - 744, PASSED - 701 / FAILED - 42 / SKIPPED - 1 | Existing issues - [559](https://github.com/kruize/autotune/issues/559), [610](https://github.com/kruize/autotune/issues/610)                                                             |
| 3  | Fault tolerant test                                    | PASSED                                                | PASSED                                                |                                                                                                                                                                                          |
| 4  | Stress test (legacy/v1)                                | PASSED                                                | PASSED                                                |                                                                                                                                                                                          |
| 5  | Scalability test (short run legacy/v1 API)             | PASSED                                                | NA                                                    |                                                                                                                                                                                          |
| 6  | DB Migration test                                      | PASSED                                                | NA                                                    | There is an issue with backup / restore which needs to be investigated                                                                                                                   |
| 7  | Perf Profile Migration test                            | PASSED                                                | NA                                                    |                                                                                                                                                                                          |
| 8  | Recommendation and box plot values validations         | PASSED                                                | PASSED                                                |                                                                                                                                                                                          |
| 9  | Local Fault tolerant test                              | PASSED                                                | PASSED                                                |                                                                                                                                                                                          |
| 10 | Kruize Local Functional tests (legacy)                 | TOTAL - 234 , PASSED - 230 / FAILED - 1 / SKIPPED - 3 | TOTAL - 234, PASSED - 228 / FAILED - 3 / SKIPPED - 3  | [Issue 1395](https://github.com/kruize/autotune/issues/1395), [Issue 1273](https://github.com/kruize/autotune/issues/1273), [Issue 1821](https://github.com/kruize/autotune/issues/1821) |


Kruize test result summary:

No new regressions seen

### SCALE TEST RESULTS

Evaluate Kruize Scalability on OCP, with 5k experiments by uploading resource usage data for 15 days and update recommendations.
Changes do not have scalability implications for container experiments. Short scalability test for container experiments will be run as part of the release testing. Namespace, mixed, and GPU workload scalability tests were not run for 0.12 as the changes in this release do not affect those workload paths

Short Scalability run configuration:
- 5K exps / 15 days of results / 2 containers per exp
- Kruize replicas - 3
- OCP - IBM cluster
- PV/PVC storage - 1Gi
- Kruize DB resources - requests - 10Gi / 2 cores, limits - 16Gi / 2 cores
- Kruize resources - requests - 4Gi / 1 cores, limits - 8Gi / 3 cores



| Kruize Release                   | Exps / Results / Recos                   | Execution Time | Latency: UpdateRecos (Max/Avg) | Latency: UpdateResults (Max/Avg) | Latency: LoadResults (Max/Avg) | Postgres DB (MB) | Max CPU | Max Memory (GB) |
|:---------------------------------|:-----------------------------------------|:---------------|:-------------------------------|:---------------------------------|:-------------------------------|:-----------------|:--------|:----------------|
| **0.10-rc3** (7 May)             | 5K container / 72L / 3L                  | 4h 37m         | 0.9 / 0.49                     | 0.13 / 0.11                      | 0.44 / 0.3                     | 21757            | 8.38    | 41.73           |
| **0.11-rc2** (28 Jun legacy API) | 5K container / 72L / 3L                  | 3h 20m         | 0.69 / 0.4                     | 0.08 / 0.08                      | 0.29 / 0.2                     | 22012            | 7.44    | 41.51           |
| **0.11-rc2** (28 Jun v1 API)     | 5K container / 72L / 3L                  | 3h 17m         | 0.67 / 0.39                    | 0.09 / 0.08                      | 0.31 / 0.22                    | 22010            | 11.72   | 43.52           |
| **0.10** (30 Jun)                | 5K container / 72L / 3L                  | 3h 25m         | 0.71 / 0.4                     | 0.08 / 0.08                      | 0.29 / 0.2                     | 21749            | 7.78    | 42.55           |
| ---                              | ---                                      | ---            | ---                            | ---                              | ---                            | ---              | ---     | ---             |
| **0.10-rc3** (8 May)             | 5K namespace / 72L / 3L                  | 3h 12m         | 0.57 / 0.31                    | 0.13 / 0.09                      | 0.35 / 0.22                    | 10779            | 9.05    | 25.37           |
| **0.11-rc2** (28 Jun legacy API) | 5K namespace / 72L / 3L                  | 2h 15m         | 0.43 / 0.26                    | 0.09 / 0.07                      | 0.23 / 0.16                    | 10777            | 6.36    | 27.58           |
| **0.11-rc2** (28 Jun v1 API)     | 5K namespace / 72L / 3L                  | 2h 12m         | 0.42 / 0.25                    | 0.09 / 0.07                      | 0.23 / 0.16                    | 10776            | 7.58    | 28.39           |
| ---                              | ---                                      | ---            | ---                            | ---                              | ---                            | ---              | ---     | ---             |
| **0.10-rc3** (8 May)             | 4.5k container, 500 namespace / 72L / 3L | 4h 35m         | 0.85 / 0.47                    | 0.13 / 0.11                      | 0.43 / 0.3                     | 20649            | 8.69    | 38.45           |
| **0.11-rc2** (29 Jun legacy API) | 4.5k container, 500 namespace / 72L / 3L | 3h 18m         | 0.65 / 0.38                    | 0.08 / 0.07                      | 0.28 / 0.19                    | 20882            | 7.11    | 42              |
| **0.11-rc2** (29 Jun v1 API)     | 4.5k container, 500 namespace / 72L / 3L | 3h 18m         | 0.65 / 0.38                    | 0.08 / 0.07                      | 0.28 / 0.19                    | 20882            | 7.11    | 42              |
| ---                              | ---                                      | ---            | ---                            | ---                              | ---                            | ---              | ---     | ---             |
| **0.10-rc2** (1 May)             | 5k gpucontainer / 72L / 3L               | 7h 55m         | 1.61 / 0.85                    | 0.22 / 0.19                      | 0.77 / 0.54                    | 31206            | 12.67   | 35.94           |
| **0.11-rc2** (29 Jun legacy API) | 5k gpucontainer / 72L / 3L               | 6h             | 1.28 / 0.67                    | 0.13 / 0.12                      | 0.55 / 0.37                    | 31459            | 13.62   | 38.86           |
| **0.11-rc2** (30 Jun v1 API)     | 5k gpucontainer / 72L / 3L               | 5h 50m         | 1.23 / 0.65                    | 0.14 / 0.13                      | 0.57 / 0.39                    | 31459            | 13.19   | 34.27           |
| ---                              | ---                                      | ---            | ---                            | ---                              | ---                            | ---              | ---     | ---             |
| **0.12-rc3** (4th Nov)           | 5k container / 72L / 3L                  | 9h 08m         | 1.75 / 0.85                    | 0.18 / 0.14                      | 0.61 / 0.4                     | 22232            | 8.68    | 16.63 GB        |

Here exps - Experiments, L - Lakhs


----

### KRUIZE DEMOS RESULTS

| #  | KRUIZE DEMO                   | CLUSTER   | OPERATOR MODE RESULTS | NON-OPERATOR MODE RESULTS | COMMENTS |
|----|:------------------------------|:----------|:----------------------|:--------------------------|:---------|
| 1  | Kruize remote monitoring demo | Openshift | NA                    | PASSED                    |          |
| 2  | Kruize remote monitoring demo | Minikube  | NA                    | PASSED                    |          |
| 3  | Kruize remote monitoring demo | Kind      | NA                    | PASSED                    |          |
| 4  | Kruize local monitoring demo  | Openshift | PASSED                | PASSED                    |          |
| 5  | Kruize local monitoring demo  | Minikube  | PASSED                | PASSED                    |          |
| 6  | Kruize local monitoring demo  | Kind      | PASSED                | PASSED                    |          |
| 7  | Kruize bulk demo              | Openshift | PASSED                | PASSED                    |          |
| 8  | Kruize bulk demo              | Minikube  | PASSED                | PASSED                    |          |
| 9  | Kruize bulk demo              | Kind      | PASSED                | PASSED                    |          |
| 10 | Kruize vpa demo               | Openshift | PASSED                | PASSED                    |          |
| 11 | Kruize vpa demo               | Minikube  | PASSED                | PASSED                    |          |
| 12 | Kruize vpa demo               | Kind      | PASSED                | PASSED                    |          |
| 13 | Kruize runtimes demo          | Openshift | PASSED                | PASSED                    |          |
| 14 | Kruize runtimes demo          | Minikube  | PASSED                | PASSED                    |          |
| 15 | Kruize runtimes demo          | Kind      | PASSED                | PASSED                    |          |
| 16 | Kruize optimizer demo         | Openshift | PASSED                | PASSED                    |          |
| 17 | Kruize optimizer demo         | Minikube  | PASSED                | PASSED                    |          |
| 18 | Kruize optimizer demo         | Kind      | PASSED                | PASSED                    |          |

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
