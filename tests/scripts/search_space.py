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
import sys
from urllib.parse import urlparse, parse_qs


json_file = "searchspace.json"

class HTTPRequestHandler(BaseHTTPRequestHandler):
    def _set_response(self, status_code, return_value):
        # TODO: add status_message
        self.send_response(status_code)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        self.wfile.write(return_value.encode('utf-8'))


    def do_GET(self):
        # TODO: perform better regex search
        if re.search('/searchSpace', self.path):
            query = parse_qs(urlparse(self.path).query)
            print("********")
            print(json_file)
            print("********")
            with open(json_file, "r") as read_file:
                search_space_json = json.load(read_file)
            data = json.dumps(search_space_json)
            self._set_response(200, data)
        else:
            self._set_response(403, "-1")



def main():
    host_name = "localhost"
    server_port = 8080

    global json_file
    json_file=sys.argv[1]

    server = HTTPServer((host_name, server_port), HTTPRequestHandler)
    print("Starting server at http://%s:%s" % (host_name, server_port))
    server.serve_forever()


if __name__ == '__main__':
    main()
