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

csv_headers = ['report_period_start','report_period_end','interval_start','interval_end','updateRecommendations_count_success', 'updateResults_count_success', 'createExperiment_count_success', 'listExperiments_count_success', 'addRecommendationToDB_count_success', 'loadAllPerformanceProfiles_count_success', 'loadExperimentByName_count_success', 'loadResultsByExperimentName_count_success', 'loadPerformanceProfileByName_count_success', 'loadRecommendationsByExperimentNameAndDate_count_success', 'addBulkResultsToDBAndFetchFailedResults_count_success', 'loadRecommendationsByExperimentName_count_success', 'addExperimentToDB_count_success', 'updateRecommendations_count_failure', 'updateResults_count_failure', 'createExperiment_count_failure', 'listExperiments_count_failure', 'addRecommendationToDB_count_failure', 'loadAllPerformanceProfiles_count_failure', 'loadExperimentByName_count_failure', 'loadResultsByExperimentName_count_failure', 'loadPerformanceProfileByName_count_failure', 'loadRecommendationsByExperimentNameAndDate_count_failure', 'addBulkResultsToDBAndFetchFailedResults_count_failure', 'loadRecommendationsByExperimentName_count_failure', 'addExperimentToDB_count_failure', 'updateRecommendations_sum_success', 'updateResults_sum_success', 'createExperiment_sum_success', 'listExperiments_sum_success', 'addRecommendationToDB_sum_success', 'loadAllPerformanceProfiles_sum_success', 'loadExperimentByName_sum_success', 'loadResultsByExperimentName_sum_success', 'loadPerformanceProfileByName_sum_success', 'loadRecommendationsByExperimentNameAndDate_sum_success', 'addBulkResultsToDBAndFetchFailedResults_sum_success', 'loadRecommendationsByExperimentName_sum_success', 'addExperimentToDB_sum_success', 'updateRecommendations_sum_failure', 'updateResults_sum_failure', 'createExperiment_sum_failure', 'listExperiments_sum_failure', 'addRecommendationToDB_sum_failure', 'loadAllPerformanceProfiles_sum_failure', 'loadExperimentByName_sum_failure', 'loadResultsByExperimentName_sum_failure', 'loadPerformanceProfileByName_sum_failure', 'loadRecommendationsByExperimentNameAndDate_sum_failure', 'addBulkResultsToDBAndFetchFailedResults_sum_failure', 'loadRecommendationsByExperimentName_sum_failure', 'addExperimentToDB_sum_failure']

queries_map_total = {
        "updateRecommendations_sum_success": "sum((kruizeAPI_sum{api=\"updateRecommendations\",application=\"Kruize\",status=\"success\"}))",
        "updateRecommendations_sum_failure": "sum((kruizeAPI_sum{api=\"updateRecommendations\",application=\"Kruize\",status=\"failure\"}))",
        "updateResults_sum_success": "sum((kruizeAPI_sum{api=\"updateResults\",application=\"Kruize\",status=\"success\"}))",
        "updateResults_sum_failure": "sum((kruizeAPI_sum{api=\"updateResults\",application=\"Kruize\",status=\"failure\"}))",
        "createExperiment_sum_success": "sum((kruizeAPI_sum{api=\"createExperiment\",application=\"Kruize\",status=\"success\"}))",
        "createExperiment_sum_failure": "sum((kruizeAPI_sum{api=\"createExperiment\",application=\"Kruize\",status=\"failure\"}))",
        "listExperiments_sum_success": "sum((kruizeAPI_sum{api=\"listExperiments\",application=\"Kruize\",status=\"success\"}))",
        "listExperiments_sum_failure": "sum((kruizeAPI_sum{api=\"listExperiments\",application=\"Kruize\",status=\"failure\"}))",
        "addRecommendationToDB_sum_success": "sum((kruizeDB_sum{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"success\"}))",
        "addRecommendationToDB_sum_failure": "sum((kruizeDB_sum{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"failure\"}))",
        "loadAllPerformanceProfiles_sum_success": "sum((kruizeDB_sum{api=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"success\"}))",
        "loadAllPerformanceProfiles_sum_failure": "sum((kruizeDB_sum{api=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"failure\"}))",
        "loadExperimentByName_sum_success": "sum((kruizeDB_sum{method=\"loadExperimentByName\",application=\"Kruize\",status=\"success\"}))",
        "loadExperimentByName_sum_failure": "sum((kruizeDB_sum{method=\"loadExperimentByName\",application=\"Kruize\",status=\"failure\"}))",
        "loadResultsByExperimentName_sum_success": "sum((kruizeDB_sum{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"success\"}))",
        "loadResultsByExperimentName_sum_failure": "sum((kruizeDB_sum{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"failure\"}))",
        "loadPerformanceProfileByName_sum_success": "sum((kruizeDB_sum{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"success\"}))",
        "loadPerformanceProfileByName_sum_failure": "sum((kruizeDB_sum{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"failure\"}))",
        "loadRecommendationsByExperimentNameAndDate_sum_success": "sum((kruizeDB_sum{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"success\"}))",
        "loadRecommendationsByExperimentNameAndDate_sum_failure": "sum((kruizeDB_sum{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"failure\"}))",
        "addBulkResultsToDBAndFetchFailedResults_sum_success": "sum((kruizeDB_sum{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"success\"}))",
        "addBulkResultsToDBAndFetchFailedResults_sum_failure": "sum((kruizeDB_sum{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"failure\"}))",
        "addExperimentToDB_sum_success": "sum((kruizeDB_sum{method=\"addExperimentToDB\",application=\"Kruize\",status=\"success\"}))",
        "addExperimentToDB_sum_failure": "sum((kruizeDB_sum{method=\"addExperimentToDB\",application=\"Kruize\",status=\"failure\"}))",
        "updateRecommendations_count_success": "sum((kruizeAPI_count{api=\"updateRecommendations\",application=\"Kruize\",status=\"success\"}))",
        "updateRecommendations_count_failure": "sum((kruizeAPI_count{api=\"updateRecommendations\",application=\"Kruize\",status=\"failure\"}))",
        "updateResults_count_success": "sum((kruizeAPI_count{api=\"updateResults\",application=\"Kruize\",status=\"success\"}))",
        "updateResults_count_failure": "sum((kruizeAPI_count{api=\"updateResults\",application=\"Kruize\",status=\"failure\"}))",
        "createExperiment_count_success": "sum((kruizeAPI_count{api=\"createExperiment\",application=\"Kruize\",status=\"success\"}))",
        "createExperiment_count_failure": "sum((kruizeAPI_count{api=\"createExperiment\",application=\"Kruize\",status=\"failure\"}))",
        "listExperiments_count_success": "sum((kruizeAPI_count{api=\"listExperiments\",application=\"Kruize\",status=\"success\"}))",
        "listExperiments_count_failure": "sum((kruizeAPI_count{api=\"listExperiments\",application=\"Kruize\",status=\"failure\"}))",
        "addRecommendationToDB_count_success": "sum((kruizeDB_count{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"success\"}))",
        "addRecommendationToDB_count_failure": "sum((kruizeDB_count{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"failure\"}))",
        "loadAllPerformanceProfiles_count_success": "sum((kruizeDB_count{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"success\"}))",
        "loadAllPerformanceProfiles_count_failure": "sum((kruizeDB_count{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"failure\"}))",
        "loadExperimentByName_count_success": "sum((kruizeDB_count{method=\"loadExperimentByName\",application=\"Kruize\",status=\"success\"}))",
        "loadExperimentByName_count_failure": "sum((kruizeDB_count{method=\"loadExperimentByName\",application=\"Kruize\",status=\"failure\"}))",
        "loadResultsByExperimentName_count_success": "sum((kruizeDB_count{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"success\"}))",
        "loadResultsByExperimentName_count_failure": "sum((kruizeDB_count{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"failure\"}))",
        "loadPerformanceProfileByName_count_success": "sum((kruizeDB_count{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"success\"}))",
        "loadPerformanceProfileByName_count_failure": "sum((kruizeDB_count{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"failure\"}))",
        "loadRecommendationsByExperimentNameAndDate_count_success": "sum((kruizeDB_count{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"success\"}))",
        "loadRecommendationsByExperimentNameAndDate_count_failure": "sum((kruizeDB_count{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"failure\"}))",
        "addBulkResultsToDBAndFetchFailedResults_count_success": "sum((kruizeDB_count{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"success\"}))",
        "addBulkResultsToDBAndFetchFailedResults_count_failure": "sum((kruizeDB_count{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"failure\"}))",
        "addExperimentToDB_count_success": "sum((kruizeDB_count{method=\"addExperimentToDB\",application=\"Kruize\",status=\"success\"}))",
        "addExperimentToDB_count_failure": "sum((kruizeDB_count{method=\"addExperimentToDB\",application=\"Kruize\",status=\"failure\"}))",
        }

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
            #else:
            #    print(f"Failed to run query '{query}' with status code {response.status_code}")
    return results_map


def write_header_to_csv(filename):
    with open(filename, 'w') as f:
        writer = csv.DictWriter(f, fieldnames=csv_headers)
        writer.writeheader()

def job(queries_type,outputfile):
    now_utc = datetime.utcnow()
    timestamp_utc = now_utc.isoformat()
    if queries_type == "increase":
        print("RUNNING THE JOB TO COLLECT KRUIZE INCREASE METRICS..")
        results_map = run_queries("increase")
    elif queries_type == "total":
        print("RUNNING THE JOB TO COLLECT KRUIZE TOTAL METRICS..")
        results_map = run_queries("total")
    results_map['interval_end'] = timestamp_utc
    with open(outputfile, 'a') as f:
        writer = csv.DictWriter(f, fieldnames=csv_headers)
        writer.writerow(results_map)

def schedule_job(queries_type):
    if queries_type == "increase":
        outputfile="kruizemetrics_increase.csv"
    elif queries_type == "total":
        outputfile="kruizemetrics_total.csv"
    write_header_to_csv(outputfile)
    numeric_time = int(time_duration[:-1])
    time_in_seconds = numeric_time * 60
    if duration is None:
        while True:
            job(queries_type,outputfile)
            print("Sleep for ",time_in_seconds, " seconds")
            time.sleep(time_in_seconds)
    else:
        now = datetime.utcnow()
        end = now + timedelta(hours=duration)

        while now < end:
            now = datetime.utcnow()
            job(queries_type,outputfile) 
            print("Sleep for ",time_in_seconds, " seconds")
            time.sleep(time_in_seconds) 


def main(argv):
    global duration
    global cluster_type
    global server
    global clusterResults
    global time_duration 
    global queries_map

    try:
        opts, args = getopt.getopt(argv,"h:c:s:d:t:q:")
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
            
    if '-t' not in sys.argv:
        time_duration = "60m"
    if '-q' not in sys.argv:
        queries_type = "increase"
    if '-d' not in sys.argv:
        duration = None

    queries_map = {
       	"updateRecommendations_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"updateRecommendations\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "updateRecommendations_sum_failure": f"sum(increase(kruizeAPI_sum{{api=\"updateRecommendations\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "updateResults_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"updateResults\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "updateResults_sum_failure": f"sum(increase(kruizeAPI_sum{{api=\"updateResults\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "createExperiment_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"createExperiment\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "createExperiment_sum_failure": f"sum(increase(kruizeAPI_sum{{api=\"createExperiment\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "listExperiments_sum_success": f"sum(increase(kruizeAPI_sum{{api=\"listExperiments\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "listExperiments_sum_failure": f"sum(increase(kruizeAPI_sum{{api=\"listExperiments\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "addRecommendationToDB_sum_success": f"sum(increase(kruizeDB_sum{{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "addRecommendationToDB_sum_failure": f"sum(increase(kruizeDB_sum{{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "loadAllPerformanceProfiles_sum_success": f"sum(increase(kruizeDB_sum{{api=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "loadAllPerformanceProfiles_sum_failure": f"sum(increase(kruizeDB_sum{{api=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "loadExperimentByName_sum_success": f"sum(increase(kruizeDB_sum{{method=\"loadExperimentByName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "loadExperimentByName_sum_failure": f"sum(increase(kruizeDB_sum{{method=\"loadExperimentByName\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "loadResultsByExperimentName_sum_success": f"sum(increase(kruizeDB_sum{{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "loadResultsByExperimentName_sum_failure": f"sum(increase(kruizeDB_sum{{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "loadPerformanceProfileByName_sum_success": f"sum(increase(kruizeDB_sum{{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "loadPerformanceProfileByName_sum_failure": f"sum(increase(kruizeDB_sum{{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "loadRecommendationsByExperimentNameAndDate_sum_success": f"sum(increase(kruizeDB_sum{{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "loadRecommendationsByExperimentNameAndDate_sum_failure": f"sum(increase(kruizeDB_sum{{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "addBulkResultsToDBAndFetchFailedResults_sum_success": f"sum(increase(kruizeDB_sum{{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "addBulkResultsToDBAndFetchFailedResults_sum_failure": f"sum(increase(kruizeDB_sum{{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "addExperimentToDB_sum_success": f"sum(increase(kruizeDB_sum{{method=\"addExperimentToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "addExperimentToDB_sum_failure": f"sum(increase(kruizeDB_sum{{method=\"addExperimentToDB\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "updateRecommendations_count_success": f"sum(increase(kruizeAPI_count{{api=\"updateRecommendations\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "updateRecommendations_count_failure": f"sum(increase(kruizeAPI_count{{api=\"updateRecommendations\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "updateResults_count_success": f"sum(increase(kruizeAPI_count{{api=\"updateResults\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "updateResults_count_failure": f"sum(increase(kruizeAPI_count{{api=\"updateResults\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "createExperiment_count_success": f"sum(increase(kruizeAPI_count{{api=\"createExperiment\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "createExperiment_count_failure": f"sum(increase(kruizeAPI_count{{api=\"createExperiment\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "listExperiments_count_success": f"sum(increase(kruizeAPI_count{{api=\"listExperiments\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "listExperiments_count_failure": f"sum(increase(kruizeAPI_count{{api=\"listExperiments\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "addRecommendationToDB_count_success": f"sum(increase(kruizeDB_count{{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "addRecommendationToDB_count_failure": f"sum(increase(kruizeDB_count{{method=\"addRecommendationToDB\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "loadAllPerformanceProfiles_count_success": f"sum(increase(kruizeDB_count{{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "loadAllPerformanceProfiles_count_failure": f"sum(increase(kruizeDB_count{{method=\"loadAllPerformanceProfiles\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "loadExperimentByName_count_success": f"sum(increase(kruizeDB_count{{method=\"loadExperimentByName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "loadExperimentByName_count_failure": f"sum(increase(kruizeDB_count{{method=\"loadExperimentByName\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "loadResultsByExperimentName_count_success": f"sum(increase(kruizeDB_count{{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "loadResultsByExperimentName_count_failure": f"sum(increase(kruizeDB_count{{method=\"loadResultsByExperimentName\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "loadPerformanceProfileByName_count_success": f"sum(increase(kruizeDB_count{{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "loadPerformanceProfileByName_count_failure": f"sum(increase(kruizeDB_count{{method=\"loadPerformanceProfileByName\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "loadRecommendationsByExperimentNameAndDate_count_success": f"sum(increase(kruizeDB_count{{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "loadRecommendationsByExperimentNameAndDate_count_failure": f"sum(increase(kruizeDB_count{{method=\"loadRecommendationsByExperimentNameAndDate\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "addBulkResultsToDBAndFetchFailedResults_count_success": f"sum(increase(kruizeDB_count{{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "addBulkResultsToDBAndFetchFailedResults_count_failure": f"sum(increase(kruizeDB_count{{method=\"addBulkResultsToDBAndFetchFailedResults\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        "addExperimentToDB_count_success": f"sum(increase(kruizeDB_count{{method=\"addExperimentToDB\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
        "addExperimentToDB_count_failure": f"sum(increase(kruizeDB_count{{method=\"addExperimentToDB\",application=\"Kruize\",status=\"failure\"}}[{time_duration}]))",
        }
    
    # Create a thread to run the job scheduler
    job_thread = threading.Thread(target=schedule_job(queries_type))
    job_thread.start()
    job_thread.join()

if __name__ == '__main__':
    main(sys.argv[1:])
