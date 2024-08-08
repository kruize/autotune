"""
Copyright (c) 2024, 2024 Red Hat, IBM Corporation and others.

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
from jsonschema import FormatChecker
from jsonschema.exceptions import ValidationError
from helpers.list_metric_profiles_schema import list_metric_profiles_schema


SLO_CLASSES_SUPPORTED = ("throughput", "response_time", "resource_usage")
SLO_CLASSES_NOT_SUPPORTED = "SLO class not supported!"

DIRECTIONS_SUPPORTED = ("minimize", "maximize")
DIRECTIONS_NOT_SUPPORTED = "Directions not supported!"

VALUE_TYPES_SUPPORTED = ("double", "int", "string", "categorical")
VALUE_TYPE_NOT_SUPPORTED = "Value type not supported!"

KUBERNETES_OBJECTS_TYPE_SUPPORTED = ("deployment", "pod", "container", "namespace")
KUBERNETES_OBJECTS_TYPE_NOT_SUPPORTED = "Kubernetes objects type not supported!"

FUNCTION_TYPES_SUPPORTED = ("sum", "avg", "min", "max")
FUNCTION_TYPE_NOT_SUPPORTED = "Aggregation function type not supported!"

JSON_NULL_VALUES = ("is not of type 'string'", "is not of type 'integer'", "is not of type 'number'")
VALUE_MISSING = " cannot be empty or null!"


def validate_list_metric_profiles_json(list_metric_profiles_json, json_schema):
    errorMsg = ""
    try:
        # create a validator with the format checker
        print("Validating json against the json schema...")
        validator = jsonschema.Draft7Validator(json_schema, format_checker=FormatChecker())

        # validate the JSON data against the schema
        errors = ""
        errors = list(validator.iter_errors(list_metric_profiles_json))
        print("Validating json against the json schema...done")
        errorMsg = validate_list_metric_profiles_json_values(list_metric_profiles_json)

        if errors:
            custom_err = ValidationError(errorMsg)
            errors.append(custom_err)
            return errors
        else:
            return errorMsg
    except ValidationError as err:
        print("Received a VaidationError")

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

def validate_list_metric_profiles_json_values(metric_profile):
    validationErrorMsg = ""
    slo = "slo"
    func_var = "function_variables"
    aggr_func = "aggregation_functions"

    for key in metric_profile[0].keys():

        # Check if any of the key is empty or null
        if not (str(metric_profile[0][key]) and str(metric_profile[0][key]).strip()):
            validationErrorMsg = ",".join([validationErrorMsg, "Parameters" + VALUE_MISSING])

        if slo == key:
            for subkey in metric_profile[0][key].keys():
                if not (str(metric_profile[0][key][subkey]) and str(metric_profile[0][key][subkey]).strip()):
                    print(f"FAILED - {str(metric_profile[0][key][subkey])} is empty or null")
                    validationErrorMsg = ",".join([validationErrorMsg, "Parameters" + VALUE_MISSING])
                elif str(subkey) == "sloClass" and (str(metric_profile[0][key][subkey]) not in SLO_CLASSES_SUPPORTED):
                    validationErrorMsg = ",".join([validationErrorMsg, SLO_CLASSES_NOT_SUPPORTED])
                elif str(subkey) == "direction" and (str(metric_profile[0][key][subkey]) not in DIRECTIONS_SUPPORTED):
                    validationErrorMsg = ",".join([validationErrorMsg, DIRECTIONS_NOT_SUPPORTED])

                if func_var == subkey:
                    for func_var_object in metric_profile[0][key][subkey]:
                        for field in func_var_object.keys():
                            # Check if any of the key is empty or null
                            if not (str(func_var_object.get(field)) and str(func_var_object.get(field)).strip()):
                                print(f"FAILED - {str(func_var_object.get(field))} is empty or null")
                                validationErrorMsg = ",".join([validationErrorMsg, "Parameters" + VALUE_MISSING])
                            elif str(field) == "value_type" and str(func_var_object.get(field)) not in VALUE_TYPES_SUPPORTED:
                                validationErrorMsg = ",".join([validationErrorMsg, VALUE_TYPE_NOT_SUPPORTED])
                            elif str(field) == "kubernetes_object" and str(func_var_object.get(field)) not in KUBERNETES_OBJECTS_TYPE_SUPPORTED:
                                validationErrorMsg = ",".join([validationErrorMsg, KUBERNETES_OBJECTS_TYPE_NOT_SUPPORTED])

                            if aggr_func == field:
                                aggr_func_obj = func_var_object.get("aggregation_functions", {})
                                for aggr_func_object, aggr_func_value in aggr_func_obj.items():
                                    for query in aggr_func_value.keys():
                                        # Check if any of the key is empty or null
                                        if not (str(aggr_func_value.get(query)) and str(aggr_func_value.get(query)).strip()):
                                            print(f"FAILED - {str(aggr_func_value.get(query))} is empty or null")
                                            validationErrorMsg = ",".join([validationErrorMsg, "Parameters" + VALUE_MISSING])
                                        elif str(query) == "function" and str(aggr_func_value.get(query)) not in FUNCTION_TYPES_SUPPORTED:
                                            validationErrorMsg = ",".join([validationErrorMsg, FUNCTION_TYPE_NOT_SUPPORTED])

    return validationErrorMsg.lstrip(',')
