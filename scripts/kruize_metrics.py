"""
Copyright (c) 2023, 2023 Red Hat, IBM Corporation and others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import requests
import csv
import sched
import time
from datetime import datetime, timedelta
import subprocess
import sys, getopt
import threading
import os
import argparse

csv_headers = ["timestamp","listRecommendations_count_success","listExperiments_count_success","createExperiment_count_success","updateResults_count_success","updateRecommendations_count_success","createBulkJob_count_success", "bulkJobs_count_running" , "jobStatus_count_success", "bulk_getExperimentMap_count_success", "runBulkJob_count_success","importMetadata_count_success", "generatePlots_count_success","loadRecommendationsByExperimentName_count_success","loadRecommendationsByExperimentNameAndDate_count_success","loadResultsByExperimentName_count_success","loadExperimentByName_count_success","addRecommendationToDB_count_success","addResultToDB_count_success","addBulkResultsToDBAndFetchFailedResults_count_success","addExperimentToDB_count_success","addPerformanceProfileToDB_count_success","loadPerformanceProfileByName_count_success","loadAllPerformanceProfiles_count_success","listRecommendations_count_failure","listExperiments_count_failure","createExperiment_count_failure","updateResults_count_failure","updateRecommendations_count_failure","createBulkJob_count_failure","jobStatus_count_failure","runBulkJob_count_failure","importMetadata_count_failure","generatePlots_count_failure","loadRecommendationsByExperimentName_count_failure","loadRecommendationsByExperimentNameAndDate_count_failure","loadResultsByExperimentName_count_failure","loadExperimentByName_count_failure","addRecommendationToDB_count_failure","addResultToDB_count_failure","addBulkResultsToDBAndFetchFailedResults_count_failure","addExperimentToDB_count_failure","addPerformanceProfileToDB_count_failure","loadPerformanceProfileByName_count_failure","loadAllPerformanceProfiles_count_failure","listRecommendations_sum_success","listExperiments_sum_success","createExperiment_sum_success","updateResults_sum_success","updateRecommendations_sum_success","createBulkJob_sum_success", "jobStatus_sum_success", "bulk_getExperimentMap_sum_success", "runBulkJob_sum_success","importMetadata_sum_success", "generatePlots_sum_success","loadRecommendationsByExperimentName_sum_success","loadRecommendationsByExperimentNameAndDate_sum_success","loadResultsByExperimentName_sum_success","loadExperimentByName_sum_success","addRecommendationToDB_sum_success","addResultToDB_sum_success","addBulkResultsToDBAndFetchFailedResults_sum_success","addExperimentToDB_sum_success","addPerformanceProfileToDB_sum_success","loadPerformanceProfileByName_sum_success","loadAllPerformanceProfiles_sum_success","listRecommendations_sum_failure","listExperiments_sum_failure","createExperiment_sum_failure","updateResults_sum_failure","updateRecommendations_sum_failure","createBulkJob_sum_failure", "jobStatus_sum_failure", "bulk_getExperimentMap_sum_failure", "runBulkJob_sum_failure","importMetadata_sum_failure","generatePlots_sum_failure","loadRecommendationsByExperimentName_sum_failure","loadRecommendationsByExperimentNameAndDate_sum_failure","loadResultsByExperimentName_sum_failure","loadExperimentByName_sum_failure","addRecommendationToDB_sum_failure","addResultToDB_sum_failure","addBulkResultsToDBAndFetchFailedResults_sum_failure","addExperimentToDB_sum_failure","addPerformanceProfileToDB_sum_failure","loadPerformanceProfileByName_sum_failure","loadAllPerformanceProfiles_sum_failure","loadAllRecommendations_sum_failure","loadAllExperiments_sum_failure","loadAllResults_sum_failure","loadAllRecommendations_sum_success","loadAllExperiments_sum_success","loadAllResults_sum_success","listRecommendations_max_success","listExperiments_max_success","createExperiment_max_success","updateResults_max_success","updateRecommendations_max_success","createBulkJob_max_success", "jobStatus_max_success", "bulk_getExperimentMap_max_success", "runBulkJob_max_success","importMetadata_max_success","generatePlots_max_success","loadRecommendationsByExperimentName_max_success","loadRecommendationsByExperimentNameAndDate_max_success","loadResultsByExperimentName_max_success","loadExperimentByName_max_success","addRecommendationToDB_max_success","addResultToDB_max_success","addBulkResultsToDBAndFetchFailedResults_max_success","addExperimentToDB_max_success","addPerformanceProfileToDB_max_success","loadPerformanceProfileByName_max_success","loadAllPerformanceProfiles_max_success","kruizedb_cpu_max","kruizedb_memory","kruize_cpu_max","kruize_memory","kruize_results","db_size","updateResultsPerCall_success","updateRecommendationsPerCall_success","BulkJobPerCall_success", "updateRecommendations_notifications_total"]


queries_map_total = {
        "listRecommendations_count_success": "sum((kruizeAPI_count{api=\"listRecommendations\",application=\"Kruize\",status=\"success\"}))",
        "listExperiments_count_success": "sum((kruizeAPI_count{api=\"listExperiments\",application=\"Kruize\",status=\"success\"}))",
        "createExperiment_count_success": "sum((kruizeAPI_count{api=\"createExperiment\",application=\"Kruize\",status=\"success\"}))",
        "updateResults_count_success": "sum((kruizeAPI_count{api=\"updateResults\",application=\"Kruize\",status=\"success\"}))",
        "updateRecommendations_count_success": "sum((kruizeAPI_count{api=\"updateRecommendations\",application=\"Kruize\",status=\"success\"}))",
        "createBulkJob_count_success": "sum((kruizeAPI_count{api=\"bulk\",application=\"Kruize\",method=\"createBulkJob\",status=\"success\"}))",
        "bulkJobs_count_running": "sum((kruizeAPI_active_jobs_count{api=\"bulk\",application=\"Kruize\",method=\"runBulkJob\",status=\"running\"}))",
        "jobStatus_count_success": "sum((kruizeAPI_count{api=\"bulk\",application=\"Kruize\",method=\"jobStatus\",status=\"success\"}))",
        "bulk_getExperimentMap_count_success": "sum((kruizeAPI_count{api=\"bulk\",application=\"Kruize\",method=\"getExperimentMap\",status=\"success\"}))",
        "runBulkJob_count_success": "sum((kruizeAPI_count{api=\"bulk\",application=\"Kruize\",method=\"runBulkJob\",status=\"success\"}))",
        "importMetadata_count_success": "sum((kruizeAPI_count{api=\"datasources\",application=\"Kruize\",method=\"importMetadata\",status=\"success\"}))",
        "generatePlots_count_success": "sum((KruizeMethod_count{method=\"generatePlots\",application=\"Kruize\",status=\"success\"}))",
        "loadRecommendationsByExperimentName_count_success": "sum((kruizeDB_count{method=\"loadRecommendationsByExperimentName\",application=\"Kruize\",status=\"success\"}))",
        "loadRecommendationsByExperimentNameAndDate_count_success": "sum((kruizeDB_count{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"success\"}))",
        "loadResultsByExperimentName_count_success": "sum((kruizeDB_count{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"success\"}))",
        "loadExperimentByName_count_success": "sum((kruizeDB_count{method=\"loadExperimentByName\",application=\"Kruize\",status=\"success\"}))",
        "addRecommendationToDB_count_success": "sum((kruizeDB_count{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"success\"}))",
        "addResultToDB_count_success": "sum((kruizeDB_count{method=\"addResultToDB\",application=\"Kruize\",status=\"success\"}))",
        "addBulkResultsToDBAndFetchFailedResults_count_success": "sum((kruizeDB_count{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"success\"}))",
        "addExperimentToDB_count_success": "sum((kruizeDB_count{method=\"addExperimentToDB\",application=\"Kruize\",status=\"success\"}))",
        "addPerformanceProfileToDB_count_success": "sum((kruizeDB_count{method=\"addPerformanceProfileToDB\",application=\"Kruize\",status=\"success\"}))",
        "loadPerformanceProfileByName_count_success": "sum((kruizeDB_count{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"success\"}))",
        "loadAllPerformanceProfiles_count_success": "sum((kruizeDB_count{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"success\"}))",
        "listRecommendations_count_failure": "sum((kruizeAPI_count{api=\"listRecommendations\",application=\"Kruize\",status=\"failure\"}))",
        "listExperiments_count_failure": "sum((kruizeAPI_count{api=\"listExperiments\",application=\"Kruize\",status=\"failure\"}))",
        "createExperiment_count_failure": "sum((kruizeAPI_count{api=\"createExperiment\",application=\"Kruize\",status=\"failure\"}))",
        "updateResults_count_failure": "sum((kruizeAPI_count{api=\"updateResults\",application=\"Kruize\",status=\"failure\"}))",
        "updateRecommendations_count_failure": "sum((kruizeAPI_count{api=\"updateRecommendations\",application=\"Kruize\",status=\"failure\"}))",
        "createBulkJob_count_failure": "sum((kruizeAPI_count{api=\"bulk\",application=\"Kruize\",method=\"createBulkJob\",status=\"failure\"}))",
        "jobStatus_count_failure": "sum((kruizeAPI_count{api=\"bulk\",application=\"Kruize\",method=\"jobStatus\",status=\"failure\"}))",
        "runBulkJob_count_failure": "sum((kruizeAPI_count{api=\"bulk\",application=\"Kruize\",method=\"runBulkJob\",status=\"failure\"}))",
        "importMetadata_count_failure": "sum((kruizeAPI_count{api=\"datasources\",application=\"Kruize\",method=\"importMetadata\",status=\"failure\"}))",
        "generatePlots_count_failure": "sum((KruizeMethod_count{method=\"generatePlots\",application=\"Kruize\",status=\"failure\"}))",
        "loadRecommendationsByExperimentName_count_failure": "sum((kruizeDB_count{method=\"loadRecommendationsByExperimentName\",application=\"Kruize\",status=\"failure\"}))",
        "loadRecommendationsByExperimentNameAndDate_count_failure": "sum((kruizeDB_count{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"failure\"}))",
        "loadResultsByExperimentName_count_failure": "sum((kruizeDB_count{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"failure\"}))",
        "loadExperimentByName_count_failure": "sum((kruizeDB_count{method=\"loadExperimentByName\",application=\"Kruize\",status=\"failure\"}))",
        "addRecommendationToDB_count_failure": "sum((kruizeDB_count{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"failure\"}))",
        "addResultToDB_count_failure": "sum((kruizeDB_count{method=\"addResultToDB\",application=\"Kruize\",status=\"failure\"}))",
        "addBulkResultsToDBAndFetchFailedResults_count_failure": "sum((kruizeDB_count{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"failure\"}))",
        "addExperimentToDB_count_failure": "sum((kruizeDB_count{method=\"addExperimentToDB\",application=\"Kruize\",status=\"failure\"}))",
        "addPerformanceProfileToDB_count_failure": "sum((kruizeDB_count{method=\"addPerformanceProfileToDB\",application=\"Kruize\",status=\"failure\"}))",
        "loadPerformanceProfileByName_count_failure": "sum((kruizeDB_count{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"failure\"}))",
        "loadAllPerformanceProfiles_count_failure": "sum((kruizeDB_count{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"failure\"}))",
        "listRecommendations_sum_success": "sum((kruizeAPI_sum{api=\"listRecommendations\",application=\"Kruize\",status=\"success\"}))",
        "listExperiments_sum_success": "sum((kruizeAPI_sum{api=\"listExperiments\",application=\"Kruize\",status=\"success\"}))",
        "createExperiment_sum_success": "sum((kruizeAPI_sum{api=\"createExperiment\",application=\"Kruize\",status=\"success\"}))",
        "updateResults_sum_success": "sum((kruizeAPI_sum{api=\"updateResults\",application=\"Kruize\",status=\"success\"}))",
        "updateRecommendations_sum_success": "sum((kruizeAPI_sum{api=\"updateRecommendations\",application=\"Kruize\",status=\"success\"}))",
        "createBulkJob_sum_success": "sum((kruizeAPI_sum{api=\"bulk\",application=\"Kruize\",method=\"createBulkJob\",status=\"success\"}))",
        "jobStatus_sum_success": "sum((kruizeAPI_sum{api=\"bulk\",application=\"Kruize\",method=\"jobStatus\",status=\"success\"}))",
        "bulk_getExperimentMap_sum_success": "sum((kruizeAPI_sum{api=\"bulk\",application=\"Kruize\",method=\"getExperimentMap\",status=\"success\"}))",
        "runBulkJob_sum_success": "sum((kruizeAPI_sum{api=\"bulk\",application=\"Kruize\",method=\"runBulkJob\",status=\"success\"}))",
        "importMetadata_sum_success": "sum((kruizeAPI_sum{api=\"datasources\",application=\"Kruize\",method=\"importMetadata\",status=\"success\"}))",
        "generatePlots_sum_success": "sum((KruizeMethod_sum{method=\"generatePlots\",application=\"Kruize\",status=\"success\"}))",
        "loadRecommendationsByExperimentName_sum_success": "sum((kruizeDB_sum{method=\"loadRecommendationsByExperimentName\",application=\"Kruize\",status=\"success\"}))",
        "loadRecommendationsByExperimentNameAndDate_sum_success": "sum((kruizeDB_sum{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"success\"}))",
        "loadResultsByExperimentName_sum_success": "sum((kruizeDB_sum{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"success\"}))",
        "loadExperimentByName_sum_success": "sum((kruizeDB_sum{method=\"loadExperimentByName\",application=\"Kruize\",status=\"success\"}))",
        "addRecommendationToDB_sum_success": "sum((kruizeDB_sum{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"success\"}))",
        "addResultToDB_sum_success": "sum((kruizeDB_sum{method=\"addResultToDB\",application=\"Kruize\",status=\"success\"}))",
        "addBulkResultsToDBAndFetchFailedResults_sum_success": "sum((kruizeDB_sum{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"success\"}))",
        "addExperimentToDB_sum_success": "sum((kruizeDB_sum{method=\"addExperimentToDB\",application=\"Kruize\",status=\"success\"}))",
        "addPerformanceProfileToDB_sum_success": "sum((kruizeDB_sum{method=\"addPerformanceProfileToDB\",application=\"Kruize\",status=\"success\"}))",
        "loadPerformanceProfileByName_sum_success": "sum((kruizeDB_sum{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"success\"}))",
        "loadAllPerformanceProfiles_sum_success": "sum((kruizeDB_sum{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"success\"}))",
        "listRecommendations_sum_failure": "sum((kruizeAPI_sum{api=\"listRecommendations\",application=\"Kruize\",status=\"failure\"}))",
        "listExperiments_sum_failure": "sum((kruizeAPI_sum{api=\"listExperiments\",application=\"Kruize\",status=\"failure\"}))",
        "createExperiment_sum_failure": "sum((kruizeAPI_sum{api=\"createExperiment\",application=\"Kruize\",status=\"failure\"}))",
        "updateResults_sum_failure": "sum((kruizeAPI_sum{api=\"updateResults\",application=\"Kruize\",status=\"failure\"}))",
        "updateRecommendations_sum_failure": "sum((kruizeAPI_sum{api=\"updateRecommendations\",application=\"Kruize\",status=\"failure\"}))",
        "createBulkJob_sum_failure": "sum((kruizeAPI_sum{api=\"bulk\",application=\"Kruize\",method=\"createBulkJob\",status=\"failure\"}))",
        "jobStatus_sum_failure": "sum((kruizeAPI_sum{api=\"bulk\",application=\"Kruize\",method=\"jobStatus\",status=\"failure\"}))",
        "bulk_getExperimentMap_sum_failure": "sum((kruizeAPI_sum{api=\"bulk\",application=\"Kruize\",method=\"getExperimentMap\",status=\"failure\"}))",
        "runBulkJob_sum_failure": "sum((kruizeAPI_sum{api=\"bulk\",application=\"Kruize\",method=\"runBulkJob\",status=\"failure\"}))",
        "importMetadata_sum_failure": "sum((kruizeAPI_sum{api=\"datasources\",application=\"Kruize\",method=\"importMetadata\",status=\"failure\"}))",
        "generatePlots_sum_failure": "sum((KruizeMethod_sum{method=\"generatePlots\",application=\"Kruize\",status=\"failure\"}))",
        "loadRecommendationsByExperimentName_sum_failure": "sum((kruizeDB_sum{method=\"loadRecommendationsByExperimentName\",application=\"Kruize\",status=\"failure\"}))",
        "loadRecommendationsByExperimentNameAndDate_sum_failure": "sum((kruizeDB_sum{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"failure\"}))",
        "loadResultsByExperimentName_sum_failure": "sum((kruizeDB_sum{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"failure\"}))",
        "loadExperimentByName_sum_failure": "sum((kruizeDB_sum{method=\"loadExperimentByName\",application=\"Kruize\",status=\"failure\"}))",
        "addRecommendationToDB_sum_failure": "sum((kruizeDB_sum{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"failure\"}))",
        "addResultToDB_sum_failure": "sum((kruizeDB_sum{method=\"addResultToDB\",application=\"Kruize\",status=\"failure\"}))",
        "addBulkResultsToDBAndFetchFailedResults_sum_failure": "sum((kruizeDB_sum{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"failure\"}))",
        "addExperimentToDB_sum_failure": "sum((kruizeDB_sum{method=\"addExperimentToDB\",application=\"Kruize\",status=\"failure\"}))",
        "addPerformanceProfileToDB_sum_failure": "sum((kruizeDB_sum{method=\"addPerformanceProfileToDB\",application=\"Kruize\",status=\"failure\"}))",
        "loadPerformanceProfileByName_sum_failure": "sum((kruizeDB_sum{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"failure\"}))",
        "loadAllPerformanceProfiles_sum_failure": "sum((kruizeDB_sum{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"failure\"}))",
        "loadAllRecommendations_sum_failure": "sum((kruizeDB_sum{method=\"loadAllRecommendations\",application=\"Kruize\",status=\"failure\"}))",
        "loadAllExperiments_sum_failure": "sum((kruizeDB_sum{method=\"loadAllExperiments\",application=\"Kruize\",status=\"failure\"}))",
        "loadAllResults_sum_failure": "sum((kruizeDB_sum{method=\"loadAllResults\",application=\"Kruize\",status=\"failure\"}))",
        "loadAllRecommendations_sum_success": "sum((kruizeDB_sum{method=\"loadAllRecommendations\",application=\"Kruize\",status=\"success\"}))",
        "loadAllExperiments_sum_success": "sum((kruizeDB_sum{method=\"loadAllExperiments\",application=\"Kruize\",status=\"success\"}))",
        "loadAllResults_sum_success": "sum((kruizeDB_sum{method=\"loadAllResults\",application=\"Kruize\",status=\"success\"}))",
        "listRecommendations_max_success": "max(max_over_time(kruizeAPI_max{{api=\"listRecommendations\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "listExperiments_max_success": "max(max_over_time(kruizeAPI_max{{api=\"listExperiments\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "createExperiment_max_success": "max(max_over_time(kruizeAPI_max{{api=\"createExperiment\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "updateResults_max_success": "max(max_over_time(kruizeAPI_max{{api=\"updateResults\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "updateRecommendations_max_success": "max(max_over_time(kruizeAPI_max{{api=\"updateRecommendations\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "createBulkJob_max_success": "max(max_over_time(kruizeAPI_max{{api=\"bulk\",application=\"Kruize\",method=\"createBulkJob\",status=\"success\"}}[6h]))",
        "jobStatus_max_success": "max(max_over_time(kruizeAPI_max{{api=\"bulk\",application=\"Kruize\",method=\"jobStatus\",status=\"success\"}}[6h]))",
        "bulk_getExperimentMap_max_success": "max(max_over_time(kruizeAPI_max{{api=\"bulk\",application=\"Kruize\",method=\"getExperimentMap\",status=\"success\"}}[6h]))",
        "runBulkJob_max_success": "max(max_over_time(kruizeAPI_max{{api=\"bulk\",application=\"Kruize\",method=\"runBulkJob\",status=\"success\"}}[6h]))",
        "importMetadata_max_success": "max(max_over_time(kruizeAPI_max{{api=\"datasources\",application=\"Kruize\",method=\"importMetadata\",status=\"success\"}}[6h]))",
        "generatePlots_max_success": "max(max_over_time(KruizeMethod_max{{method=\"generatePlots\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "loadRecommendationsByExperimentName_max_success": "max(max_over_time(kruizeDB_max{{method=\"loadRecommendationsByExperimentName\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "loadRecommendationsByExperimentNameAndDate_max_success": "max(max_over_time(kruizeDB_max{{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "loadResultsByExperimentName_max_success": "max(max_over_time(kruizeDB_max{{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "loadExperimentByName_max_success": "max(max_over_time(kruizeDB_max{{method=\"loadExperimentByName\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "addRecommendationToDB_max_success": "max(max_over_time(kruizeDB_max{{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "addResultToDB_max_success": "max(max_over_time(kruizeDB_max{{method=\"addResultToDB\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "addBulkResultsToDBAndFetchFailedResults_max_success": "max(max_over_time(kruizeDB_max{{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "addExperimentToDB_max_success": "max(max_over_time(kruizeDB_max{{method=\"addExperimentToDB\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "addPerformanceProfileToDB_max_success": "max(max_over_time(kruizeDB_max{{method=\"addPerformanceProfileToDB\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "loadPerformanceProfileByName_max_success": "max(max_over_time(kruizeDB_max{{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "loadAllPerformanceProfiles_max_success": "max(max_over_time(kruizeDB_max{{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"success\"}}[6h]))",
        "kruizedb_memory": "(sum(container_memory_working_set_bytes{pod=~"'"kruize-db-deployment-[^-]*-[^-]*$"'",container=\"kruize-db\"}))",
        "kruizedb_cpu_max": "max(sum(rate(container_cpu_usage_seconds_total{pod=~"'"kruize-db-deployment-[^-]*-[^-]*$"'",container=\"kruize-db\"}[6h])))",
        "kruize_memory": "(sum(container_memory_working_set_bytes{pod=~"'"kruize-[^-]*-[^-]*$"'",container=\"kruize\"}))",
        "kruize_cpu_max": "max(sum(rate(container_cpu_usage_seconds_total{pod=~"'"kruize-[^-]*-[^-]*$"'",container=\"kruize\"}[6h])))",
        "updateRecommendations_notifications_total": "sum((KruizeNotifications_total{api=\"updateRecommendations\",application=\"Kruize\"}))"
        }

def get_kruize_db_metrics(namespace):
    try:
        pod_name = subprocess.check_output(["kubectl", "get", "pods", "-n", namespace, "--selector=app=kruize-db", "-o", "jsonpath='{.items[0].metadata.name}'"], universal_newlines=True)
        pod_name = pod_name.strip("'")
    except subprocess.CalledProcessError as e:
        return f"Error getting pod name: {e}"
    # Queries
    PG_DB = "kruizeDB"
    PG_USER = "admin"
    KRUIZE_RESULTS_QUERY = f"psql -U {PG_USER} -d {PG_DB} -c \"select count(*) from kruize_results;\""
    DB_SIZE_QUERY = f"psql -U {PG_USER} -d {PG_DB} -c \"SELECT pg_size_pretty(pg_database_size('{PG_DB}'));\""
    try:
        kruize_results = subprocess.check_output(["kubectl", "exec", "-it", pod_name, "-n", namespace, "--", "/bin/sh", "-c", KRUIZE_RESULTS_QUERY], universal_newlines=True)
        db_size = subprocess.check_output(["kubectl", "exec", "-it", pod_name, "-n", namespace, "--", "/bin/sh", "-c", DB_SIZE_QUERY], universal_newlines=True)
        # Extract numeric values
        kruize_results_numeric = ''.join(kruize_results.split()).split('-')[-1].split('(')[0]
        db_size_numeric = ''.join(db_size.split()).split('-')[-1].split('(')[0]
        return kruize_results_numeric,db_size_numeric
    except subprocess.CalledProcessError as e:
        return f"Error executing queries: {e}"

def run_queries(map_type,server,prometheus_url=None):
    TOKEN = 'TOKEN'
    if prometheus_url is None:
        if cluster_type == "openshift":
            prometheus_url = f"https://thanos-querier-openshift-monitoring.apps.{server}/api/v1/query"
        elif cluster_type == "minikube":
            prometheus_url = f"http://{server}:9090/api/v1/query"

    if cluster_type == "openshift":
        output = subprocess.check_output(['oc', 'whoami', '--show-token'])
        TOKEN = output.decode().strip()
        # Disable the InsecureRequestWarning
        requests.packages.urllib3.disable_warnings(requests.packages.urllib3.exceptions.InsecureRequestWarning)

    headers = {'Authorization': f'Bearer {TOKEN}'}
    print("RUNNING THE QUERIES NOW")

    results_map = {}
    results_data = {}
    if map_type == "increase":
        queries_data = queries_map.items()
    else:
        queries_data = queries_map_total.items()

    try:
        for key, query in queries_data:
            response = requests.get(prometheus_url, headers=headers, params={'query': query}, verify=False)
            if response.status_code == 200:
                results_data[key] = response.json()['data']
                if "result" in results_data[key] and isinstance(results_data[key]["result"], list) and len(results_data[key]["result"]) > 0:
                    if "value" in results_data[key]["result"][0]:
                        results_map[key] = results_data[key]["result"][0]["value"][1]
                # Uncomment else part to debug which query is not working.
                #else:
                #    print(f"Failed to run query '{query}' with status code {response.status_code}")
        kruize_results, db_size = get_kruize_db_metrics(namespace)
        results_map["kruize_results"] = kruize_results
        results_map["db_size"] = db_size
        if "updateResults_sum_success" in results_map and "updateResults_count_success" in results_map:
            if results_map["updateResults_count_success"] and results_map["updateResults_sum_success"]:
                try:
                    sum_success = round(float(results_map["updateResults_sum_success"]),10)
                    count_success = round(float(results_map["updateResults_count_success"]),10)
                    if count_success != 0:
                        results_map["updateResultsPerCall_success"] = sum_success / count_success
                except ValueError:
                    print("Error: Unable to convert values to floats.")
        if "updateRecommendations_sum_success" in results_map and "updateRecommendations_count_success" in results_map:
            if results_map["updateRecommendations_count_success"] and results_map["updateRecommendations_sum_success"]:
                try:
                    sum_success = round(float(results_map["updateRecommendations_sum_success"]),10)
                    count_success = round(float(results_map["updateRecommendations_count_success"]),10)
                    if count_success != 0:
                        results_map["updateRecommendationsPerCall_success"] = sum_success / count_success
                except ValueError:
                    print("Error: Unable to convert values to floats.")
        if "runBulkJob_sum_success" in results_map and "runBulkJob_count_success" in results_map:
                    if results_map["runBulkJob_sum_success"] and results_map["runBulkJob_count_success"]:
                        try:
                            sum_success = round(float(results_map["runBulkJob_sum_success"]),10)
                            count_success = round(float(results_map["runBulkJob_count_success"]),10)
                            if count_success != 0:
                                results_map["BulkJobPerCall_success"] = sum_success / count_success
                        except ValueError:
                            print("Error: Unable to convert values to floats.")
    except Exception as e:
        print(f"AN ERROR OCCURED: {e}")
        sys.exit(1) 
    return results_map


def write_header_to_csv(filename):
    if not os.path.exists(filename) or os.path.getsize(filename) == 0:
        with open(filename, 'w') as f:
            writer = csv.DictWriter(f, fieldnames=csv_headers)
            writer.writeheader()

def job(queries_type,outputdir,server,prometheus_url=None):
    now_utc = datetime.utcnow()
    timestamp_utc = now_utc.isoformat()
    if queries_type == "increase":
        outputfile = os.path.join(outputdir, "increase_" + resultsfile)
        print("====================================================")
        print("RUNNING THE JOB TO COLLECT KRUIZE INCREASE METRICS..")
        results_map = run_queries("increase",server,prometheus_url)
    elif queries_type == "total":
        outputfile = os.path.join(outputdir, "total_" + resultsfile)
        print("====================================================")
        print("RUNNING THE JOB TO COLLECT KRUIZE TOTAL METRICS..")
        results_map = run_queries("total",server,prometheus_url)
    results_map['timestamp'] = timestamp_utc
    write_header_to_csv(outputfile)
    with open(outputfile, 'a') as f:
        writer = csv.DictWriter(f, fieldnames=csv_headers)
        writer.writerow(results_map)

def schedule_job(queries_type,server,prometheus_url):
    if not os.path.exists(outputdir):
        os.mkdir(outputdir)
    numeric_time = int(time_duration[:-1])
    time_in_seconds = numeric_time * 60
    if getOneDataPoint == "true" and duration is None:
        print("COLLECTING THE METRICS FOR ONE TIME. METRICS DATA WILL BE AVAILABLE IN \"results\" DIRECTORY IN CSV FORMAT")
        job("increase",outputdir,server,prometheus_url)
        job("total",outputdir,server,prometheus_url)
   
    if duration is not None:
        print("COLLECTING THE METRICS FOR ", duration, " HOURS WITH AN INTERVAL OF ", time_in_seconds, " SECONDS")
        print("METRICS DATA WILL BE AVAILABLE IN \"results\" DIRECTORY IN CSV FORMAT")
        now = datetime.utcnow()
        end = now + timedelta(hours=int(duration))
        while now < end:
            now = datetime.utcnow()
            job(queries_type,outputdir,server,prometheus_url)
            print("SLEEPING FOR ",time_in_seconds, " SECONDS")
            time.sleep(time_in_seconds)
    print("====================================================")
    print("COLLECTION OF METRICS IS COMPLETED")
    print("RESULTS ARE AVAILABLE IN CSV FORMAT IN RESULTS DIRECTORY")
    print("RESULTS FOR INCREASE METRICS AT results/increase_kruizemetrics.csv")
    print("RESULTS FOR TOTAL METRICS AT results/total_kruizemetrics.csv")

def main(argv):
    global duration
    global cluster_type
    global server
    global clusterResults
    global time_duration 
    global queries_map
    global getOneDataPoint
    global resultsfile
    global namespace
    global prometheus_url
    global outputdir

    parser = argparse.ArgumentParser(description='kruize_metrics.py -c <cluster_type> -s <cluster_name> -p <prometheus_url> -t <time duration for a query in mins:Default:60m> -d <duration the script runs in hours> -q <query_type:increase/total.Default:increase> -o <single data point:Default:true> -e <results dir:Default:results')
    parser.add_argument('-c', '--cluster_type', help='Cluster type. Supported types:openshift/minikube')
    parser.add_argument('-s', '--cluster_name', help='Name/IP to access the openshift/minikube cluster. Example:kruize-rm.p1.openshiftapps.com/localhost. Prometheus URL is generated using this name if prometheus_url is None')
    parser.add_argument('-p', '--prometheus_url', help='Prometheus URL',default=None)
    parser.add_argument('-t', '--time', help='Time duration for a query in mins', default='60m')
    parser.add_argument('-d', '--duration', help='Duration for the script to run:value in hours')
    parser.add_argument('-q', '--queries_type', help='Query type: increase/total', default='increase')
    parser.add_argument('-o', '--get_one_data_point', help='Single data point', default='true')
    parser.add_argument('-r', '--resultsfile', help='Results file',default='kruizemetrics.csv')
    parser.add_argument('-e', '--outputdir', help='directory to store the results', default='results')
    #args = parser.parse_args()
    args, unknown = parser.parse_known_args()

    if not args.cluster_type:
        print("CLUSTER_TYPE is required.")
        parser.print_help()
        parser.exit()

    if not (args.cluster_name or args.prometheus_url):
        print("Either CLUSTER_NAME or PROMETHEUS_URL is required.")
        parser.print_help()
        parser.exit()

    # Access the arguments using the dot operator
    cluster_type = args.cluster_type
    server = args.cluster_name
    duration = args.duration
    time_duration = args.time
    queries_type = args.queries_type
    getOneDataPoint = args.get_one_data_point
    resultsfile = args.resultsfile
    prometheus_url = args.prometheus_url
    outputdir = args.outputdir

    if cluster_type == "openshift":
        namespace = "openshift-tuning"
    else:
        namespace = "monitoring"

    queries_map = {
            "listRecommendations_count_success": f"sum(increase(kruizeAPI_count{{api=\"listRecommendations\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "listExperiments_count_success": f"sum(increase(kruizeAPI_count{{api=\"listExperiments\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "createExperiment_count_success": f"sum(increase(kruizeAPI_count{{api=\"createExperiment\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "updateResults_count_success": f"sum(increase(kruizeAPI_count{{api=\"updateResults\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "updateRecommendations_count_success": f"sum(increase(kruizeAPI_count{{api=\"updateRecommendations\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "createBulkJob_count_success": f"sum(increase(kruizeAPI_count{{api=\"bulk\",application=\"Kruize\",method=\"createBulkJob\",status=\"success\"}}[{time_duration}]))",
            "bulkJobs_count_running": f"sum(increase(kruizeAPI_active_jobs_count{{api=\"bulk\",application=\"Kruize\",method=\"runBulkJob\",status=\"running\"}}[{time_duration}]))",
            "jobStatus_count_success": f"sum(increase(kruizeAPI_count{{api=\"bulk\",application=\"Kruize\",method=\"jobStatus\",status=\"success\"}}[{time_duration}]))",
            "bulk_getExperimentMap_count_success": f"sum(increase(kruizeAPI_count{{api=\"bulk\",application=\"Kruize\",method=\"getExperimentMap\",status=\"success\"}}[{time_duration}]))",
            "runBulkJob_count_success": f"sum(increase(kruizeAPI_count{{api=\"bulk\",application=\"Kruize\",method=\"runBulkJob\",status=\"success\"}}[{time_duration}]))",
            "importMetadata_count_success": f"sum(increase(kruizeAPI_count{{api=\"datasources\",application=\"Kruize\",method=\"importMetadata\",status=\"success\"}}[{time_duration}]))",
            "generatePlots_count_success": f"sum(increase(KruizeMethod_count{{method=\"generatePlots\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadRecommendationsByExperimentName_count_success": f"sum(increase(kruizeDB_count{{method=\"loadRecommendationsByExperimentName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadRecommendationsByExperimentNameAndDate_count_success": f"sum(increase(kruizeDB_count{{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadResultsByExperimentName_count_success": f"sum(increase(kruizeDB_count{{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadExperimentByName_count_success": f"sum(increase(kruizeDB_count{{method=\"loadExperimentByName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addRecommendationToDB_count_success": f"sum(increase(kruizeDB_count{{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addResultToDB_count_success": f"sum(increase(kruizeDB_count{{method=\"addResultToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addBulkResultsToDBAndFetchFailedResults_count_success": f"sum(increase(kruizeDB_count{{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addExperimentToDB_count_success": f"sum(increase(kruizeDB_count{{method=\"addExperimentToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addPerformanceProfileToDB_count_success": f"sum(increase(kruizeDB_count{{method=\"addPerformanceProfileToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadPerformanceProfileByName_count_success": f"sum(increase(kruizeDB_count{{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadAllPerformanceProfiles_count_success": f"sum(increase(kruizeDB_count{{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "listRecommendations_count_failure": f"sum(increase(kruizeAPI_count{{api=\"listRecommendations\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "listExperiments_count_failure": f"sum(increase(kruizeAPI_count{{api=\"listExperiments\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "createExperiment_count_failure": f"sum(increase(kruizeAPI_count{{api=\"createExperiment\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "updateResults_count_failure": f"sum(increase(kruizeAPI_count{{api=\"updateResults\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "updateRecommendations_count_failure": f"sum(increase(kruizeAPI_count{{api=\"updateRecommendations\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "createBulkJob_count_failure": f"sum(increase(kruizeAPI_count{{api=\"bulk\",application=\"Kruize\",method=\"createBulkJob\",status=\"failure\"}}[{time_duration}]))",
            "jobStatus_count_failure": f"sum(increase(kruizeAPI_count{{api=\"bulk\",application=\"Kruize\",method=\"jobStatus\",status=\"failure\"}}[{time_duration}]))",
            "runBulkJob_count_failure": f"sum(increase(kruizeAPI_count{{api=\"bulk\",application=\"Kruize\",method=\"runBulkJob\",status=\"failure\"}}[{time_duration}]))",
            "importMetadata_count_failure": f"sum(increase(kruizeAPI_count{{api=\"datasources\",application=\"Kruize\",method=\"importMetadata\",status=\"failure\"}}[{time_duration}]))",
            "generatePlots_count_failure": f"sum(increase(KruizeMethod_count{{method=\"generatePlots\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "loadRecommendationsByExperimentName_count_failure": f"sum(increase(kruizeDB_count{{method=\"loadRecommendationsByExperimentName\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "loadRecommendationsByExperimentNameAndDate_count_failure": f"sum(increase(kruizeDB_count{{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "loadResultsByExperimentName_count_failure": f"sum(increase(kruizeDB_count{{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "loadExperimentByName_count_failure": f"sum(increase(kruizeDB_count{{method=\"loadExperimentByName\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "addRecommendationToDB_count_failure": f"sum(increase(kruizeDB_count{{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "addResultToDB_count_failure": f"sum(increase(kruizeDB_count{{method=\"addResultToDB\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "addBulkResultsToDBAndFetchFailedResults_count_failure": f"sum(increase(kruizeDB_count{{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "addExperimentToDB_count_failure": f"sum(increase(kruizeDB_count{{method=\"addExperimentToDB\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "addPerformanceProfileToDB_count_failure": f"sum(increase(kruizeDB_count{{method=\"addPerformanceProfileToDB\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "loadPerformanceProfileByName_count_failure": f"sum(increase(kruizeDB_count{{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "loadAllPerformanceProfiles_count_failure": f"sum(increase(kruizeDB_count{{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
            "listRecommendations_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"listRecommendations\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "listExperiments_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"listExperiments\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "createExperiment_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"createExperiment\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "updateResults_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"updateResults\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "updateRecommendations_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"updateRecommendations\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "createBulkJob_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"bulk\",application=\"Kruize\",method=\"createBulkJob\",status=\"success\"}}[{time_duration}]))",
            "jobStatus_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"bulk\",application=\"Kruize\",method=\"jobStatus\",status=\"success\"}}[{time_duration}]))",
            "bulk_getExperimentMap_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"bulk\",application=\"Kruize\",method=\"getExperimentMap\",status=\"success\"}}[{time_duration}]))",
            "runBulkJob_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"bulk\",application=\"Kruize\",method=\"runBulkJob\",status=\"success\"}}[{time_duration}]))",
            "importMetadata_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"datasources\",application=\"Kruize\",method=\"importMetadata\",status=\"success\"}}[{time_duration}]))",
            "generatePlots_sum_success": f"sum(increase(KruizeMethod_sum{{method=\"generatePlots\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadRecommendationsByExperimentName_sum_success": f"sum(increase(kruizeDB_sum{{method=\"loadRecommendationsByExperimentName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadRecommendationsByExperimentNameAndDate_sum_success": f"sum(increase(kruizeDB_sum{{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadResultsByExperimentName_sum_success": f"sum(increase(kruizeDB_sum{{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadExperimentByName_sum_success": f"sum(increase(kruizeDB_sum{{method=\"loadExperimentByName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addRecommendationToDB_sum_success": f"sum(increase(kruizeDB_sum{{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addResultToDB_sum_success": f"sum(increase(kruizeDB_sum{{method=\"addResultToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addBulkResultsToDBAndFetchFailedResults_sum_success": f"sum(increase(kruizeDB_sum{{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addExperimentToDB_sum_success": f"sum(increase(kruizeDB_sum{{method=\"addExperimentToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addPerformanceProfileToDB_sum_success": f"sum(increase(kruizeDB_sum{{method=\"addPerformanceProfileToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadPerformanceProfileByName_sum_success": f"sum(increase(kruizeDB_sum{{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadAllPerformanceProfiles_sum_success": f"sum(increase(kruizeDB_sum{{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "listRecommendations_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"listRecommendations\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "listExperiments_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"listExperiments\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "createExperiment_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"createExperiment\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "updateResults_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"updateResults\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "updateRecommendations_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"updateRecommendations\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "createBulkJob_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"bulk\",application=\"Kruize\",method=\"createBulkJob\",status=\"success\"}}[{time_duration}]))",
            "jobStatus_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"bulk\",application=\"Kruize\",method=\"jobStatus\",status=\"success\"}}[{time_duration}]))",
            "bulk_getExperimentMap_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"bulk\",application=\"Kruize\",method=\"getExperimentMap\",status=\"success\"}}[{time_duration}]))",
            "runBulkJob_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"bulk\",application=\"Kruize\",method=\"runBulkJob\",status=\"success\"}}[{time_duration}]))",
            "importMetadata_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"datasources\",application=\"Kruize\",method=\"importMetadata\",status=\"success\"}}[{time_duration}]))",
            "generatePlots_max_success": f"max(max_over_time(KruizeMethod_max{{method=\"generatePlots\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadRecommendationsByExperimentName_max_success": f"max(max_over_time(kruizeDB_max{{method=\"loadRecommendationsByExperimentName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadRecommendationsByExperimentNameAndDate_max_success": f"max(max_over_time(kruizeDB_max{{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadResultsByExperimentName_max_success": f"max(max_over_time(kruizeDB_max{{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadExperimentByName_max_success": f"max(max_over_time(kruizeDB_max{{method=\"loadExperimentByName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addRecommendationToDB_max_success": f"max(max_over_time(kruizeDB_max{{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addResultToDB_max_success": f"max(max_over_time(kruizeDB_max{{method=\"addResultToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addBulkResultsToDBAndFetchFailedResults_max_success": f"max(max_over_time(kruizeDB_max{{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addExperimentToDB_max_success": f"max(max_over_time(kruizeDB_max{{method=\"addExperimentToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "addPerformanceProfileToDB_max_success": f"max(max_over_time(kruizeDB_max{{method=\"addPerformanceProfileToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadPerformanceProfileByName_max_success": f"max(max_over_time(kruizeDB_max{{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "loadAllPerformanceProfiles_max_success": f"max(max_over_time(kruizeDB_max{{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "kruizedb_memory": "(sum(container_memory_working_set_bytes{pod=~"'"kruize-db-deployment-[^-]*-[^-]*$"'",container=\"kruize-db\"}))",
            "kruizedb_cpu_max": "max(sum(rate(container_cpu_usage_seconds_total{pod=~"'"kruize-db-deployment-[^-]*-[^-]*$"'",container=\"kruize-db\"}"f"[{time_duration}])))",
            "kruize_memory": "(sum(container_memory_working_set_bytes{pod=~"'"kruize-[^-]*-[^-]*$"'",container=\"kruize\"}))",
            "kruize_cpu_max": "max(sum(rate(container_cpu_usage_seconds_total{pod=~"'"kruize-[^-]*-[^-]*$"'",container=\"kruize\"}"f"[{time_duration}])))",
            "updateRecommendations_notifications_total": "sum((KruizeNotifications_total{api=\"updateRecommendations\",application=\"Kruize\"}))"
        }
    
    # Create a thread to run the job scheduler
    job_thread = threading.Thread(target=schedule_job(queries_type,server,prometheus_url))
    job_thread.start()
    job_thread.join()

if __name__ == '__main__':
    main(sys.argv[1:])
