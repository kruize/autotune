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
import re
import cgi
import json
import requests
import threading
import time
from urllib.parse import urlparse, parse_qs

from json_validate import validate_trial_generate_json
from tunables import get_all_tunables

from bayes_optuna import optuna_hpo

autotune_object_ids = {}

api_endpoint = "/experiment_trials"
host_name = "localhost"
server_port = 8085


class HTTPRequestHandler(BaseHTTPRequestHandler):
    """
    A class used to handle the HTTP requests that arrive at the server.

    The handler will parse the request and the headers, then call a method specific to the request type. The method name
    is constructed from the request. For example, for the request method GET, the do_GET() method will be called.
    """

    def _set_response(self, status_code, return_value):
        # TODO: add status_message
        self.send_response(status_code)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        self.wfile.write(return_value.encode('utf-8'))

    def do_POST(self):
        """Serve a POST request."""
        if re.search(api_endpoint + "$", self.path):
            content_type, params = cgi.parse_header(self.headers.get('content-type'))
            if content_type == 'application/json':
                length = int(self.headers.get('content-length'))
                str_object = self.rfile.read(length).decode('utf8')
                json_object = json.loads(str_object)
                # TODO: validate structure of json_object for each operation
                if json_object["operation"] == "EXP_TRIAL_GENERATE_NEW":
                    self.handle_generate_new_operation(json_object)
                elif json_object["operation"] == "EXP_TRIAL_GENERATE_SUBSEQUENT":
                    self.handle_generate_subsequent_operation(json_object)
                elif json_object["operation"] == "EXP_TRIAL_RESULT":
                    self.handle_result_operation(json_object)
                else:
                    self._set_response(400, "-1")
            else:
                self._set_response(400, "-1")
        else:
            self._set_response(403, "-1")

    def do_GET(self):
        """Serve a GET request."""
        if re.search(api_endpoint, self.path):
            query = parse_qs(urlparse(self.path).query)
            if ("id" in query and "trial_number" in query and query["id"][0] in autotune_object_ids.keys() and
                    query["trial_number"][0] == str(get_trial_number(query["id"][0]))):
                data = get_trial_json_object(query["id"][0])
                self._set_response(200, data)
            else:
                self._set_response(404, "-1")
        else:
            self._set_response(403, "-1")

    def handle_generate_new_operation(self, json_object):
        """Process EXP_TRIAL_GENERATE_NEW operation."""
        is_valid_json_object = validate_trial_generate_json(json_object)

        if is_valid_json_object and json_object["experiment_id"] not in autotune_object_ids.keys():
            get_search_create_study(json_object["experiment_id"], json_object["operation"], json_object["url"])
            trial_number = get_trial_number(json_object["experiment_id"])
            self._set_response(200, str(trial_number))
        else:
            self._set_response(400, "-1")

    def handle_generate_subsequent_operation(self, json_object):
        """Process EXP_TRIAL_GENERATE_SUBSEQUENT operation."""
        is_valid_json_object = validate_trial_generate_json(json_object)

        if is_valid_json_object and json_object["experiment_id"] in autotune_object_ids.keys():
            get_search_create_study(json_object["experiment_id"], json_object["operation"], json_object["url"])
            trial_number = get_trial_number(json_object["experiment_id"])
            self._set_response(200, str(trial_number))
        else:
            self._set_response(400, "-1")

    def handle_result_operation(self, json_object):
        """Process EXP_TRIAL_RESULT operation."""
        if (json_object["experiment_id"] in autotune_object_ids.keys() and
                json_object["trial_number"] == get_trial_number(json_object["experiment_id"])):
            set_result(json_object["experiment_id"], json_object["trial_result"], json_object["result_value_type"],
                       json_object["result_value"])
            self._set_response(200, "0")
        else:
            self._set_response(400, "-1")


def get_search_create_study(id_, operation, url):
    # TODO: validate structure of search_space_json
    search_space_json = get_search_space(id_, url)
    if operation == "EXP_TRIAL_GENERATE_NEW":
        experiment_name, direction, hpo_algo_impl, id_, objective_function, tunables, value_type = get_all_tunables(
            search_space_json)
        autotune_object_ids[id_] = hpo_algo_impl
        if hpo_algo_impl in ("optuna_tpe", "optuna_tpe_multivariate", "optuna_skopt"):
            threading.Thread(
                target=optuna_hpo.recommend, args=(experiment_name, direction, hpo_algo_impl, id_, objective_function,
                                                   tunables, value_type)).start()
        time.sleep(2)


def get_search_space(id_, url):
    """Perform a GET request and return the search space json."""
    params = {"id": id_}
    r = requests.get(url, params)
    r.raise_for_status()
    search_space_json = r.json()
    return search_space_json


def get_trial_number(id_):
    """Return the trial number."""
    if autotune_object_ids[id_] in ("optuna_tpe", "optuna_tpe_multivariate", "optuna_skopt"):
        trial_number = optuna_hpo.TrialDetails.trial_number
    return trial_number


def get_trial_json_object(id_):
    """Return the trial json object."""
    if autotune_object_ids[id_] in ("optuna_tpe", "optuna_tpe_multivariate", "optuna_skopt"):
        trial_json_object = json.dumps(optuna_hpo.TrialDetails.trial_json_object)
    return trial_json_object


def set_result(id_, trial_result, result_value_type, result_value):
    """Set the details of a trial."""
    if autotune_object_ids[id_] in ("optuna_tpe", "optuna_tpe_multivariate", "optuna_skopt"):
        optuna_hpo.TrialDetails.trial_result = trial_result
        optuna_hpo.TrialDetails.result_value_type = result_value_type
        optuna_hpo.TrialDetails.result_value = result_value
        optuna_hpo.TrialDetails.trial_result_received = 1


def main():
    server = HTTPServer((host_name, server_port), HTTPRequestHandler)
    print("Starting server at http://%s:%s" % (host_name, server_port))
    server.serve_forever()


if __name__ == '__main__':
    main()
