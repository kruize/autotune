"""
Copyright (c) 2024, 2024 Red Hat, IBM Corporation and others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Integration tests for Bulk API label / namespace / workload / container filtering.

These encode the semantics of the canonical 30-case filter matrix (T1..T30). The reference
matrix is stated with absolute counts for a specific demo cluster (causa-demo / openshift-tuning,
heap-oom / system-oom / kruize workloads); those counts are environment-specific and cannot be
asserted portably. Instead the tests assert the cluster-agnostic *relationships and invariants*
the matrix demonstrates, discovering real namespaces/workloads from the datasource at runtime:

  * grammar  — every include/exclude filter shape is accepted and the job completes
               (T1,T2,T4-T7,T10-T27,T29 grammar)
  * OR within a key           — include [a,b] == include [a] + include [b]  (T2, T6, T11, T22)
  * AND across keys           — a required key no workload has yields 0      (T8, T25, T29)
  * no match                  — a non-existent value yields 0                (T9)
  * namespace complement      — include(ns) + exclude(ns) == baseline        (T3, T28)
  * include then exclude same — exclude of an included resource wins (0)     (T30)
  * include[a,b] exclude[b]   == include[a]                                  (T30)
  * narrowing (AND)           — ns + workload is a subset of each            (T13, T14, T18, T24)
  * metadata reflection       — a namespace filter restricts returned metadata (T1, T15)
"""

import logging
import time

import pytest

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

METADATA_PROFILE_NAME = "cluster-metadata-local-monitoring"

JOB_POLL_TIMEOUT_SECONDS = 120
JOB_POLL_INTERVAL_SECONDS = 5
# The datasource (Prometheus) can transiently refuse connections under load; retry such jobs
# rather than failing the test on an environment blip.
MAX_JOB_RETRIES = 3
RETRY_BACKOFF_SECONDS = 10


def _is_transient_datasource_failure(summary):
    """True when a FAILED job's notifications indicate a transient datasource connectivity error."""
    if summary.get("status") != "FAILED":
        return False
    for notification in (summary.get("notifications") or {}).values():
        message = str(notification.get("message", "")).lower()
        if notification.get("code") == 503 or "data source" in message or "connect" in message:
            return True
    return False


# -------------------------------------------------------------------------------------------------
# Payload + job helpers
# -------------------------------------------------------------------------------------------------
DISCOVERED_DATASOURCE = None

def _get_datasource_name():
    global DISCOVERED_DATASOURCE
    if DISCOVERED_DATASOURCE is None:
        try:
            response = list_datasources()
            if response.status_code == 200:
                datasources = response.json()
                if isinstance(datasources, list):
                    if datasources:
                        DISCOVERED_DATASOURCE = datasources[0].get("name", "prometheus-1")
                elif isinstance(datasources, dict):
                    ds_list = datasources.get("datasources", [])
                    if ds_list:
                        DISCOVERED_DATASOURCE = ds_list[0].get("name", "prometheus-1")
        except Exception as e:
            logger.warning(f"Could not list datasources: {e}")
        if DISCOVERED_DATASOURCE is None:
            DISCOVERED_DATASOURCE = "prometheus-1"
    return DISCOVERED_DATASOURCE


def label_filter_payload(include=None, exclude=None):
    """Build a bulk payload with the given include/exclude filter sections merged onto an empty base."""
    payload = {
        "filter": {
            "exclude": {"namespace": [], "workload": [], "containers": [], "labels": {}},
            "include": {"namespace": [], "workload": [], "containers": [], "labels": {}},
        },
        "datasource": _get_datasource_name(),
        "metadata_profile": METADATA_PROFILE_NAME,
        "measurement_duration": "15mins",
        "time_range": {},
    }
    if include:
        payload["filter"]["include"].update(include)
    if exclude:
        payload["filter"]["exclude"].update(exclude)
    return payload


def _post_bulk_job(payload):
    response = post_bulk_api(payload, logger)
    assert response.status_code == SUCCESS_200_STATUS_CODE, \
        f"Unexpected POST status {response.status_code}: {response.text}"
    job_id = response.json().get("job_id")
    assert job_id, f"job_id not found in POST bulk API response: {response.json()}"
    return job_id


def experiment_count(payload):
    """
    POST a bulk request and return summary.total_experiments once it is finalized.

    total_experiments is fixed at discovery (when the job reaches IN_PROGRESS), so we return as
    soon as it is available rather than waiting for every recommendation to be processed — the
    count is what these tests assert, and this keeps them robust on clusters with many workloads.
    """
    summary = {}
    for attempt in range(1, MAX_JOB_RETRIES + 1):
        job_id = _post_bulk_job(payload)
        deadline = time.time() + JOB_POLL_TIMEOUT_SECONDS
        while time.time() < deadline:
            status_response = get_bulk_job_status(job_id, include="summary,experiments", logger=logger)
            assert status_response.status_code == SUCCESS_200_STATUS_CODE
            summary = status_response.json().get("summary", {})
            status = summary.get("status")
            # IN_PROGRESS means discovery is done and total_experiments is final; COMPLETED is terminal.
            if status in ("COMPLETED", "IN_PROGRESS") and "total_experiments" in summary:
                return summary.get("total_experiments", 0)
            if status == "FAILED":
                break
            time.sleep(JOB_POLL_INTERVAL_SECONDS)
        if _is_transient_datasource_failure(summary):
            if attempt < MAX_JOB_RETRIES:
                logger.warning(f"Transient datasource failure, retrying ({attempt}/{MAX_JOB_RETRIES})")
                time.sleep(RETRY_BACKOFF_SECONDS)
                continue
            # The datasource is unreachable/erroring — an infra problem, not a filter-logic bug.
            pytest.skip(f"Datasource unavailable after {MAX_JOB_RETRIES} attempts: {summary.get('notifications')}")
        assert summary.get("status") != "FAILED", f"Bulk job FAILED: {summary}"
        raise AssertionError(f"Bulk job did not reach a counted state in time (summary={summary})")
    pytest.skip(f"Datasource unavailable after {MAX_JOB_RETRIES} attempts (summary={summary})")


def _bulk_metadata(payload):
    """POST a bulk request and poll the metadata view until datasources are populated."""
    summary = {}
    metadata = {}
    for attempt in range(1, MAX_JOB_RETRIES + 1):
        job_id = _post_bulk_job(payload)
        metadata = {}
        deadline = time.time() + JOB_POLL_TIMEOUT_SECONDS
        while time.time() < deadline:
            get_response = get_bulk_job_status(job_id, include="summary,metadata", logger=logger)
            assert get_response.status_code == SUCCESS_200_STATUS_CODE
            body = get_response.json()
            metadata = body.get("metadata", {})
            summary = body.get("summary", {})
            if metadata.get("datasources"):
                return metadata
            if summary.get("status") in ("COMPLETED", "FAILED"):
                break
            time.sleep(JOB_POLL_INTERVAL_SECONDS)
        if _is_transient_datasource_failure(summary):
            if attempt < MAX_JOB_RETRIES:
                logger.warning(f"Transient datasource failure (metadata), retrying ({attempt}/{MAX_JOB_RETRIES})")
                time.sleep(RETRY_BACKOFF_SECONDS)
                continue
            pytest.skip(f"Datasource unavailable after {MAX_JOB_RETRIES} attempts: {summary.get('notifications')}")
        break
    return metadata


# -------------------------------------------------------------------------------------------------
# Cluster discovery — so the semantic tests run on real data instead of hard-coded names
# -------------------------------------------------------------------------------------------------
def _discover_namespaces():
    metadata = _bulk_metadata(label_filter_payload())
    namespaces = []
    for ds in metadata.get("datasources", {}).values():
        for cluster in ds.get("clusters", {}).values():
            namespaces.extend(cluster.get("namespaces", {}).keys())
    return namespaces


def _discover_workloads():
    """Return a list of (namespace, workload) pairs discovered from the datasource."""
    metadata = _bulk_metadata(label_filter_payload())
    workloads = []
    for ds in metadata.get("datasources", {}).values():
        for cluster in ds.get("clusters", {}).values():
            for ns_name, ns_data in cluster.get("namespaces", {}).items():
                for wl_name in ns_data.get("workloads", {}).keys():
                    workloads.append((ns_name, wl_name))
    return workloads


def _discover_containers():
    """Return a list of container names discovered from the datasource."""
    metadata = _bulk_metadata(label_filter_payload())
    containers = set()
    for ds in metadata.get("datasources", {}).values():
        for cluster in ds.get("clusters", {}).values():
            for ns_data in cluster.get("namespaces", {}).values():
                for wl_data in ns_data.get("workloads", {}).values():
                    containers.update(wl_data.get("containers", {}).keys())
    return sorted(list(containers))


def _discover_active_namespace_with_experiments():
    """Return a namespace name that has at least one experiment/workload serving recommendations."""
    workloads = _discover_workloads()
    namespaces = sorted(list({wl[0] for wl in workloads}))
    # Prioritize default or openshift-tuning as they are guaranteed to have workloads in testing envs
    if "default" in namespaces:
        namespaces.remove("default")
        namespaces.insert(0, "default")
    if "openshift-tuning" in namespaces:
        namespaces.remove("openshift-tuning")
        namespaces.insert(0, "openshift-tuning")

    for ns in namespaces:
        count = experiment_count(label_filter_payload(include={"namespace": [ns]}))
        if count > 0:
            return ns, count
    return None, 0


def _discover_active_workload_with_experiments():
    """Return a workload name that has at least one experiment serving recommendations."""
    workloads = sorted({wl[1] for wl in _discover_workloads()})
    # Prioritize known active workloads
    for wl in ["sysbench", "kruize", "quarkus-perf"]:
        if wl in workloads:
            workloads.remove(wl)
            workloads.insert(0, wl)

    for wl in workloads:
        count = experiment_count(label_filter_payload(include={"workload": [wl]}))
        if count > 0:
            return wl, count
    return None, 0


def _discover_active_container_with_experiments():
    """Return a container name that has at least one experiment serving recommendations."""
    containers = _discover_containers()
    for c in ["sysbench", "kruize", "quarkus-perf"]:
        if c in containers:
            containers.remove(c)
            containers.insert(0, c)

    for c in containers:
        count = experiment_count(label_filter_payload(include={"containers": [c]}))
        if count > 0:
            return c, count
    return None, 0


METADATA_PROFILE_CREATED = False

@pytest.fixture
def bulk_label_filter_setup(cluster_type):
    """Resolve the Kruize URL for the cluster and ensure the bulk metadata profile exists."""
    global METADATA_PROFILE_CREATED
    form_kruize_url(cluster_type)
    if not METADATA_PROFILE_CREATED:
        # Check if the profile already exists to avoid heavy recreate on busy clusters
        try:
            response = list_metadata_profiles(logging=False)
            if response.status_code == 200:
                profiles = [p.get("metadata", {}).get("name") for p in response.json()]
                if METADATA_PROFILE_NAME in profiles:
                    logger.info(f"Metadata profile {METADATA_PROFILE_NAME} already exists, skipping recreate")
                    METADATA_PROFILE_CREATED = True
                    return
        except Exception as e:
            logger.warning(f"Could not list metadata profiles: {e}")

        # Fallback to recreate if not exists or if listing failed
        try:
            delete_and_create_metadata_profile()
        except Exception as e:
            logger.warning(f"Metadata profile recreation triggered an exception: {e}. Checking if profile is active anyway.")
            try:
                response = list_metadata_profiles(logging=False)
                profiles = [p.get("metadata", {}).get("name") for p in response.json()]
                if METADATA_PROFILE_NAME in profiles:
                    logger.info(f"Metadata profile {METADATA_PROFILE_NAME} is active, proceeding.")
                    METADATA_PROFILE_CREATED = True
                    return
            except Exception:
                pass
            raise e
        METADATA_PROFILE_CREATED = True


# =================================================================================================
# Grammar coverage — every filter shape is accepted and the job completes (counts are env-specific)
# =================================================================================================
@pytest.mark.test_bulk_api_ros
@pytest.mark.sanity
@pytest.mark.parametrize(
    "include, exclude",
    [
        pytest.param({"namespace": ["default"]}, None, id="T1_include_namespace"),
        pytest.param({"namespace": ["default", "monitoring"]}, None, id="T2_include_two_namespaces"),
        pytest.param(None, {"namespace": ["default"]}, id="T3_exclude_namespace"),
        pytest.param({"labels": {"app": "kruize"}}, None, id="T4_include_single_label"),
        pytest.param(None, {"labels": {"app": "kruize"}}, id="T5_exclude_single_label"),
        pytest.param({"labels": {"app": ["kruize", "kruize-db"]}}, None, id="T6_multivalue_label_OR"),
        pytest.param({"labels": {"app": "kruize", "version": "v1"}}, None, id="T7_multikey_label_AND"),
        pytest.param(None, {"labels": {"app": "kruize", "version": "v1"}}, id="T10_exclude_multikey_label"),
        pytest.param(None, {"labels": {"app": ["kruize", "kruize-db"]}}, id="T11_exclude_multivalue_label"),
        pytest.param({"labels": {"app": "sysbench"}}, None, id="T12_label_only"),
        pytest.param({"namespace": ["default"], "labels": {"app": "sysbench"}}, None, id="T13_namespace_and_label"),
        pytest.param({"namespace": ["default"], "labels": {"app": "sysbench", "version": "v1"}}, None, id="T14_namespace_and_multikey_label"),
        pytest.param({"namespace": ["default"]}, None, id="T15_include_namespace_metadata"),
        pytest.param({"labels": {"app": "kruize"}}, {"namespace": ["default"]}, id="T16_include_label_exclude_ns"),
        pytest.param({"workload": ["kruize"]}, {"labels": {"app": "kruize"}}, id="T17_include_workload_exclude_label"),
        pytest.param({"workload": ["kruize"], "labels": {"app": "kruize"}}, None, id="T18_workload_and_label"),
        pytest.param({"namespace": ["default"]}, {"labels": {"app": "kruize"}}, id="T19_include_ns_exclude_label"),
        pytest.param({"namespace": ["default"]}, {"labels": {"app": ["kruize", "kruize-db"]}}, id="T20_exclude_multivalue_label"),
        pytest.param({"workload": ["kruize"]}, None, id="T21_include_workload"),
        pytest.param({"workload": ["kruize", "kruize-db"]}, None, id="T22_include_two_workloads"),
        pytest.param({"namespace": ["default"]}, {"workload": ["kruize"]}, id="T23_include_ns_exclude_workload"),
        pytest.param({"namespace": ["default"], "workload": ["sysbench"], "labels": {"app": "sysbench"}}, None, id="T24_ns_workload_label"),
        pytest.param(None, {"labels": {"app": "kruize", "version": "no-such-version-xyz"}}, id="T25_exclude_unmatched_label_key"),
        pytest.param({"containers": ["kruize"]}, None, id="T26_include_container"),
        pytest.param({"namespace": ["default"]}, {"containers": ["kruize"]}, id="T27_include_ns_exclude_container"),
        pytest.param({}, None, id="T28_no_filter"),
        pytest.param({"labels": {"app": ["kruize", "kruize-db"], "version": "v1"}}, None, id="T29_multivalue_and_multikey"),
    ],
)
def test_label_filter_shapes_complete(bulk_label_filter_setup, include, exclude, caplog):
    with caplog.at_level(logging.INFO):
        count = experiment_count(label_filter_payload(include=include, exclude=exclude))
    assert count >= 0


# =================================================================================================
# Deterministic outcomes — hold regardless of which workloads are deployed
# =================================================================================================
@pytest.mark.test_bulk_api_ros
def test_nonexistent_label_yields_no_experiments(bulk_label_filter_setup, caplog):
    """T9 — a label value matching nothing produces zero experiments."""
    with caplog.at_level(logging.INFO):
        count = experiment_count(label_filter_payload(include={"labels": {"app": "no-such-workload-xyz"}}))
    assert count == 0, f"Expected 0 experiments for a non-existent label, got {count}"


@pytest.mark.test_bulk_api_ros
def test_multikey_label_with_missing_key_yields_no_experiments(bulk_label_filter_setup, caplog):
    """T8 / T25 — labels are ANDed across keys; requiring a key no workload has yields zero."""
    with caplog.at_level(logging.INFO):
        count = experiment_count(
            label_filter_payload(include={"labels": {"app": "kruize", "version": "no-such-version-xyz"}})
        )
    assert count == 0, f"Expected 0 experiments when a required label key is unmatched, got {count}"


# =================================================================================================
# Namespace invariants (discovered namespaces)
# =================================================================================================
@pytest.mark.test_bulk_api_ros
def test_namespace_union_equals_sum_of_singles(bulk_label_filter_setup, caplog):
    """T2 — namespaces are disjoint, so include[ns1,ns2] == include[ns1] + include[ns2]."""
    workloads = _discover_workloads()
    namespaces = sorted(list({wl[0] for wl in workloads}))
    if len(namespaces) < 2:
        pytest.skip("Need at least two active namespaces to validate namespace union")
    ns1, ns2 = namespaces[0], namespaces[1]
    with caplog.at_level(logging.INFO):
        both = experiment_count(label_filter_payload(include={"namespace": [ns1, ns2]}))
        only1 = experiment_count(label_filter_payload(include={"namespace": [ns1]}))
        only2 = experiment_count(label_filter_payload(include={"namespace": [ns2]}))
    assert both == only1 + only2, \
        f"include[{ns1},{ns2}]={both} != include[{ns1}]={only1} + include[{ns2}]={only2}"


@pytest.mark.test_bulk_api_ros
def test_exclude_namespace_is_complement_of_include(bulk_label_filter_setup, caplog):
    """T3 / T28 — include(ns) + exclude(ns) == unfiltered total."""
    with caplog.at_level(logging.INFO):
        total = experiment_count(label_filter_payload())
        if total == 0:
            pytest.skip("No experiments discovered — cannot validate namespace complement")
        workloads = _discover_workloads()
        if not workloads:
            pytest.skip("No workloads discovered")
        ns = workloads[0][0]
        included = experiment_count(label_filter_payload(include={"namespace": [ns]}))
        excluded = experiment_count(label_filter_payload(exclude={"namespace": [ns]}))
    assert included + excluded == total, \
        f"include(ns={ns})={included} + exclude(ns={ns})={excluded} != baseline={total}"


@pytest.mark.test_bulk_api_ros
def test_include_then_exclude_same_namespace_yields_zero(bulk_label_filter_setup, caplog):
    """T30 — excluding the same namespace that is included wins: zero experiments."""
    workloads = _discover_workloads()
    if not workloads:
        pytest.skip("No workloads discovered")
    ns = workloads[0][0]
    with caplog.at_level(logging.INFO):
        include_only = experiment_count(label_filter_payload(include={"namespace": [ns]}))
        assert include_only > 0, f"Expected > 0 experiments when including namespace {ns}"
        both = experiment_count(label_filter_payload(include={"namespace": [ns]}, exclude={"namespace": [ns]}))
    assert both == 0, f"Expected 0 experiments when the same namespace is included and excluded, got {both}"


@pytest.mark.test_bulk_api_ros
def test_include_two_namespaces_exclude_one_equals_other(bulk_label_filter_setup, caplog):
    """T30 — include[ns1,ns2] excluding ns2 equals include[ns1]."""
    workloads = _discover_workloads()
    namespaces = sorted(list({wl[0] for wl in workloads}))
    if len(namespaces) < 2:
        pytest.skip("Need at least two active namespaces")
    ns1, ns2 = namespaces[0], namespaces[1]
    with caplog.at_level(logging.INFO):
        only1 = experiment_count(label_filter_payload(include={"namespace": [ns1]}))
        both_minus_ns2 = experiment_count(
            label_filter_payload(include={"namespace": [ns1, ns2]}, exclude={"namespace": [ns2]})
        )
    assert both_minus_ns2 == only1, \
        f"include[{ns1},{ns2}] exclude[{ns2}]={both_minus_ns2} != include[{ns1}]={only1}"


# =================================================================================================
# Workload invariants (discovered workloads)
# =================================================================================================
@pytest.mark.test_bulk_api_ros
def test_workload_union_equals_sum_of_singles(bulk_label_filter_setup, caplog):
    """T22 — include[w1,w2] == include[w1] + include[w2] for two distinct workload names."""
    workloads = sorted({wl for _, wl in _discover_workloads()})
    if len(workloads) < 2:
        pytest.skip("Need at least two distinct workloads to validate workload union")
    w1, w2 = workloads[0], workloads[1]
    with caplog.at_level(logging.INFO):
        both = experiment_count(label_filter_payload(include={"workload": [w1, w2]}))
        only1 = experiment_count(label_filter_payload(include={"workload": [w1]}))
        only2 = experiment_count(label_filter_payload(include={"workload": [w2]}))
    assert both == only1 + only2, \
        f"include[{w1},{w2}]={both} != include[{w1}]={only1} + include[{w2}]={only2}"


@pytest.mark.test_bulk_api_ros
def test_namespace_and_workload_is_subset(bulk_label_filter_setup, caplog):
    """T13 / T14 / T18 / T24 — namespace + workload (AND) is a subset of each filter alone."""
    workloads = _discover_workloads()
    if not workloads:
        pytest.skip("No workloads discovered")
    ns, wl = workloads[0]
    with caplog.at_level(logging.INFO):
        by_namespace = experiment_count(label_filter_payload(include={"namespace": [ns]}))
        by_workload = experiment_count(label_filter_payload(include={"workload": [wl]}))
        combined = experiment_count(label_filter_payload(include={"namespace": [ns], "workload": [wl]}))
    assert combined <= by_namespace, f"ns+workload={combined} should be <= namespace-only={by_namespace}"
    assert combined <= by_workload, f"ns+workload={combined} should be <= workload-only={by_workload}"
    assert combined >= 1, f"Expected at least one experiment for ns={ns}, workload={wl}"


# =================================================================================================
# Metadata reflection
# =================================================================================================
@pytest.mark.test_bulk_api_ros
def test_namespace_filter_restricts_returned_metadata(bulk_label_filter_setup, caplog):
    """T1 / T15 — including one namespace restricts the job metadata to only that namespace."""
    namespaces = _discover_namespaces()
    if not namespaces:
        pytest.skip("No namespace discovered to validate metadata restriction")
    ns = namespaces[0]
    with caplog.at_level(logging.INFO):
        metadata = _bulk_metadata(label_filter_payload(include={"namespace": [ns]}))
    for ds_data in metadata.get("datasources", {}).values():
        for cluster_data in ds_data.get("clusters", {}).values():
            returned = list(cluster_data.get("namespaces", {}).keys())
            assert returned == [ns], f"Namespace filter not honoured: expected only [{ns}], got {returned}"


# =================================================================================================
# Additional workload invariants
# =================================================================================================
@pytest.mark.test_bulk_api_ros
def test_exclude_workload_is_complement_of_include(bulk_label_filter_setup, caplog):
    """Exclude(wl) is complement of Include(wl) -> include(wl) + exclude(wl) == baseline total."""
    with caplog.at_level(logging.INFO):
        total = experiment_count(label_filter_payload())
        if total == 0:
            pytest.skip("No experiments discovered — cannot validate workload complement")
        wl, included = _discover_active_workload_with_experiments()
        if not wl:
            pytest.skip("No workload with active experiments discovered")
        excluded = experiment_count(label_filter_payload(exclude={"workload": [wl]}))
    assert included + excluded == total, \
        f"include(wl={wl})={included} + exclude(wl={wl})={excluded} != baseline={total}"


@pytest.mark.test_bulk_api_ros
def test_include_then_exclude_same_workload_yields_zero(bulk_label_filter_setup, caplog):
    """Excluding the same workload that is included wins: zero experiments."""
    wl, include_only = _discover_active_workload_with_experiments()
    if not wl:
        pytest.skip("No workload with active experiments discovered")
    with caplog.at_level(logging.INFO):
        both = experiment_count(label_filter_payload(include={"workload": [wl]}, exclude={"workload": [wl]}))
    assert both == 0, f"Expected 0 experiments when the same workload is included and excluded, got {both}"


@pytest.mark.test_bulk_api_ros
def test_include_two_workloads_exclude_one_equals_other(bulk_label_filter_setup, caplog):
    """Include[wl1,wl2] excluding wl2 equals include[wl1]."""
    workloads = sorted({wl for _, wl in _discover_workloads()})
    if len(workloads) < 2:
        pytest.skip("Need at least two workloads")
    wl1, wl2 = workloads[0], workloads[1]
    with caplog.at_level(logging.INFO):
        only1 = experiment_count(label_filter_payload(include={"workload": [wl1]}))
        both_minus_wl2 = experiment_count(
            label_filter_payload(include={"workload": [wl1, wl2]}, exclude={"workload": [wl2]})
        )
    assert both_minus_wl2 == only1, \
        f"include[{wl1},{wl2}] exclude[{wl2}]={both_minus_wl2} != include[{wl1}]={only1}"


# =================================================================================================
# Container invariants (discovered containers)
# =================================================================================================
@pytest.mark.test_bulk_api_ros
def test_container_union_equals_sum_of_singles(bulk_label_filter_setup, caplog):
    """T26 OR within key — containers are disjoint (within unique names), so include[c1,c2] == include[c1] + include[c2]."""
    containers = _discover_containers()
    if len(containers) < 2:
        pytest.skip("Need at least two containers to validate container union")
    c1, c2 = containers[0], containers[1]
    with caplog.at_level(logging.INFO):
        both = experiment_count(label_filter_payload(include={"containers": [c1, c2]}))
        only1 = experiment_count(label_filter_payload(include={"containers": [c1]}))
        only2 = experiment_count(label_filter_payload(include={"containers": [c2]}))
    assert both == only1 + only2, \
        f"include[{c1},{c2}]={both} != include[{c1}]={only1} + include[{c2}]={only2}"


@pytest.mark.test_bulk_api_ros
def test_exclude_container_is_complement_of_include(bulk_label_filter_setup, caplog):
    """T27 / T28 complement — include(c) + exclude(c) == unfiltered total."""
    with caplog.at_level(logging.INFO):
        total = experiment_count(label_filter_payload())
        if total == 0:
            pytest.skip("No experiments discovered — cannot validate container complement")
        c, included = _discover_active_container_with_experiments()
        if not c:
            pytest.skip("No container with active experiments discovered")
        excluded = experiment_count(label_filter_payload(exclude={"containers": [c]}))
    assert included + excluded == total, \
        f"include(container={c})={included} + exclude(container={c})={excluded} != baseline={total}"


@pytest.mark.test_bulk_api_ros
def test_include_then_exclude_same_container_yields_zero(bulk_label_filter_setup, caplog):
    """Excluding the same container that is included wins: zero experiments."""
    c, include_only = _discover_active_container_with_experiments()
    if not c:
        pytest.skip("No container with active experiments discovered")
    with caplog.at_level(logging.INFO):
        both = experiment_count(label_filter_payload(include={"containers": [c]}, exclude={"containers": [c]}))
    assert both == 0, f"Expected 0 experiments when the same container is included and excluded, got {both}"


@pytest.mark.test_bulk_api_ros
def test_include_two_containers_exclude_one_equals_other(bulk_label_filter_setup, caplog):
    """Include[c1,c2] excluding c2 equals include[c1]."""
    containers = _discover_containers()
    if len(containers) < 2:
        pytest.skip("Need at least two containers")
    c1, c2 = containers[0], containers[1]
    with caplog.at_level(logging.INFO):
        only1 = experiment_count(label_filter_payload(include={"containers": [c1]}))
        both_minus_c2 = experiment_count(
            label_filter_payload(include={"containers": [c1, c2]}, exclude={"containers": [c2]})
        )
    assert both_minus_c2 == only1, \
        f"include[{c1},{c2}] exclude[{c2}]={both_minus_c2} != include[{c1}]={only1}"
