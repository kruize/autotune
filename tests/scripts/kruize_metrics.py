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

csv_headers = ["timestamp","listRecommendations_count_success","listExperiments_count_success","createExperiment_count_success","updateResults_count_success","updateRecommendations_count_success","loadRecommendationsByExperimentName_count_success","loadRecommendationsByExperimentNameAndDate_count_success","loadResultsByExperimentName_count_success","loadExperimentByName_count_success","addRecommendationToDB_count_success","addResultToDB_count_success","addBulkResultsToDBAndFetchFailedResults_count_success","addExperimentToDB_count_success","addPerformanceProfileToDB_count_success","loadPerformanceProfileByName_count_success","loadAllPerformanceProfiles_count_success","listRecommendations_count_failure","listExperiments_count_failure","createExperiment_count_failure","updateResults_count_failure","updateRecommendations_count_failure","loadRecommendationsByExperimentName_count_failure","loadRecommendationsByExperimentNameAndDate_count_failure","loadResultsByExperimentName_count_failure","loadExperimentByName_count_failure","addRecommendationToDB_count_failure","addResultToDB_count_failure","addBulkResultsToDBAndFetchFailedResults_count_failure","addExperimentToDB_count_failure","addPerformanceProfileToDB_count_failure","loadPerformanceProfileByName_count_failure","loadAllPerformanceProfiles_count_failure","listRecommendations_sum_success","listExperiments_sum_success","createExperiment_sum_success","updateResults_sum_success","updateRecommendations_sum_success","loadRecommendationsByExperimentName_sum_success","loadRecommendationsByExperimentNameAndDate_sum_success","loadResultsByExperimentName_sum_success","loadExperimentByName_sum_success","addRecommendationToDB_sum_success","addResultToDB_sum_success","addBulkResultsToDBAndFetchFailedResults_sum_success","addExperimentToDB_sum_success","addPerformanceProfileToDB_sum_success","loadPerformanceProfileByName_sum_success","loadAllPerformanceProfiles_sum_success","listRecommendations_sum_failure","listExperiments_sum_failure","createExperiment_sum_failure","updateResults_sum_failure","updateRecommendations_sum_failure","loadRecommendationsByExperimentName_sum_failure","loadRecommendationsByExperimentNameAndDate_sum_failure","loadResultsByExperimentName_sum_failure","loadExperimentByName_sum_failure","addRecommendationToDB_sum_failure","addResultToDB_sum_failure","addBulkResultsToDBAndFetchFailedResults_sum_failure","addExperimentToDB_sum_failure","addPerformanceProfileToDB_sum_failure","loadPerformanceProfileByName_sum_failure","loadAllPerformanceProfiles_sum_failure","loadAllRecommendations_sum_failure","loadAllExperiments_sum_failure","loadAllResults_sum_failure","loadAllRecommendations_sum_success","loadAllExperiments_sum_success","loadAllResults_sum_success","kruizedb_cpu_max","kruizedb_memory","kruize_cpu_max","kruize_memory","kruize_results","db_size"]

queries_map_total = {
        "listRecommendations_count_success": "sum((kruizeAPI_count{api=\"listRecommendations\",application=\"Kruize\",status=\"success\"}))",
        "listExperiments_count_success": "sum((kruizeAPI_count{api=\"listExperiments\",application=\"Kruize\",status=\"success\"}))",
        "createExperiment_count_success": "sum((kruizeAPI_count{api=\"createExperiment\",application=\"Kruize\",status=\"success\"}))",
        "updateResults_count_success": "sum((kruizeAPI_count{api=\"updateResults\",application=\"Kruize\",status=\"success\"}))",
        "updateRecommendations_count_success": "sum((kruizeAPI_count{api=\"updateRecommendations\",application=\"Kruize\",status=\"success\"}))",
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
        "kruizedb_memory": "(sum(container_memory_working_set_bytes{pod=~"'"postgres-deployment-[^-]*-[^-]*$"'",container=\"postgres\"}))",
        "kruizedb_cpu_max": "max(sum(rate(container_cpu_usage_seconds_total{pod=~"'"postgres-deployment-[^-]*-[^-]*$"'",container=\"postgres\"}[6h])))",
        "kruize_memory": "(sum(container_memory_working_set_bytes{pod=~"'"kruize-[^-]*-[^-]*$"'",container=\"kruize\"}))",
        "kruize_cpu_max": "max(sum(rate(container_cpu_usage_seconds_total{pod=~"'"kruize-[^-]*-[^-]*$"'",container=\"kruize\"}[6h])))",
        }

def get_postgresql_metrics(namespace):
    try:
        pod_name = subprocess.check_output(["kubectl", "get", "pods", "-n", namespace, "--selector=app=postgres", "-o", "jsonpath='{.items[0].metadata.name}'"], universal_newlines=True)
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

def run_queries(map_type):
    TOKEN = 'TOKEN'
    prometheus_url = None
    if cluster_type == "openshift":
        output = subprocess.check_output(['oc', 'whoami', '--show-token'])
        TOKEN = output.decode().strip()
        prometheus_url = f"https://thanos-querier-openshift-monitoring.apps.{server}/api/v1/query"
        # Disable the InsecureRequestWarning
        requests.packages.urllib3.disable_warnings(requests.packages.urllib3.exceptions.InsecureRequestWarning)

    elif cluster_type == "minikube":
        prometheus_url = f"http://{server}:9090/api/v1/query"
    headers = {'Authorization': f'Bearer {TOKEN}'}

    print("RUNNING THE QUERIES NOW")

    results_map = {}
    results_data = {}
    if map_type == "increase":
        queries_data = queries_map.items()
    else:
        queries_data = queries_map_total.items()
    for key, query in queries_data:
        response = requests.get(prometheus_url, headers=headers, params={'query': query}, verify=False)
        if response.status_code == 200:
            results_data[key] = response.json()['data']
            if "result" in results_data[key] and isinstance(results_data[key]["result"], list) and len(results_data[key]["result"]) > 0:
                if "value" in results_data[key]["result"][0]:
                    results_map[key] = results_data[key]["result"][0]["value"][1]
            # Uncomment else part to debug which query is not working.
            else:
                print(f"Failed to run query '{query}' with status code {response.status_code}")
    kruize_results, db_size = get_postgresql_metrics(namespace)
    results_map["kruize_results"] = kruize_results
    results_map["db_size"] = db_size
    return results_map


def write_header_to_csv(filename):
    if not os.path.exists(filename) or os.path.getsize(filename) == 0:
        with open(filename, 'w') as f:
            writer = csv.DictWriter(f, fieldnames=csv_headers)
            writer.writeheader()

def job(queries_type,outputdir):
    now_utc = datetime.utcnow()
    timestamp_utc = now_utc.isoformat()
    if queries_type == "increase":
        outputfile = os.path.join(outputdir, "increase_" + resultsfile)
        print("RUNNING THE JOB TO COLLECT KRUIZE INCREASE METRICS..")
        results_map = run_queries("increase")
    elif queries_type == "total":
        outputfile = os.path.join(outputdir, "total_" + resultsfile)
        print("RUNNING THE JOB TO COLLECT KRUIZE TOTAL METRICS..")
        results_map = run_queries("total")
    results_map['timestamp'] = timestamp_utc
    write_header_to_csv(outputfile)
    with open(outputfile, 'a') as f:
        writer = csv.DictWriter(f, fieldnames=csv_headers)
        writer.writerow(results_map)

def schedule_job(queries_type):
    outputdir = "results"
    if not os.path.exists(outputdir):
        os.mkdir(outputdir)
    numeric_time = int(time_duration[:-1])
    time_in_seconds = numeric_time * 60
    if getOneDataPoint == "true":
        job("increase",outputdir)
        job("total",outputdir)
    else:
        if duration is None:
            while True:
                job(queries_type,outputdir)
                print("Sleep for ",time_in_seconds, " seconds")
                time.sleep(time_in_seconds)
        else:
            now = datetime.utcnow()
            end = now + timedelta(hours=duration)
            while now < end:
                now = datetime.utcnow()
                job(queries_type,outputdir) 
                print("Sleep for ",time_in_seconds, " seconds")
                time.sleep(time_in_seconds) 

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

    try:
        opts, args = getopt.getopt(argv,"h:c:s:d:t:q:p:r:")
    except getopt.GetoptError:
        print("kruize_metrics.py -c <cluster type> -s <server>")
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print("kruize_metrics.py -c <cluster type> -s <server> -t <time duration for a query in mins:Default:60m> -d <duration the script runs in hours:Default:None> -q <query_type:increase/total.Default:increase>")
            sys.exit()
        elif opt == '-c':
            cluster_type = arg
        elif opt == '-s':
            server = arg
        elif opt == '-d':
            duration = arg
        elif opt == '-t':
            time_duration = arg
        elif opt == '-q':
            queries_type = arg
        elif opt == '-p':
            getOneDataPoint = arg
        elif opt == '-r':
            resultsfile = arg
            
    if '-t' not in sys.argv:
        time_duration = "60m"
    if '-q' not in sys.argv:
        queries_type = "increase"
    if '-d' not in sys.argv:
        duration = None
    if '-p' not in sys.argv:
        getOneDataPoint = "false"
    if '-r' not in sys.argv:
        resultsfile = "kruizemetrics.csv"

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
            "kruizedb_memory": "(sum(container_memory_working_set_bytes{pod=~"'"postgres-deployment-[^-]*-[^-]*$"'",container=\"postgres\"}))",
            "kruizedb_cpu_max": "max(sum(rate(container_cpu_usage_seconds_total{pod=~"'"postgres-deployment-[^-]*-[^-]*$"'",container=\"postgres\"}"f"[{time_duration}])))",
            "kruize_memory": "(sum(container_memory_working_set_bytes{pod=~"'"kruize-[^-]*-[^-]*$"'",container=\"kruize\"}))",
            "kruize_cpu_max": "max(sum(rate(container_cpu_usage_seconds_total{pod=~"'"kruize-[^-]*-[^-]*$"'",container=\"kruize\"}"f"[{time_duration}])))",
        }
    
    # Create a thread to run the job scheduler
    job_thread = threading.Thread(target=schedule_job(queries_type))
    job_thread.start()
    job_thread.join()

if __name__ == '__main__':
    main(sys.argv[1:])
