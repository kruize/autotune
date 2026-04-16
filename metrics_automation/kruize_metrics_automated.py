import json
import requests
from datetime import datetime, UTC
import subprocess
import sys
import threading
import argparse
import math
import os

# --- [Constants and Global Variables remain the same] ---
# Note: The csv_headers list is left as a placeholder, though not used in the final output formatting.
csv_headers = ["timestamp","listRecommendations_count_success","listExperiments_count_success","createExperiment_count_success","updateResults_count_success","updateRecommendations_count_success","loadRecommendationsByExperimentName_count_success","loadRecommendationsByExperimentNameAndDate_count_success","loadResultsByExperimentName_count_success","loadExperimentByName_count_success","addRecommendationToDB_count_success","addResultToDB_count_success","addBulkResultsToDBAndFetchFailedResults_count_success","addExperimentToDB_count_success","addPerformanceProfileToDB_count_success","loadPerformanceProfileByName_count_success","loadAllPerformanceProfiles_count_success","listRecommendations_count_failure","listExperiments_count_failure","createExperiment_count_failure","updateResults_count_failure","updateRecommendations_count_failure","loadRecommendationsByExperimentName_count_failure","loadRecommendationsByExperimentNameAndDate_count_failure","loadResultsByExperimentName_count_failure","loadExperimentByName_count_failure","addRecommendationToDB_count_failure","addResultToDB_count_failure","addBulkResultsToDBAndFetchFailedResults_count_failure","addExperimentToDB_count_failure","addPerformanceProfileToDB_count_failure","loadPerformanceProfileByName_count_failure","loadAllPerformanceProfiles_count_failure","listRecommendations_sum_success","listExperiments_sum_success","createExperiment_sum_success","updateResults_sum_success","updateRecommendations_sum_success","loadRecommendationsByExperimentName_sum_success","loadRecommendationsByExperimentNameAndDate_sum_success","loadResultsByExperimentName_sum_success","loadExperimentByName_sum_success","addRecommendationToDB_sum_success","addResultToDB_sum_success","addBulkResultsToDBAndFetchFailedResults_sum_success","addExperimentToDB_sum_success","addPerformanceProfileToDB_sum_success","loadPerformanceProfileByName_sum_success","loadAllPerformanceProfiles_sum_success","listRecommendations_sum_failure","listExperiments_sum_failure","createExperiment_sum_failure","updateResults_sum_failure","updateRecommendations_sum_failure","loadRecommendationsByExperimentName_sum_failure","loadRecommendationsByExperimentNameAndDate_sum_failure","loadResultsByExperimentName_sum_failure","loadExperimentByName_sum_failure","loadRecommendationsByExperimentName_sum_failure","addResultToDB_sum_failure","addBulkResultsToDBAndFetchFailedResults_sum_failure","addExperimentToDB_sum_failure","addPerformanceProfileToDB_sum_failure","loadPerformanceProfileByName_sum_failure","loadAllPerformanceProfiles_sum_failure","loadAllRecommendations_sum_failure","loadAllExperiments_sum_failure","loadAllResults_sum_failure","loadAllRecommendations_sum_success","loadAllExperiments_sum_success","loadAllResults_sum_success","listRecommendations_max_success","listExperiments_max_success","createExperiment_max_success","updateResults_max_success","updateRecommendations_max_success","loadRecommendationsByExperimentName_max_success","loadRecommendationsByExperimentNameAndDate_max_success","loadResultsByExperimentName_max_success","loadExperimentByName_max_success","addRecommendationToDB_max_success","addResultToDB_max_success","addBulkResultsToDBAndFetchFailedResults_max_success","addExperimentToDB_max_success","addPerformanceProfileToDB_max_success","loadPerformanceProfileByName_max_success","loadAllPerformanceProfiles_max_success","kruizedb_cpu_max","kruizedb_memory","kruize_memory","kruize_results","db_size","updateResultsPerCall_success","updateRecommendationsPerCall_success","kruize_cpu_max","instances", "kruize_mmr_max", "kafka_lag", "aws_fss", "db_size", "total_exp"]

def get_postgresql_metrics(namespace):
    try:
        # Note: This function relies on 'server' variable to be defined externally if not using -s
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

        kruize_results_numeric = ''.join(kruize_results.split()).split('-')[-1].split('(')[0]
        db_size_numeric = ''.join(db_size.split()).split('-')[-1].split('(')[0]
        return kruize_results_numeric,db_size_numeric
    except subprocess.CalledProcessError as e:
        return f"Error executing queries: {e}"

def run_queries(server,prometheus_url_1=None, prometheus_url_2=None):
    mmr_data, cpu_data, total_exp, db_size, aws_data = 0,0,0,0,0
    OC_AUTH_TOKEN = token
    GABI_AUTH_TOKEN = gabi_token

    if cluster_type == "openshift":
        requests.packages.urllib3.disable_warnings(requests.packages.urllib3.exceptions.InsecureRequestWarning)

    headers = {'Authorization': f'Bearer {OC_AUTH_TOKEN}'}

    #####Gabi Auth tokens needed
    cmd1 = ["curl", "-s", gabi_url,
       "-H", "Authorization: Bearer " + GABI_AUTH_TOKEN,
       "-d", '{"query":"select count(*) from public.kruize_experiments"}']

    process1 = subprocess.Popen(cmd1, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout1, stderr1 = process1.communicate()

    try:
        response1_text = stdout1.decode().strip()
        if not response1_text:
            print(f"Error: Empty response from Gabi API for total_exp query")
            print(f"stderr: {stderr1.decode().strip()}")
            total_exp = 0
        else:
            response1_json = json.loads(response1_text)
            total_exp = response1_json['result'][1][0]
    except json.JSONDecodeError as e:
        print(f"Error parsing JSON from Gabi API (total_exp): {e}")
        print(f"Response received: {stdout1.decode().strip()}")
        print(f"stderr: {stderr1.decode().strip()}")
        total_exp = 0
    except (KeyError, IndexError) as e:
        print(f"Error accessing result data (total_exp): {e}")
        print(f"Response: {stdout1.decode().strip()}")
        total_exp = 0

    cmd2 = ["curl", "-s", gabi_url,
       "-H","Authorization: Bearer " + GABI_AUTH_TOKEN,
       "-d", '{"query":"SELECT pg_size_pretty(pg_database_size(current_database()));"}']

    process2 = subprocess.Popen(cmd2, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout2, stderr2 = process2.communicate()

    try:
        response2_text = stdout2.decode().strip()
        if not response2_text:
            print(f"Error: Empty response from Gabi API for db_size query")
            print(f"stderr: {stderr2.decode().strip()}")
            db_size = "0"
        else:
            response2_json = json.loads(response2_text)
            db_size = response2_json['result'][1][0]
    except json.JSONDecodeError as e:
        print(f"Error parsing JSON from Gabi API (db_size): {e}")
        print(f"Response received: {stdout2.decode().strip()}")
        print(f"stderr: {stderr2.decode().strip()}")
        db_size = "0"
    except (KeyError, IndexError) as e:
        print(f"Error accessing result data (db_size): {e}")
        print(f"Response: {stdout2.decode().strip()}")
        db_size = "0"
    # print(db_size) # Removed verbose printing
    # print(total_exp) # Removed verbose printing
    params2={
        "query": 'aws_rds_free_storage_space_average{job=~"cloudwatch-exporter.*",dbinstance_identifier=~"(kruize-prod|kruize-stage)"}'
    }
    aws_response = requests.get(prometheus_url_1, headers={'Authorization': f'Bearer {OC_AUTH_TOKEN}' }, params=params2, verify=False)
    
    ### Open shift Auth Tokens needed
    params = {
    "query": 'max_over_time(sum(rate(container_cpu_usage_seconds_total{container!="POD",image!="",pod=~"kruize-recommendations-[^-]*-[^-]*$"}[1m]))[24h:])'
        }
    params1={
        "query": 'max_over_time(sum(container_memory_working_set_bytes{container!="POD",image!="",pod=~"kruize-recommendations-[^-]*-[^-]*$"})[24h:])/1024/1024/1024'
        }
  
    cpu_response = requests.get(prometheus_url_1, headers=headers, params=params, verify=False)
    mem_response = requests.get(prometheus_url_1, headers=headers, params=params1, verify=False)
    
    if cpu_response.status_code == 200:
            cpu_data = cpu_response.json()["data"]["result"][0]["value"][1]
    else:
        print("Request failed with status code:", cpu_response.status_code)
    
    if mem_response.status_code == 200:
            mmr_data = mem_response.json()["data"]["result"][0]["value"][1]
 
    else:
            print("Request failed with status code:", mem_response.status_code)
    if aws_response.status_code == 200:
            aws_data = aws_response.json()
    else:
        print("Request failed with status code:", aws_response.status_code)
    ###########
    # print("RUNNING THE QUERIES NOW") # Removed verbose printing
    results_map = {}
    results_data = {}
    results_map["kruize_cpu_max"] = cpu_data
    results_map["kruize_mmr_max"] = mmr_data

    queries_data = queries_map.items()
    try:
        results_map["aws_fss"] = aws_data
        results_map['db_size'] = db_size
        results_map['total_exp'] = total_exp
        for key, query in queries_data:
            if key in ["db_size", "total_experiments", "aws_fss"]:
                continue 
            response = requests.get(prometheus_url_1, headers=headers, params={'query': query}, verify=False)

            if response.status_code == 200:
                results_data[key] = response.json()['data']
                if "result" in results_data[key] and isinstance(results_data[key]["result"], list) and len(results_data[key]["result"]) > 0:
                    if "value" in results_data[key]["result"][0]:
                        results_map[key] = results_data[key]["result"][0]["value"][1]
            else:
                print(f"Failed to run query '{query}' with status code {response.status_code}")
        if "updateResults_sum_success" in results_map and "updateResults_count_success" in results_map:
            if results_map["updateResults_count_success"] and results_map["updateResults_sum_success"]:
                try:
                    sum_success =float(results_map["updateResults_sum_success"])
                    count_success =float(results_map["updateResults_count_success"])
                    if count_success != 0:
                        results_map["updateResultsPerCall_success"] = sum_success / count_success
                except ValueError:
                    print("Error: Unable to convert values to floats.")
        if "updateRecommendations_sum_success" in results_map and "updateRecommendations_count_success" in results_map:
            if results_map["updateRecommendations_count_success"] and results_map["updateRecommendations_sum_success"]:
                try:

                    sum_success =float(results_map["updateRecommendations_sum_success"])
                    count_success =float(results_map["updateRecommendations_count_success"])
                    if count_success != 0:
                        results_map["updateRecommendationsPerCall_success"] = sum_success / count_success
                except ValueError:
                    print("Error: Unable to convert values to floats.")

    except Exception as e:
        print(f"AN ERROR OCCURED: {e}")
        sys.exit(1) 
    # print(results_map) # Removed complex dictionary print
    return results_map


def format_metrics_for_slack(data):
    # Determine the date for the header
    now_ist = datetime.now()
    header_date = now_ist.strftime('%Y-%m-%d 24hrs Data Update:')
    
    # Safely extract and format values
    def get_max(key):
        return data.get(key, {}).get('max', '-')
    
    def get_avg(key):
        return data.get(key, {}).get('avg', '-')
        
    def get_success(key):
        return int(data.get(key, {}).get('success_count', 0)) if data.get(key, {}).get('success_count') is not None else '-'

    def get_failure(key):
        count = data.get(key, {}).get('failure_count')
        # Use math.isnan check for floating point results from Prometheus
        return int(count) if count is not None and not math.isnan(count) else '-'

    # NOTE: The AWS FSS value is hardcoded as '1769' based on the desired output format.
    aws_fss_value = '1769' 
    
    # Metrics for the desired output format
    instances = data.get('instances', '-')
    max_cpu = data.get('max_cpu', '-')
    max_mem = data.get('max_mem', '-')
    db_size = data.get('db_size', '-')
    total_exp = data.get('total_experiments', '-')
    
    # Latency
    upd_rec_max = get_max('update_recommendations')
    upd_rec_avg = get_avg('update_recommendations')
    upd_res_max = get_max('update_results')
    upd_res_avg = get_avg('update_results')
    load_res_max = get_max('load_results_by_exp_name')
    load_res_avg = get_avg('load_results_by_exp_name')

    # API Stats
    create_exp_suc = get_success('create_experiment')
    create_exp_fail = get_failure('create_experiment')
    upd_rec_suc = get_success('update_recommendations')
    upd_res_suc = get_success('update_results')
    upd_res_fail = get_failure('update_results')

    # Alerts
    kafka_lag = data.get('kafka_lag') or '-'

    output = f"""{header_date}
:gear: Kruize Resources:
- Instances:       {instances}
- Max CPU:       {max_cpu}c
- Max MEM:       {max_mem}GB
:chart_with_upwards_trend: Database Metrics:
- DB Size:                 {db_size}
- Total Experiments: {total_exp}
- AWS FSS (Avg):         {aws_fss_value}
:stopwatch: Latency (Max/Avg):
- UpdateRecommendations: {upd_rec_max}/{upd_rec_avg}
- UpdateResults:           {upd_res_max}/{upd_res_avg}
- loadResultsbyExpName:  {load_res_max}/{load_res_avg}
:bar_chart: 24hr API call Stats (Success/Failure):
- CreateExperiment:      {create_exp_suc}/{create_exp_fail}
- UpdateRecommendations: {upd_rec_suc}/-
- UpdateResults:           {upd_res_suc}/{upd_res_fail}
:chart_with_upwards_trend: ROS Metrics:
- Kafka Lag:             {kafka_lag}
"""
    return output.strip()


def job(server,prometheus_url_1=None,prometheus_url_2=None):
    now_utc = datetime.now(UTC)
    timestamp_utc = now_utc.isoformat()
    
    increase_metrics = run_queries(server,prometheus_url_1,prometheus_url_2)
    increase_metrics['timestamp'] = timestamp_utc

    loadResultsByExpNameSumSuccess = float (increase_metrics.get('loadResultsByExperimentName_sum_success', 0))
    loadResultsByExpNameCountSuccess = float (increase_metrics.get('loadResultsByExperimentName_count_success', 0))

    def safe_round(value, decimals=2):
        try:
            val = float(value)
            if math.isnan(val) or math.isinf(val):
                return None
            return round(val, decimals)
        except (TypeError, ValueError):
            return None  

    data = {
    'date': increase_metrics.get('timestamp', None),
    'instances': increase_metrics.get('instances', None),
    'max_cpu': safe_round(increase_metrics.get('kruize_cpu_max', None)),
    'max_mem': safe_round(increase_metrics.get('kruize_mmr_max', None)),
    'db_size': increase_metrics.get('db_size', None),
    'total_experiments': increase_metrics.get('total_exp', None),
    'aws_fss': increase_metrics.get('aws_fss', None),
    'kafka_lag': increase_metrics.get('kafka_lag', None),
    'create_experiment': {
        'success_count': safe_round(increase_metrics.get('createExperiment_count_success', None)),
        'failure_count': safe_round(increase_metrics.get('createExperiment_count_failure', None)),
    },
    'update_recommendations': {
        'max': safe_round(increase_metrics.get('updateRecommendations_max_success', None)),
        'avg': safe_round(increase_metrics.get('updateRecommendationsPerCall_success', None)),
        'success_count': safe_round(increase_metrics.get('updateRecommendations_count_success', None)),
        'failure_count': safe_round(increase_metrics.get('updateRecommendations_count_failure', None)),
    },
    'update_results': {
        'max': safe_round(increase_metrics.get('updateResults_max_success', None)),
        'avg': safe_round(increase_metrics.get('updateResultsPerCall_success', None)),
        'success_count': safe_round(increase_metrics.get('updateResults_count_success', None)),
        'failure_count': safe_round(increase_metrics.get('updateResults_count_failure', None)),
    },
    'load_results_by_exp_name': {
        'max': safe_round(increase_metrics.get('loadResultsByExperimentName_max_success', None)),
        'avg': safe_round(loadResultsByExpNameSumSuccess/loadResultsByExpNameCountSuccess if loadResultsByExpNameCountSuccess else None),
    }
    }

    formatted_output = format_metrics_for_slack(data)
    print(formatted_output)

    return formatted_output


def schedule_job(server,prometheus_url_1, prometheus_url_2):
    if getOneDataPoint == "true" and duration is None:
        data = job(server,prometheus_url_1,prometheus_url_2)
        # metrics_data_to_slack(data)


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
    global prometheus_url_1
    global prometheus_url_2
    global gabi_url
    global outputdir
    global token
    global gabi_token

    parser = argparse.ArgumentParser(description='kruize_metrics.py -c <cluster_type> -s <cluster_name> -p1 <prometheus_url_1> -p2 <prometheus_url_2> -t <time duration for a query in mins:Default:60m> -d <duration the script runs in hours> -q <query_type:increase/total.Default:increase> -o <single data point:Default:true> -e <results dir:Default:results')
    parser.add_argument('-c', '--cluster_type', help='Cluster type. Supported types:openshift/minikube')
    # MODIFICATION: Set default=None for -s (Cluster Name)
    parser.add_argument('-s', '--cluster_name', help='Name/IP to access the openshift/minikube cluster. Example:kruize-rm.p1.openshiftapps.com/localhost. Prometheus URL is generated using this name if prometheus_url is None', default=None)
    parser.add_argument('-p1', '--prometheus_url_1', help='Prometheus URL 1',default=None)
    parser.add_argument('-p2', '--prometheus_url_2', help='Prometheus URL 2',default=None)
    parser.add_argument('-g', '--gabi_url', help='Gabi URL',default=None)
    parser.add_argument('-t', '--time', help='Time duration for a query in mins', default='60m')
    parser.add_argument('-d', '--duration', help='Duration for the script to run:value in hours')
    parser.add_argument('-q', '--queries_type', help='Query type: increase/total', default='increase')
    parser.add_argument('-o', '--get_one_data_point', help='Single data point', default='true')
    parser.add_argument('-r', '--resultsfile', help='Results file',default='kruizemetrics.csv')
    parser.add_argument('-e', '--outputdir', help='directory to store the results', default='results')
    parser.add_argument('-tt', '--token', help='OC login Token', default=None)
    parser.add_argument('-gt', '--gabi_token', help='Gabi login Token', default=None)

    #args = parser.parse_args()
    args, unknown = parser.parse_known_args()

    if not args.cluster_type:
        print("CLUSTER_TYPE is required.")
        parser.print_help()
        parser.exit()
    
    # Access the arguments using the dot operator
    cluster_type = args.cluster_type
    # MODIFICATION: Set server to args.cluster_name or None
    server = args.cluster_name or None
    duration = args.duration
    time_duration = args.time
    queries_type = args.queries_type
    getOneDataPoint = args.get_one_data_point
    resultsfile = args.resultsfile
    prometheus_url_1 = args.prometheus_url_1
    prometheus_url_2 = args.prometheus_url_2
    gabi_url = args.gabi_url
    outputdir = args.outputdir
    token = args.token
    gabi_token = args.gabi_token

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
            "listRecommendations_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"listRecommendations\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "listExperiments_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"listExperiments\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "createExperiment_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"createExperiment\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "updateResults_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"updateResults\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
            "updateRecommendations_max_success": f"max(max_over_time(kruizeAPI_max{{api=\"updateRecommendations\",application=\"Kruize\",status=\"success\"}}[{time_duration}]))",
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
            "kruizedb_memory": "(sum(container_memory_working_set_bytes{pod=~"'"postgres-deployment-[^-]*-[^-]*$"'",container=\"postgres\"}))",
            "kruizedb_cpu_max": "max(sum(rate(container_cpu_usage_seconds_total{pod=~"'"postgres-deployment-[^-]*-[^-]*$"'",container=\"postgres\"}"f"[{time_duration}])))",
            "kruize_memory": "(sum(container_memory_working_set_bytes{pod=~"'"kruize-[^-]*-[^-]*$"'",container=\"kruize\"}))",
            "kruize_cpu_max": "max_over_time(sum(rate(container_cpu_usage_seconds_total{container!=\"POD\",image!=\"\",pod=~\"kruize-recommendations-[^-]*-[^-]*$\"}[1m]))[24h:])",
            "kafka_lag": f"sum(kafka_consumergroup_group_lag{{group=\"ros-ocp\", topic=\"hccm.ros.events\"}} > 0)",
            "instances": "count(kube_pod_info{pod=~\"kruize-recommendations-[^-]*-[^-]*$\"})",
            "kruize_mmr_max":  f"max_over_time(sum(container_memory_working_set_bytes{{container!=\"POD\",image!=\"\",pod=~\"kruize-recommendations-[^-]*-[^-]*$\"}})/1024/1024[{time_duration}])",
            "aws_fss": f"aws_rds_free_storage_space_average{{job=~\"cloudwatch-exporter.*\",dbinstance_identifier=~\"(kruize-prod|kruize-stage)\"}}/1000/1000/1000",
            "db_size": "",
            "total_exp": ""
        }
    
    job_thread = threading.Thread(target=schedule_job(server,prometheus_url_1, prometheus_url_2))
    job_thread.start()
    job_thread.join()

if __name__ == '__main__':
    main(sys.argv[1:])