"""
Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.

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

from http.server import BaseHTTPRequestHandler, HTTPServer
import argparse
import re
import cgi
import json
import requests
import threading
import time
from urllib import parse
from urllib.parse import urlparse, parse_qs

from tunables import get_all_tunables

from bayes_optuna import optuna_hpo

autotune_object_ids = []


class HTTPRequestHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        if re.search('/experiment_trials', self.path):
            ctype, pdict = cgi.parse_header(self.headers.get('content-type'))
            if ctype == 'application/json':
                length = int(self.headers.get('content-length'))
                rfile_str = self.rfile.read(length).decode('utf8')
                json_object = json.loads(rfile_str)
                # print(json_object)
                # TODO: validate structure of json_object
                if json_object["operation"] == "EXP_TRIAL_GENERATE_NEW":
                    # print("url: ", json_object["url"])
                    if json_object["id"] in autotune_object_ids:
                        # HTTP 400: bad request
                        self.send_response(400)
                        self.send_header("content-type", "text/html")
                        self.end_headers()
                        self.wfile.write("-1".encode('utf-8'))
                    else:
                        autotune_object_ids.append(json_object["id"])
                        get_search_create_study(json_object["id"], json_object["url"])
                        # HTTP 200: ok
                        trial_number = optuna_hpo.TrialDetails.trial_number
                        self.send_response(200)
                        self.send_header("content-type", "text/html")
                        self.end_headers()
                        self.wfile.write(str(trial_number).encode('utf-8'))
                elif json_object["operation"] == "EXP_TRIAL_GENERATE_SUBSEQUENT":
                    print("url: ", json_object["url"])
                    if json_object["id"] not in autotune_object_ids:
                        # HTTP 400: bad request
                        self.send_response(400)
                        self.send_header("content-type", "text/html")
                        self.end_headers()
                        self.wfile.write("-1".encode('utf-8'))
                    else:
                        # HTTP 200: ok
                        trial_number = optuna_hpo.TrialDetails.trial_number
                        self.send_response(200)
                        self.send_header("content-type", "text/html")
                        self.end_headers()
                        self.wfile.write(str(trial_number).encode('utf-8'))
                elif json_object["operation"] == "EXP_TRIAL_RESULT":
                    if json_object["id"] not in autotune_object_ids and json_object["trial_number"] != optuna_hpo.TrialDetails.trial_number:
                        # HTTP 400: bad request
                        self.send_response(400)
                        self.send_header("content-type", "text/html")
                        self.end_headers()
                        self.wfile.write("-1".encode('utf-8'))
                    else:
                        optuna_hpo.TrialDetails.trial_result = json_object["trial_result"]
                        optuna_hpo.TrialDetails.result_value_type = json_object["result_value_type"]
                        optuna_hpo.TrialDetails.result_value = json_object["result_value"]
                        optuna_hpo.TrialDetails.trial_result_received = 1
                        # HTTP 200: ok
                        self.send_response(200)
                        self.send_header("content-type", "text/html")
                        self.end_headers()
                        self.wfile.write("0".encode('utf-8'))
            else:
                # HTTP 400: bad request
                self.send_response(400, "Bad Request: content-type must be application/json")
                self.end_headers()
        else:
            # HTTP 403: forbidden
            self.send_response(403)
            self.end_headers()

    def do_GET(self):
        if re.search('/experiment_trials', self.path):
            query = parse_qs(urlparse(self.path).query)
            print("self.path:", self.path)
            print("query:", query)
            if query["id"][0] in autotune_object_ids and query["trial_number"][0] == str(optuna_hpo.TrialDetails.trial_number):
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                data = json.dumps(optuna_hpo.TrialDetails.trial_json_object)
                self.wfile.write(data.encode('utf8'))
            else:
                self.send_response(404, 'Not Found: record does not exist')
                self.end_headers()
        else:
            self.send_response(403)
            self.end_headers()


def get_search_create_study(id_, url):
    search_space_json = get_search_space(id_, url)
    application_name, direction, hpo_algo_impl, id_, objective_function, tunables, value_type = get_all_tunables(search_space_json)
    if hpo_algo_impl in ("optuna_tpe", "optuna_tpe_multivariate", "optuna_skopt"):
        threading.Thread(
            target=optuna_hpo.recommend, args=(application_name, direction, hpo_algo_impl, id_, objective_function,
                                               tunables, value_type)).start()
    time.sleep(2)
    print("recommend done")


def get_search_space(id_, url):
    params = {"id": id_}
    r = requests.get(url, params)
    search_space_json = r.json()
    return search_space_json


def main():
    host_name = "localhost"
    server_port = 8085

    server = HTTPServer((host_name, server_port), HTTPRequestHandler)
    print("Starting server at http://%s:%s" % (host_name, server_port))
    server.serve_forever()


if __name__ == '__main__':
    main()
