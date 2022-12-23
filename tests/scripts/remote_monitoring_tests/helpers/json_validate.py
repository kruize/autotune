"""
Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.

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

import jsonschema
from jsonschema import validate, draft7_format_checker

DIRECTIONS_SUPPORTED = ("maximize", "minimize")

DIRECTION_NOT_SUPPORTED = "Direction not supported!"
JSON_NULL_VALUES = ("is not of type 'string'", "is not of type 'integer'", "is not of type 'number'")
VALUE_MISSING = " cannot be empty or null!"

exp_input_schema = {
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
            "name": {"type": "string"},
            "namespace": {"type": "string"},
            "slo": {
                "type": "object",
                "properties": {
                    "slo_class": {"type": "string"},
                    "direction": {"type": "string"}
                },
                "required": ["slo_class", "direction"],
                "additionalProperties": False
            },
            "mode": {"type": "string"},
            "targetCluster": {"type": "string"},
            "containers": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "image": {"type": "string"},
                        "container_name": {"type": "string"}
                    },
                    "required": ["image", "container_name"],
                    "additionalProperties": False
                }
            },
            "trial_settings": {
                "type": "object",
                "properties": {
                    "measurement_duration": {"type": "string"}
                },
                "required": ["measurement_duration"],
                "additionalProperties": False
            },
            "recommendation_settings": {
                "type": "object",
                "properties": {
                    "threshold": {"type": "string"}
                },
                "required": ["threshold"],
                "additionalProperties": False
            },
            "selector": {
                "type": "object",
                "properties": {
                    "matchLabel": {"type": "string"},
                    "matchLabelValue": {"type": "string"},
                    "matchRoute": {"type": "string"},
                    "matchURI": {"type": "string"},
                    "matchService": {"type": "string"}
                },
                "required": ["matchLabel", "matchLabelValue"],
                "additionalProperties": False
             }
        },
        "required": ["name", "namespace", "slo", "mode", "targetCluster", "containers", "trial_settings", "recommendation_settings", "selector"],
        "additionalProperties": False
    }
}

def validate_exp_input_json(exp_input_json):
    errorMsg = ""
    try:
        validate(instance=exp_input_json, schema=exp_input_schema, format_checker=draft7_format_checker)
        errorMsg = validate_exp_input_json_values(exp_input_json[0])
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

def validate_exp_input_json_values(exp):
    validationErrorMsg = ""
    obj_arr = ["slo", "trial_settings", "recommendation_settings"]

    for key in exp.keys():

        # Check if any of the key is empty or null
        if not (str(exp[key]) and str(exp[key]).strip()):
            validationErrorMsg = ",".join([validationErrorMsg, "Parameters" + VALUE_MISSING])

        for obj in obj_arr:
            if obj == key:
                for subkey in exp[key].keys():
                    # Check if any of the key is empty or null
                    if not (str(exp[key][subkey]) and str(exp[key][subkey]).strip()):
                        validationErrorMsg = ",".join([validationErrorMsg, "Parameters" + VALUE_MISSING])
                    elif str(subkey) == "direction" and str(exp[key][subkey]) not in DIRECTIONS_SUPPORTED:
                        validationErrorMsg = ",".join([validationErrorMsg, DIRECTION_NOT_SUPPORTED])

    return validationErrorMsg.lstrip(',')

