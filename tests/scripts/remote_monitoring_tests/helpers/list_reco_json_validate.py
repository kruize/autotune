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
import json
import jsonschema
from jsonschema import validate, draft7_format_checker
from helpers.list_reco_json_schema import list_reco_json_schema

KUBERNETES_OBJECTS_TYPE_SUPPORTED = ("deployment", "replicaset", "deployConfig", "statefulset", "daemonset", "replicationController")

KUBERNETES_OBJECTS_TYPE_NOT_SUPPORTED = "Kubernetes objects type not supported!"
JSON_NULL_VALUES = ("is not of type 'string'", "is not of type 'integer'", "is not of type 'number'")
VALUE_MISSING = " cannot be empty or null!"

def validate_list_reco_json(list_reco_json):
    errorMsg = ""
    try:
        print("Validating json against the json schema...")
        validate(instance=list_reco_json, schema=list_reco_json_schema, format_checker=draft7_format_checker)
        print("Validating json against the json schema...done")
        errorMsg = validate_list_reco_json_values(list_reco_json[0])

        return errorMsg
    except jsonschema.exceptions.ValidationError as err:
        # Check if the exception is due to empty or null required parameters and prepare the response accordingly
        if any(word in err.message for word in JSON_NULL_VALUES):
            errorMsg = "Parameters" + VALUE_MISSING
            return errorMsg
        # Modify the error response in case of additional properties error
        elif str(err.message).__contains__('('):
            errorMsg = str(err.message).split('(')
            return errorMsg[0]
        else:
            return err.message

def validate_list_reco_json_values(reco):
    validationErrorMsg = ""
    obj_arr = ["kubernetes_objects"]

    for key in reco.keys():

        # Check if any of the key is empty or null
        if not (str(reco[key]) and str(reco[key]).strip()):
            validationErrorMsg = ",".join([validationErrorMsg, "Parameters" + VALUE_MISSING])

        for obj in obj_arr:
            if obj == key:
                for subkey in reco[key][0].keys():
                    # Check if any of the key is empty or null
                    if not (str(reco[key][0][subkey]) and str(reco[key][0][subkey]).strip()):
                        validationErrorMsg = ",".join([validationErrorMsg, "Parameters" + VALUE_MISSING])
                    elif str(subkey) == "type" and str(reco[key][0][subkey]) not in KUBERNETES_OBJECTS_TYPE_SUPPORTED:
                        validationErrorMsg = ",".join([validationErrorMsg, KUBERNETES_OBJECTS_TYPE_NOT_SUPPORTED])

    return validationErrorMsg.lstrip(',')

