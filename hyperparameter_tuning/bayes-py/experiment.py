"""
Copyright (c) 2020, 2020 Red Hat, IBM Corporation and others.

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

import csv
import subprocess
import sys


def perform_experiment(experiment_tunables):
    """
    Return the result received from the experiment manager.
    
    Parameters:
        experiment_tunables (dict): A list containing hyperparameter values suggested by the sampler.
    
    Returns:
        sla (float/str): Value returned by the experiment manager.
        is_success (bool): A boolean value that is set to true if the experiment runs successfully for this config, false otherwise.

    Files generated:
        total-output.txt:
            Sample format:
            Instances , Throughput , Responsetime , TOTAL_PODS_MEM , TOTAL_PODS_CPU , CPU_MIN , CPU_MAX , MEM_MIN , MEM_MAX , CLUSTER_MEM% , CLUSTER_CPU% , CPU_REQ , MEM_REQ , WEB_ERRORS
            1 ,  338.3 , 765 , 0 , 0 , 0 , 0 , 0 , 0 ,  60.2367 , 21.4259 , 3.3294886353000983 , 410.36017895925215M , 0
            Run , CPU_REQ , MEM_REQ , Throughput , Responsetime , WEB_ERRORS , CPU , CPU_MIN , CPU_MAX , MEM , MEM_MIN , MEM_MAX
            0 , 3.3294886353000983 , 410.36017895925215M , 338.3 , 765 , 0  ,0 , 0 , 0  , 0 , 0 , 0

            Instances , Throughput , Responsetime , TOTAL_PODS_MEM , TOTAL_PODS_CPU , CPU_MIN , CPU_MAX , MEM_MIN , MEM_MAX , CLUSTER_MEM% , CLUSTER_CPU% , CPU_REQ , MEM_REQ , WEB_ERRORS
            1 ,  150 , 2130 , 0 , 0 , 0 , 0 , 0 , 0 ,  60.0533 , 17.1877 , 3.750514009853204 , 290.75151431609027M , 7778
            Run , CPU_REQ , MEM_REQ , Throughput , Responsetime , WEB_ERRORS , CPU , CPU_MIN , CPU_MAX , MEM , MEM_MIN , MEM_MAX
            0 , 3.750514009853204 , 290.75151431609027M , 150.0 , 2130 , 7778   ,0 , 0 , 0  , 0 , 0 , 0
        output.txt:
            Sample format:
            1 ,  338.3 , 765 , 0 , 0 , 0 , 0 , 0 , 0 ,  60.2367 , 21.4259 , 3.3294886353000983 , 410.36017895925215M , 0
            1 ,  150 , 2130 , 0 , 0 , 0 , 0 , 0 , 0 ,  60.0533 , 17.1877 , 3.750514009853204 , 290.75151431609027M , 7778
        experiment-data.csv:
            Sample format:
            Instances,Throughput,Responsetime,TOTAL_PODS_MEM,TOTAL_PODS_CPU,CPU_MIN,CPU_MAX,MEM_MIN,MEM_MAX,CLUSTER_MEM%,CLUSTER_CPU%,CPU_REQ,MEM_REQ,WEB_ERRORS
            1,338.3,765,0,0,0,0,0,0,60.2367,21.4259,3.3294886353000983,410.36017895925215M,0
            1,150,2130,0,0,0,0,0,0,60.0533,17.1877,3.750514009853204,290.75151431609027M,7778
    """
    experiment_data_file = "experiment-data.csv"
    
    for tunable in experiment_tunables:
        if tunable["tunable_name"] == "cpuRequest":
            cpu_request = tunable["tunable_value"]
        elif tunable["tunable_name"] == "memoryRequest":
            memory_request = tunable["tunable_value"]

    output = subprocess.run(["bash", "scripts/applyconfig.sh", str(cpu_request), str(memory_request)], stdout=subprocess.PIPE).stdout.decode('utf-8')

    orig_stdout = sys.stdout
    f = open('total-output.txt', 'a')
    sys.stdout = f

    if output == '':
        sla = "Nan"
        is_success = False
        sys.stdout = orig_stdout
        f.close()
        return sla, is_success
    else:
        is_success = True
        """
        output:
        Instances , Throughput , Responsetime , TOTAL_PODS_MEM , TOTAL_PODS_CPU , CPU_MIN , CPU_MAX , MEM_MIN , MEM_MAX , CLUSTER_MEM% , CLUSTER_CPU% , CPU_REQ , MEM_REQ , WEB_ERRORS 
        1 ,  338.3 , 765 , 0 , 0 , 0 , 0 , 0 , 0 ,  60.2367 , 21.4259 , 3.3294886353000983 , 410.36017895925215M , 0
        Run , CPU_REQ , MEM_REQ , Throughput , Responsetime , WEB_ERRORS , CPU , CPU_MIN , CPU_MAX , MEM , MEM_MIN , MEM_MAX
        0 , 3.3294886353000983 , 410.36017895925215M , 338.3 , 765 , 0  ,0 , 0 , 0  , 0 , 0 , 0 
        """
        print(output)
        sys.stdout = orig_stdout
        f.close()
        orig_stdout = sys.stdout
        f = open('output.txt', 'a')
        sys.stdout = f
        rows = output.split("\n")
        # print(rows)
        data = rows[1]
        """
        data:
        1 ,  338.3 , 765 , 0 , 0 , 0 , 0 , 0 , 0 ,  60.2367 , 21.4259 , 3.3294886353000983 , 410.36017895925215M , 0
        """
        print(data)
        sla = data.split(" , ")[2]
        file = open(experiment_data_file, "a")
        file.close()
        with open(experiment_data_file, "r+") as file:
            reader = csv.reader(file)
            csv_data = list(reader)
            row_count = len(csv_data)
            writer = csv.writer(file)
            if row_count == 0:
                column = [c.strip() for c in rows[0].split(',')]
                writer.writerow(column)
                column = [c.strip() for c in rows[1].split(',')]
                writer.writerow(column)
            else:
                column = [c.strip() for c in rows[1].split(',')]
                writer.writerow(column)
        sys.stdout = orig_stdout
        f.close()
        return sla, is_success 

